package tshy0931.com.github.weichain.module

import tshy0931.com.github.weichain._
import BlockChainModule._
import DigestModule._
import tshy0931.com.github.weichain.model.Block._
import monix.eval.Task
import tshy0931.com.github.weichain.model.{Block, Transaction}
import tshy0931.com.github.weichain.model.Block.BlockHeader

/**
  * This trait defines functionality required for mining a valid block.
  */
object MiningModule {

  import tshy0931.com.github.weichain.database.MemPool
  /**
    * Difficulty of mining, number of '0's required in a valid block header hash
    * should vary regarding specific rules like average mining time etc.
    *
    */
  def difficulty: String = "00"

  def mine(block: Block): Task[Block] = Task.defer {

    proofOfWork(block.header, difficulty) map {
      case (nonce, hash) => (blockHashLens.set(hash) andThen blockNonceLens.set(nonce))(block)
    }
  }

  /**
  Select a set of preferable transactions to include in the block to create. Rules are:
    1. prefer higher tx fees
    2. prefer earlier created
   */
  private def selectTransactions: Task[Seq[Transaction]] = {
    MemPool[Transaction].getEarliest(2000)

  }

  private def proofOfWork(blockHeader: BlockHeader, difficulty: String): Task[(Int, Hash)] = Task {

    var nonce: Int = 0
    var result: Array[Byte] = Array.emptyByteArray
    do {
      nonce += 1
      result = digest(blockHeader.computeHash :+ nonce.toByte)
    } while(!verify(nonce, result, difficulty))

    (nonce, result)
  }

  def verify(nonce: Int, hash: Hash, difficulty: String): Boolean = hash.asString startsWith difficulty

}
