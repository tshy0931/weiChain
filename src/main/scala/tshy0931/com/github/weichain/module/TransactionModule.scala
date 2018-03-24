package tshy0931.com.github.weichain.module

import tshy0931.com.github.weichain.model.BlockChain.BlockChain
import tshy0931.com.github.weichain.model.{Block, Transaction}

object TransactionModule {

  case class Error(message: String, transaction: Transaction)
  
  def isCoinbase(transaction: Transaction): Boolean =
    transaction.txIn.length == 1 && transaction.txIn(1).source.isLeft

  def isValidSource(transaction: Transaction)(implicit blockchain: BlockChain): Boolean = {

    transaction.txIn forall { in =>
      val srcBlock: Block = blockchain.blockAt(in.source.right.get.blockIndex)
      val srcTx: Transaction = srcBlock.body.transactions(in.source.right.get.txIndex)
      val srcOutput: Transaction.Output = srcTx.txOut(in.source.right.get.outputIndex)

      srcOutput.address == in.address && srcOutput.value == in.value
    }
  }

  def isValidSpending(transaction: Transaction): Boolean = {

    val totalOut: Double = transaction.txOut.map(_.value).sum
    val totalIn: Double = transaction.txIn.map(_.value).sum
    totalOut <= totalIn
  }

  def isValidSignature(transaction: Transaction): Boolean = ???

}
