package tshy0931.com.github.weichain.module

import cats._
import cats.data.Validated
import cats.syntax.validated._
import tshy0931.com.github.weichain.model.{Block, Transaction}
import tshy0931.com.github.weichain._
import BlockChainModule._

object ValidationModule {

  case class ValidationError(message: String, transaction: Transaction)

  final case object TransactionValidation {

    type TransactionValidation = Transaction => Validated[ValidationError, Transaction]

    def verify: TransactionValidation =
      tx => isValidSource(tx) andThen isValidSpending

    def isValidSource: TransactionValidation =
      tx => {
        val allSourcesValid: Boolean = tx.txIn forall { in =>
          val srcBlock: Option[Block] = blockWithHash(in.source.blockHash)
          val srcTx: Option[Transaction] = srcBlock map (_.body.transactions(in.source.txIndex))
          val srcOutput: Option[Transaction.Output] = srcTx map (_.txOut(in.source.outputIndex))
println(srcBlock.get.body.transactions+ " - " +in.source.blockHash)
          srcOutput.exists{ srcOut =>
            println(hashToString(srcOut.address))
            println(hashToString(in.address))
            (srcOut.address sameElements in.address) && srcOut.value >= in.value}
        }

        if(allSourcesValid) tx.valid
        else ValidationError("invalid source tx", tx).invalid
      }

    def isValidSpending: TransactionValidation =
      tx => {
        val totalOut: Double = tx.txOut.map(_.value).sum
        val totalIn: Double = tx.txIn.map(_.value).sum

        if(totalOut <= totalIn) tx.valid
        else ValidationError(s"output ($totalOut) must not be greater than input ($totalIn)", tx).invalid
      }

    def isValidSignature: TransactionValidation = ??? //TODO: implement when signature module is done

    def isValidScriptResult: TransactionValidation = ??? //TODO: execute scriptSig and scriptPubKey to validate tx
  }



}
