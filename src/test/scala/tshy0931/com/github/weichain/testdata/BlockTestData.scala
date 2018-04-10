package tshy0931.com.github.weichain.testdata

import tshy0931.com.github.weichain.Hash
import tshy0931.com.github.weichain.model.Block.{BlockBody, BlockHeader}
import tshy0931.com.github.weichain.model.{Block, MerkleTree, Transaction}

trait BlockTestData { this: TransactionTestData with CommonData =>

  def testBlock(hash: Hash = testHash) = Block(
    header = BlockHeader(hash, 1, testHash, testHash, 1L, 1, 1L),
    body = BlockBody(
      hash,
      MerkleTree(Vector.empty[Hash], 0),
      3,
      3L,
      Vector(
        Transaction(testHash, 1, 0, Vector.empty, 3, Vector(txOutput(1, 2.0), txOutput(2, 0.2)), 0, 1L, 0.0, System.currentTimeMillis),
        Transaction(testHash, 1, 0, Vector.empty, 3, Vector(txOutput(1, 2.0), txOutput(2, 2.0)), 0, 1L, 0.0, System.currentTimeMillis),
        Transaction(testHash, 1, 0, Vector.empty, 3, Vector(txOutput(1, 0.1), txOutput(2, 0.1)), 0, 1L, 0.0, System.currentTimeMillis)
      )
    )
  )
}
