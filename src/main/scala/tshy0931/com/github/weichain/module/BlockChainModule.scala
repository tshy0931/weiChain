package tshy0931.com.github.weichain.module

import java.util.concurrent.ConcurrentHashMap

import tshy0931.com.github.weichain._
import tshy0931.com.github.weichain.model.{Block, MerkleTree, Transaction}
import tshy0931.com.github.weichain.model.Block.{BlockBody, BlockHeader}
import ConfigurationModule._
import tshy0931.com.github.weichain.message.MerkleBlock

import scala.collection.JavaConverters._

object BlockChainModule {

  val genesisHash = "000000000019d6689c085ae165831e934ff763ae46a2a6c172b3f1b60a8ce26f".getBytes("UTF-8")
  val genesisMerkleRoot = "4a5e1e4baab89f3a32518a88c31bc87f618f76673e2cc77ab2127b7afdeda33b".getBytes("UTF-8")
  val genesisPrevHash = "0000000000000000000000000000000000000000000000000000000000000000".getBytes("UTF-8")
  val genesisNonce = 2083236893

  lazy val genesisBlock = Block(
    header = BlockHeader(
      hash = genesisHash,
      version = 1,
      prevBlockHash = genesisPrevHash,
      merkleRoot = genesisMerkleRoot,
      time = 1521820483592L,
      nBits = 486604799,
      nonce = genesisNonce
    ),
    body = BlockBody(
      merkleTree = MerkleTree(Vector(genesisMerkleRoot), 1),
      nTx = 1,
      size = 100L,
      transactions = Vector.empty[Transaction]
    )
  )

  var bestLocalHeaderChain: Vector[BlockHeader] = Vector(genesisBlock.header)
  //TODO - Store only header chain in memory and persist blocks into disk storage
  val bestLocalBlockChain: collection.concurrent.Map[String, Block] = new ConcurrentHashMap[String, Block]().asScala
  def chainHeight: Long = bestLocalHeaderChain.size

  val memPool: collection.concurrent.Map[String, Transaction] = new ConcurrentHashMap[String, Transaction]().asScala

  var blocksSynced: Boolean = false
  var headersSynced: Boolean = false

  def latestBlock: Block = bestLocalBlockChain(bestLocalHeaderChain.last.hash)
  def blockAt(index: Int): Block = bestLocalBlockChain(bestLocalHeaderChain(index).hash)
  def blockWithHash(hash: String): Option[Block] = bestLocalBlockChain.get(hash)

  def merkleBlockOf(blockHash: String, txHash: String): Option[MerkleBlock] = {
    for {
      block       <- blockWithHash(blockHash)
      merkleBlock <- block.body.merkleTree.deriveMerkleBlockFor(txHash)(block.header, block.body.nTx)
    } yield merkleBlock
  }

  def getBlocksByHashes(hashes: Vector[Hash]): Vector[Option[Block]] = {
    hashes map { bestLocalBlockChain.get(_) }
  }

  /**
    * Search best local header chain to locate the earliest header in the given headers.
    * If there is a fork between this and peer's best chain,
    * returned headers will tell where the fork occurred.
    * @param headers - latest headers on best header chain of the peer node.
    * @param count - number of headers following the given headers to return
    * @return - headers following the given headers on local best header chain.
    */
  def searchHeadersAfter(headers: Vector[BlockHeader], count: Int = maxHeadersPerRequest): (Int, Vector[BlockHeader]) = {

    var forkIndex: Int = 0
    val peerChain: Iterator[BlockHeader] = headers.iterator
    val peerChainHead: BlockHeader = peerChain.next
    val matchingChain: Vector[BlockHeader] = bestLocalHeaderChain.dropWhile( header => hashToString(header.hash) != hashToString(peerChainHead.hash) )

    if(matchingChain.isEmpty) {
      (forkIndex, bestLocalHeaderChain take count)
    } else {
      val followingChain: Vector[BlockHeader] = (matchingChain drop 1) dropWhile { header =>
        forkIndex += 1
        peerChain.hasNext && hashToString(header.hash) == hashToString(peerChain.next.hash)
      } take count

      (forkIndex, followingChain)
    }
  }
}
