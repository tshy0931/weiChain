package tshy0931.com.github.weichain.model

import monocle.function.all._
import monocle.{Lens, Traversal}
import monocle.macros.GenLens
import Transaction._
import tshy0931.com.github.weichain._
import tshy0931.com.github.weichain.model.Block.{BlockBody, BlockHeader}
import tshy0931.com.github.weichain.module.DigestModule._

case class Block(header: BlockHeader, body: BlockBody)

object Block {

  /** Example:
    * 02000000 ........................... Block version: 2

      b6ff0b1b1680a2862a30ca44d346d9e8
      910d334beb48ca0c0000000000000000 ... Hash of previous block's header
      9d10aa52ee949386ca9385695f04ede2
      70dda20810decd12bc9b048aaab31471 ... Merkle root

      24d95a54 ........................... Unix time: 1415239972
      30c31b18 ........................... Target: 0x1bc330 * 256**(0x18-3)
      fe9f0864 ........................... Nonce
    * @param hash - hash of current block header
    * @param version - version of block validation rules
    * @param prevHeaderHash - hash of previous block header
    * @param merkleRoot - root value of the Merkle tree built on transactions in this block
    * @param time - Unix epoch time when the miner started hashing the header. Must be within [median time of previous 11 blocks, 2 hours in future]
    * @param height - encoded version of the target threshold the header hash must be less than, aka difficulty.
    * @param nonce - the number to hash with block header and produce a hash less than the target threshold. proof of work.
    */
  case class BlockHeader(hash: Hash,
                         version: Int,
                         prevHeaderHash: Hash,
                         merkleRoot: Hash,
                         time: Long = System.currentTimeMillis,
                         height: Int,
                         nonce: Long = 0L)

  /**
    *
    * @param merkleTree - merkle tree built from transactions in this block. See https://bitcoin.org/en/developer-reference#merkle-trees
    * @param nTx - number of transactions in this block
    * @param size - total size in bytes of all transactions in this block
    * @param transactions - collection of transactions in this block
    */
  case class BlockBody(headerHash: Hash = emptyHash,
                       merkleTree: MerkleTree,
                       nTx: Int,
                       size: Long = 0L,
                       transactions: Vector[Transaction])

  val headerLens: Lens[Block, BlockHeader] = GenLens[Block](_.header)
  val bodyLens: Lens[Block, BlockBody] = GenLens[Block](_.body)
  val nonceLens: Lens[BlockHeader, Long] = GenLens[BlockHeader](_.nonce)
  val headerHashLens: Lens[BlockHeader, Hash] = GenLens[BlockHeader](_.hash)
  val blockHashLens: Lens[Block, Hash] = headerLens composeLens headerHashLens
  val blockNonceLens: Lens[Block, Long] = headerLens composeLens nonceLens
  val txLens: Lens[BlockBody, Vector[Transaction]] = GenLens[BlockBody](_.transactions)
  val txBlockIndexLens: Lens[Transaction, Long] = GenLens[Transaction](_.blockIndex)
  val blockTxTraversal: Traversal[Block, Transaction] = bodyLens composeLens txLens composeTraversal each
  val blockOutputTraversal: Traversal[Block, Output] = blockTxTraversal composeTraversal txOutputTraversal

  def computeHash(prevHeaderHash: Hash, merkleRoot: Hash): Hash = {
    digest(
      s"""$prevHeaderHash
         |$merkleRoot
       """.stripMargin
    )
  }

  def applyNonce(headerHash: Hash, nonce: Long): Hash =
    digest(headerHash + nonce)

  implicit class BlockHeaderOps(header: BlockHeader) {

    def computeNoncedHash: Hash =
      applyNonce(computeHash(header.prevHeaderHash, header.merkleRoot), header.nonce)
  }
}
