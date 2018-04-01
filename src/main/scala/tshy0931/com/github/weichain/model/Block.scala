package tshy0931.com.github.weichain.model

import tshy0931.com.github.weichain.Hash
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
    * @param nBits - encoded version of the target threshold the header hash must be less than, aka difficulty.
    * @param nonce - the number to hash with block header and produce a hash less than the target threshold. proof of work.
    */
  case class BlockHeader(hash: Hash,
                         version: Int,
                         prevHeaderHash: Hash,
                         merkleRoot: Hash,
                         time: Long,
                         nBits: Long,
                         nonce: Long)

  /**
    *
    * @param merkleTree - merkle tree built from transactions in this block. See https://bitcoin.org/en/developer-reference#merkle-trees
    * @param nTx - number of transactions in this block
    * @param size - total size in bytes of all transactions in this block
    * @param transactions - collection of transactions in this block
    */
  case class BlockBody(headerHash: Hash,
                       merkleTree: MerkleTree,
                       nTx: Int,
                       size: Long,
                       transactions: Vector[Transaction])

  implicit class BlockHeaderOps(header: BlockHeader) {

    def computeHash: Hash = {
      digest(
        s"""${header.version}
           |${header.prevHeaderHash}
           |${header.merkleRoot}
           |${header.time}
           |${header.nBits}
           |${header.nonce}
        """.stripMargin
      )
    }
  }
}
