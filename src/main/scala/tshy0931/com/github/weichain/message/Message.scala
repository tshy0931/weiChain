package tshy0931.com.github.weichain.message

import tshy0931.com.github.weichain.Hash
import tshy0931.com.github.weichain.model.Block.BlockHeader
import tshy0931.com.github.weichain.model.Transaction

/** Example: https://bitcoin.org/en/developer-reference#merkleblock
  * 01000000 ........................... Block version: 1
    82bb869cf3a793432a66e826e05a6fc3
    7469f8efb7421dc88067010000000000 ... Hash of previous block's header
    7f16c5962e8bd963659c793ce370d95f
    093bc7e367117b3c30c1f8fdd0d97287 ... Merkle root
    76381b4d ........................... Time: 1293629558
    4c86041b ........................... nBits: 0x04864c * 256**(0x1b-3)
    554b8529 ........................... Nonce

    07000000 ........................... Transaction count: 7
    04 ................................. Hash count: 4

    3612262624047ee87660be1a707519a4
    43b1c1ce3d248cbfc6c15870f6c5daa2 ... Hash #1
    019f5b01d4195ecbc9398fbf3c3b1fa9
    bb3183301d7a1fb3bd174fcfa40a2b65 ... Hash #2
    41ed70551dd7e841883ab8f0b16bf041
    76b7d1480e4f0af9f3d4c3595768d068 ... Hash #3
    20d2a7bc994987302e5b1ac80fc425fe
    25f8b63169ea78e68fbaaefa59379bbf ... Hash #4

    01 ................................. Flag bytes: 1
    1d ................................. Flags: 1 0 1 1 1 0 0 0
  * @param blockHeader - block header of the block having the transaction in query
  * @param nTx - number of transactions in total, including ones filtered out
  * @param hashes - hashes retrieved from merkle tree to verify the transaction in query
  * @param flags - 0 or 1 flags to indicate how to use the hashes in validation
  */
case class MerkleBlock(blockHeader: BlockHeader,
                       nTx: Long,
                       hashes: Vector[Hash],
                       flags: Vector[Int])

case class Headers(count: Int,
                   forkIndex: Int,
                   headers: Vector[BlockHeader])

case class Blocks(count: Int,
                  blockHashes: Vector[Hash])

/** https://bitcoin.org/en/developer-reference#filterload
  * 02 ......... Filter bytes: 2
    b50f ....... Filter: 1010 1101 1111 0000
    0b000000 ... nHashFuncs: 11
    00000000 ... nTweak: 0/none
    00 ......... nFlags: BLOOM_UPDATE_NONE
  */
case class FilterLoad(owner: String,
                      initialInsertions: Vector[Transaction],
                      expectedInsertions: Int,
                      falsePositiveRate: Option[Double])

case class FilterAdd(owner: String,
                     inserts: Vector[Transaction])