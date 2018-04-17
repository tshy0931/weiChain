package tshy0931.com.github.weichain.testdata

import tshy0931.com.github.weichain.Hash
import tshy0931.com.github.weichain.model.Block.{BlockBody, BlockHeader}
import tshy0931.com.github.weichain.model.{Block, MerkleTree, Transaction}
import tshy0931.com.github.weichain.module.BlockChainModule.genesisBlockHeader
import tshy0931.com.github.weichain.module.MiningModule.mineWithTransactions
import scala.concurrent.duration._
import monix.execution.Scheduler.Implicits.global

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

  lazy val blk1 = mineWithTransactions(Vector(validTx1), genesisBlockHeader)("","","") runSyncUnsafe(120 seconds)
  lazy val blk2 = mineWithTransactions(Vector(validTx2), genesisBlockHeader)("","","") runSyncUnsafe(120 seconds)
}
