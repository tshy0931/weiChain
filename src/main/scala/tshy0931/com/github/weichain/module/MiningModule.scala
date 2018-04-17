package tshy0931.com.github.weichain.module

import tshy0931.com.github.weichain._
import tshy0931.com.github.weichain.model.Block._
import monix.eval.Task
import tshy0931.com.github.weichain.model.Transaction._
import tshy0931.com.github.weichain.model.{Block, Chain, MerkleTree, Transaction}
import ConfigurationModule.version

import scala.collection.mutable

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
  def difficulty: String = "00000"

  def mine(prevBlockHeader: BlockHeader)
          (rewardAddr: Hash,
           pubKeyScript: String,
           coinbaseScript: String): Task[Block] = {
    for {
      txs   <- selectTransactions
      block <- mineWithTransactions(txs, prevBlockHeader)(rewardAddr, pubKeyScript, coinbaseScript)
      _     <- updateHeaderChain(block.header, prevBlockHeader)
      _     <- removeTransactionsFromMempool(block.body.transactions) //TODO: design a better approach to handle tx in mempool
    } yield block
  }

  def mineWithTransactions(txs: Vector[Transaction],
                           prevBlockHeader: BlockHeader)
                          (rewardAddr: Hash,
                           pubKeyScript: String,
                           coinbaseScript: String): Task[Block] =
    for {
      pow <- proofOfWork(txs, prevBlockHeader.hash, difficulty)
      (nonce, resultHash, merkleTree) = pow
    } yield buildBlock(prevBlockHeader, txs, merkleTree, resultHash, nonce)(rewardAddr, pubKeyScript, coinbaseScript)

  /**
  Select a set of preferable transactions to include in the block to create. Rules are:
    1. prefer higher tx fees - TODO
    2. prefer earlier created
   */
  private[this] def selectTransactions: Task[Vector[Transaction]] = {
    // TODO prefer higher tx fees
    MemPool[Transaction].getEarliest(2000) map { txs => filterTxWithDuplicatedTxOut(txs) toVector }
  }

  private[this] def filterTxWithDuplicatedTxOut: Seq[Transaction] => Seq[Transaction] = input => {
    val outputs: mutable.TreeSet[Output] = new mutable.TreeSet[Output]
    input filter { _.txOut forall { output =>
      if(outputs.contains(output)) false
      else {
        outputs.add(output)
        true
      }
    }}
  }

  private def proofOfWork(transactions: Vector[Transaction],
                          prevHeaderHash: Hash,
                          difficulty: String): Task[(Int, Hash, MerkleTree)] = Task {

    val merkleTree = MerkleTree.build(transactions)
    val unnoncedHeaderHash = computeHash(prevHeaderHash, merkleTree.root)
    var nonce: Int = 0
    var resultHash: Hash = emptyHash

    do {
      nonce += 1
      resultHash = applyNonce(unnoncedHeaderHash, nonce)
    } while(!verify(nonce, resultHash, difficulty))

    (nonce, resultHash, merkleTree)
  }

  private[this] def buildBlock(prevBlockHeader: BlockHeader,
                               transactions: Vector[Transaction],
                               merkleTree: MerkleTree,
                               resultHash: Hash,
                               nonce: Int)
                              (rewardAddr: Hash,
                               pubKeyScript: String,
                               coinbaseScript: String): Block = {

    val merkleTree = MerkleTree.build(transactions)
    val blockIndex = prevBlockHeader.height+1

    val header = BlockHeader(
      hash = resultHash,
      version = version,
      prevHeaderHash = prevBlockHeader.hash,
      merkleRoot = merkleTree.root,
      height = blockIndex,
      nonce = nonce
    )

    val body = BlockBody(
      headerHash = resultHash,
      merkleTree = merkleTree,
      nTx = transactions.size,
      transactions = updateBlockDetails(transactions, header)(rewardAddr, pubKeyScript, coinbaseScript)
    )

    Block(header, body)
  }

  private[this] def updateHeaderChain(newHeader: BlockHeader, prevHeader: BlockHeader): Task[Unit] =
    Chain[BlockHeader].append(newHeader, prevHeader)

  private[this] def updateBlockDetails(txs: Vector[Transaction], header: BlockHeader)
                                      (rewardAddr: Hash, pubKeyScript: String, coinbaseScript: String): Vector[Transaction] = {
    (coinbaseTx(rewardAddr, pubKeyScript, coinbaseScript) +: txs).zipWithIndex map {
      case (tx, index) => (
        txBlockIndexLens.set(header.height) andThen
        txOutputBlockHashTraversal.set(header.hash) andThen
        txOutputTxIndexTraversal.set(index)
      ) (tx)
    }
  }

  private[this] def verify(nonce: Int, hash: Hash, difficulty: String): Boolean =
    hash startsWith difficulty

  private[this] def removeTransactionsFromMempool(txs: Seq[Transaction]) =
    Task.gatherUnordered( txs map { MemPool[Transaction].delete } )
}
