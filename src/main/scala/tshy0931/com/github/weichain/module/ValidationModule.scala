package tshy0931.com.github.weichain.module

import cats.data.Validated
import cats.syntax.validated._
import tshy0931.com.github.weichain.model.{Block, Transaction}
import tshy0931.com.github.weichain._
import BlockChainModule._
import tshy0931.com.github.weichain.model.Block.BlockHeader
import DigestModule._
import MiningModule.difficulty

object ValidationModule {

  type Validation[A] = A => Validated[ValidationError[A], A]

  case class ValidationError[A](message: String, item: A)

  final case object BlockHeaderValidation {

    type BlockHeaderValidation = (BlockHeader, BlockHeader) => Validated[ValidationError[BlockHeader], BlockHeader]

    def verifyHeaders: BlockHeaderValidation = (prevHeader, currHeader) =>
      isValidHash(currHeader) andThen { isValidLink(prevHeader, _) }

    /**
      * compute hash of the block header. it should be the same as given in [[BlockHeader.hash]]
      * And the hash value should suffice the difficulty at block creation time.
      */
    def isValidHash: Validation[BlockHeader] = header => {
      val hashString = header.computeHash.asString
      if (hashString == header.hash.asString && isValidProofOfWork(hashString)) header.valid
      else ValidationError("invalid header hash", header).invalid
    }

    /**
      * hash of previous header should match prevBlockHash in current header.
      */
    def isValidLink(prevHeader: BlockHeader, currHeader: BlockHeader): Validated[ValidationError[BlockHeader], BlockHeader] = {

      if(prevHeader.computeHash.asString == currHeader.prevHeaderHash.asString) currHeader.valid
      else ValidationError("prevBlockHash doesn't match with previous block", currHeader).invalid
    }

    def isValidProofOfWork: String => Boolean = _.startsWith(difficulty)
  }

  final case object TransactionValidation {
    
    def verifyTx: Validation[Transaction] = tx =>
      isValidSource(tx) andThen isValidSpending

    def isValidSource: Validation[Transaction] = tx => {
      val allSourcesValid: Boolean = tx.txIn forall { in =>
        val srcBlock: Option[Block] = blockWithHash(in.source.blockHash.asString)
        val srcTx: Option[Transaction] = srcBlock map (_.body.transactions(in.source.txIndex))
        val srcOutput: Option[Transaction.Output] = srcTx map (_.txOut(in.source.outputIndex))

        srcOutput.exists{ srcOut =>
          println(srcOut.address.asString)
          println(in.address.asString)
          (srcOut.address sameElements in.address) && srcOut.value >= in.value}
      }

      if(allSourcesValid) tx.valid
      else ValidationError("invalid source tx", tx).invalid
    }

    def isValidSpending: Validation[Transaction] =
      tx => {
        val totalOut: Double = tx.txOut.map(_.value).sum
        val totalIn: Double = tx.txIn.map(_.value).sum

        if(totalOut <= totalIn) tx.valid
        else ValidationError(s"output ($totalOut) must not be greater than input ($totalIn)", tx).invalid
      }

    def isValidSignature: Validation[Transaction] = ??? //TODO: implement when signature module is done

    def isValidScriptResult: Validation[Transaction] = ??? //TODO: execute scriptSig and scriptPubKey to validate tx
  }
}
