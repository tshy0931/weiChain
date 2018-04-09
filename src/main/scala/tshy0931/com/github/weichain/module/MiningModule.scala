package tshy0931.com.github.weichain.module

import tshy0931.com.github.weichain._
import tshy0931.com.github.weichain.model.Block._
import monix.eval.Task
import tshy0931.com.github.weichain.model.Transaction._
import tshy0931.com.github.weichain.model.{Block, MerkleTree, Transaction}
import ConfigurationModule.version

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

  def mine(prevBlock: Block): Task[Block] = {
    for {
      txs   <- selectTransactions
      block <- mineWithTransactions(txs, prevBlock)
    } yield block
  }

  def mineWithTransactions(txs: Vector[Transaction], prevBlock: Block): Task[Block] =
    for {
      pow <- proofOfWork(txs, prevBlock, difficulty)
      (nonce, resultHash, merkleTree) = pow
    } yield buildBlock(prevBlock, txs, merkleTree, resultHash, nonce)

  /**
  Select a set of preferable transactions to include in the block to create. Rules are:
    1. prefer higher tx fees - TODO
    2. prefer earlier created
   */
  private def selectTransactions: Task[Vector[Transaction]] = {
    // TODO prefer higher tx fees
    MemPool[Transaction].getEarliest(2000) map {_.toVector}
  }

  private def proofOfWork(transactions: Vector[Transaction],
                          prevBlock: Block,
                          difficulty: String): Task[(Int, Hash, MerkleTree)] = Task {

    val merkleTree = MerkleTree.build(transactions)
    val unnoncedHeaderHash = computeHash(prevBlock.header.hash, merkleTree.root)
    var nonce: Int = 0
    var resultHash: Hash = Array.emptyByteArray

    do {
      nonce += 1
      resultHash = applyNonce(unnoncedHeaderHash, nonce)
    } while(!verify(nonce, resultHash, difficulty))

    (nonce, resultHash, merkleTree)
  }

  private[this] def buildBlock(prevBlock: Block,
                               transactions: Vector[Transaction],
                               merkleTree: MerkleTree,
                               resultHash: Hash,
                               nonce: Int): Block = {
    val merkleTree = MerkleTree.build(transactions)
    val blockIndex = prevBlock.header.index+1

    val header = BlockHeader(
      hash = resultHash,
      version = version,
      prevHeaderHash = prevBlock.header.hash,
      merkleRoot = merkleTree.root,
      index = blockIndex,
      nonce = nonce
    )

    val body = BlockBody(
      headerHash = resultHash,
      merkleTree = merkleTree,
      nTx = transactions.size,
      transactions = updateBlockDetails(transactions, header)
    )

    Block(header, body)
  }

  private[this] def updateBlockDetails(txs: Vector[Transaction], header: BlockHeader): Vector[Transaction] =
    txs.zipWithIndex map {
      case (tx, index) => (
        txBlockIndexLens.set(header.index) andThen
        txOutputBlockHashTraversal.set(header.hash) andThen
        txOutputTxIndexTraversal.set(index)
      ) (tx)
    }

  private[this] def verify(nonce: Int, hash: Hash, difficulty: String): Boolean =
    hash.asString startsWith difficulty

}
