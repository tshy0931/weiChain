package tshy0931.com.github.weichain.module

import cats.syntax.all._
import tshy0931.com.github.weichain._
import tshy0931.com.github.weichain.model.{Block, Chain, MerkleTree, Transaction}
import tshy0931.com.github.weichain.model.Block.{BlockBody, BlockHeader}
import DigestModule._
import ConfigurationModule._
import MiningModule._
import akka.event.Logging
import cats.data.OptionT
import monix.eval.Task
import tshy0931.com.github.weichain.message.MerkleBlock
import tshy0931.com.github.weichain.database.Database._
import monix.execution.atomic._
import tshy0931.com.github.weichain.database.Database
import tshy0931.com.github.weichain.module.NetworkModule.system

import scala.concurrent.duration._
object BlockChainModule {

  import monix.execution.Scheduler.Implicits.global
  lazy val log = Logging(system, this.getClass)

  val genesisHash: Hash = digest("tshy0931") // 3147a5ca5ca35cbee1249030b7a5c92efd5cece51ed14ce5cd6fe41eb4021a96
  val genesisTime: Long = 1211976000000L
  val genesisBlockHeader: BlockHeader = BlockHeader(
    hash = genesisHash,
    version = 1,
    prevHeaderHash = emptyHash,
    merkleRoot = MerkleTree.build(Vector.empty).root,
    time = genesisTime,
    height = 0
  )

  val genesisBlock: Block =
    mineWithTransactions(Vector.empty, genesisBlockHeader)(rewardAddr, minerPubKeyScript, minerCoinbaseScript) runSyncUnsafe(60 seconds)

  def chainHeight: Task[Long] = Chain[BlockHeader].size

  var blocksSynced: Atomic[Boolean] = Atomic(false)
  var headersSynced: Atomic[Boolean] = Atomic(false)

  def latestHeader: Task[BlockHeader] = Chain[BlockHeader].last(1) map { _.head }
  def latestBlock: Task[Block] = for {
    header <- latestHeader
    block  <- blockWithHash(header.hash) getOrElse genesisBlock
  } yield block

  // TODO maintain an index or bloomfilter to find in which block a given tx is
  def blockWithHash(hash: String): OptionT[Task, Block] = OptionT(
    (Database[BlockHeader].find(hash), Database[BlockBody].find(hash)) parMapN {
      case (Some(header), Some(body)) => Block(header, body).some
      case _ => None
    })

  def transactionWithHash(hash: String): Task[Option[Transaction]] = Database[Transaction].find(hash)

  def merkleBlockOf(blockHash: String, txHash: String): OptionT[Task, MerkleBlock] = {
    for {
      block       <- blockWithHash(blockHash)
      merkleBlock <- block.body.merkleTree.deriveMerkleBlockFor(txHash)(block.header, block.body.nTx)
    } yield merkleBlock
  }

  /**
    * Search best local header chain to locate the earliest header in the given headers.
    * If there is a fork between this and peer's best chain,
    * returned headers will tell where the fork occurred.
    * @param headers - latest headers on best header chain of the peer node.
    * @param count - number of headers following the given headers to return
    * @return - headers following the given headers on local best header chain.
    */
  def searchHeadersAfter(headers: Seq[BlockHeader], count: Int = maxHeadersPerRequest): Task[(Int, Seq[BlockHeader])] =
    if(headers.isEmpty) {
      // return count number of headers starting from genesisBlockHeader
      Chain[BlockHeader].first(count) map {(0, _)}
    } else {
      for {
        localSlice  <- Chain[BlockHeader].slice(headers.head.height, headers.head.height + headers.size)
        forkIndex   <- findFork(localSlice, headers)
        correctOnes <- headersAfterFork(forkIndex, count)
      } yield (forkIndex, correctOnes)
    }

  private[this] def findFork(localSlice: Seq[BlockHeader], peerSlice: Seq[BlockHeader]): Task[Int] =
    Task.eval {
      val iter = peerSlice.iterator
      val afterFork: Seq[BlockHeader] = localSlice.dropWhile { localHeader =>
        iter.hasNext && iter.next.hash.sameElements(localHeader.hash)
      }
      afterFork.headOption map {_.height} getOrElse localSlice.last.height+1
    }

  private[this] def headersAfterFork(forkIndex: Int, count: Int): Task[Seq[BlockHeader]] =
    Chain[BlockHeader].slice(forkIndex, forkIndex+count)

  def start: Task[Unit] = {
    // store genesis block header to header chain
    log.info("initializing genesis block")
    (
      Chain[BlockHeader].update(Seq(genesisBlock.header), genesisBlock.header.height),
      Database[BlockHeader].save(genesisBlock.header),
      Database[BlockBody].save(genesisBlock.body)
    ) parMapN {
      (_, _, _) => log.info("genesis block {} initialized", genesisBlock)
    }
  }
}
