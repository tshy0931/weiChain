package tshy0931.com.github.weichain.module

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

}

object TransactionModule {

  case class Error(message: String, transaction: Transaction)

  trait SimpleTransactionModule extends TransactionModule { this: DigestModule =>

    override def isValidSource(transaction: Transaction)(implicit blockchain: BlockChain): Boolean = {

      transaction.inputs forall { in =>
        val srcBlock: Block = blockchain.blockAt(in.source.blockIndex)
        val srcTx: Transaction = srcBlock.body.transactions(in.source.transactionIndex)
        val srcOutput: Transaction.Output = srcTx.outputs(in.source.outputIndex)

        srcOutput.address == in.address && srcOutput.value == in.value
      }
    }

    override def isValidSpending(transaction: Transaction): Boolean =
      transaction.coinbase.isDefined || transaction.totalOut + transaction.fee == transaction.totalIn

    override def isValidSignature(transaction: Transaction): Boolean = ???

  }

  implicit class TransactionOps(transaction: Transaction) extends SimpleTransactionModule
                                                             with SHA256DigestModule
}
