package tshy0931.com.github.weichain.module

import java.util.concurrent.ConcurrentHashMap

import cats.syntax.all._
import tshy0931.com.github.weichain._
import tshy0931.com.github.weichain.model.{Block, MerkleTree, Transaction}
import tshy0931.com.github.weichain.model.Block.{BlockBody, BlockHeader}
import ConfigurationModule._
import cats.data.OptionT
import monix.eval.{Coeval, Task}
import tshy0931.com.github.weichain.message.MerkleBlock
import tshy0931.com.github.weichain.database.Database._
import monix.execution.atomic._
import shapeless.the
import tshy0931.com.github.weichain.database.Database

import scala.collection.JavaConverters._

object BlockChainModule {

  val genesisHash = "000000000019d6689c085ae165831e934ff763ae46a2a6c172b3f1b60a8ce26f".getBytes("UTF-8")
  val genesisMerkleRoot = "4a5e1e4baab89f3a32518a88c31bc87f618f76673e2cc77ab2127b7afdeda33b".getBytes("UTF-8")
  val genesisPrevHash = "0000000000000000000000000000000000000000000000000000000000000000".getBytes("UTF-8")
  val genesisNonce = 2083236893

  val genesisBlock: Coeval[Block] = Coeval.evalOnce(
    Block(
      header = BlockHeader(
        hash = genesisHash,
        version = 1,
        prevHeaderHash = genesisPrevHash,
        merkleRoot = genesisMerkleRoot,
        time = 1521820483592L,
        nBits = 486604799,
        nonce = genesisNonce
      ),
      body = BlockBody(
        headerHash = genesisHash,
        merkleTree = MerkleTree(Vector(genesisMerkleRoot), 1),
        nTx = 1,
        size = 100L,
        transactions = Vector.empty[Transaction]
      )
    )
  )

  val bestLocalHeaderChain: Coeval[Atomic[Vector[BlockHeader]]] =
    genesisBlock map { blk => Atomic(Vector(blk.header)) } memoizeOnSuccess

  def getBestLocalHeaderChain: Atomic[Vector[BlockHeader]] = bestLocalHeaderChain.value

  def chainHeight: Long = getBestLocalHeaderChain.get.size

  val memPool: Coeval[collection.concurrent.Map[String, Transaction]] = Coeval.evalOnce(
    new ConcurrentHashMap[String, Transaction]().asScala
  )

  var blocksSynced: Atomic[Boolean] = Atomic(false)
  var headersSynced: Atomic[Boolean] = Atomic(false)

  def latestBlock: Task[Block] = blockWithHash(getBestLocalHeaderChain.get.last.hash.asString) getOrElse genesisBlock.value

  def blockWithHash(hash: String): OptionT[Task, Block] = OptionT(
    (the[Database[BlockHeader]].find(hash), the[Database[BlockBody]].find(hash)) parMapN {
      case (Some(header), Some(body)) => Block(header, body).some
      case _ => None
    })

  def merkleBlockOf(blockHash: String, txHash: String): OptionT[Task, MerkleBlock] = {
    for {
      block       <- blockWithHash(blockHash)
      merkleBlock <- block.body.merkleTree.deriveMerkleBlockFor(txHash)(block.header, block.body.nTx)
    } yield merkleBlock
  }

//  def getBlocksByHashes(hashes: Vector[Hash]): Task[Vector[Option[Block]]] =
//    Task.eval {
//      hashes map { hash => blockWithHash(hash.asString) }
//    }

  /**
    * Search best local header chain to locate the earliest header in the given headers.
    * If there is a fork between this and peer's best chain,
    * returned headers will tell where the fork occurred.
    * @param headers - latest headers on best header chain of the peer node.
    * @param count - number of headers following the given headers to return
    * @return - headers following the given headers on local best header chain.
    */
  def searchHeadersAfter(headers: Vector[BlockHeader], count: Int = maxHeadersPerRequest): Task[(Int, Vector[BlockHeader])] =
    Task.eval {
      var forkIndex: Int = 0
      val peerChain: Iterator[BlockHeader] = headers.iterator
      val peerChainHead: BlockHeader = peerChain.next
      val matchingChain: Vector[BlockHeader] = getBestLocalHeaderChain.get.dropWhile( header => header.hash.asString != peerChainHead.hash.asString )

      if(matchingChain.isEmpty) {
        (forkIndex, getBestLocalHeaderChain.get take count)
      } else {
        val followingChain: Vector[BlockHeader] = (matchingChain drop 1) dropWhile { header =>
          forkIndex += 1
          peerChain.hasNext && header.hash.asString == peerChain.next.hash.asString
        } take count

        (forkIndex, followingChain)
      }
    }
}
