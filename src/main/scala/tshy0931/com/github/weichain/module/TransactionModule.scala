package tshy0931.com.github.weichain.module

import java.security.MessageDigest

import cats.data.Validated
import cats.syntax.validated._
import tshy0931.com.github.weichain.model.BlockChain.BlockChain
import tshy0931.com.github.weichain.model.{Block, BlockChain, Transaction}
import tshy0931.com.github.weichain.{DigestModule, SHA256DigestModule}

trait TransactionModule { this: DigestModule =>

  import TransactionModule._

  /**
    * The input of a transaction must be UTXO (Unspent Transaction Output) of a previous transaction, with exactly the same value.
    * @param transaction
    * @return
    */
  def isValidSource(transaction: Transaction)(implicit blockchain: BlockChain): Boolean

  /**
    * The total value in output must NOT be greater than the total value in input.
    * @param transaction
    * @return
    */
  def isValidSpending(transaction: Transaction): Boolean

  /**
    * the transaction's signature must be valid with given public key.
    * @param transaction
    * @return
    */
  def isValidSignature(transaction: Transaction): Boolean

  def isCoinbase(transaction: Transaction): Boolean
}

object TransactionModule {

  case class Error(message: String, transaction: Transaction)

  trait SimpleTransactionModule extends TransactionModule { this: DigestModule =>

    override def isCoinbase(transaction: Transaction): Boolean =
      transaction.txIn.length == 1 && transaction.txIn(1).source.isLeft

    override def isValidSource(transaction: Transaction)(implicit blockchain: BlockChain): Boolean = {

      transaction.txIn forall { in =>
        val srcBlock: Block = blockchain.blockAt(in.source.right.get.blockIndex)
        val srcTx: Transaction = srcBlock.body.transactions(in.source.right.get.txIndex)
        val srcOutput: Transaction.Output = srcTx.txOut(in.source.right.get.outputIndex)

        srcOutput.address == in.address && srcOutput.value == in.value
      }
    }

    override def isValidSpending(transaction: Transaction): Boolean = {

      val totalOut: Double = transaction.txOut.map(_.value).sum
      val totalIn: Double = transaction.txIn.map(_.value).sum
      totalOut <= totalIn
    }

    override def isValidSignature(transaction: Transaction): Boolean = ???

  }

  implicit class TransactionOps(transaction: Transaction) extends SimpleTransactionModule
                                                             with SHA256DigestModule
}
