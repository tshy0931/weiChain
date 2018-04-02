package tshy0931.com.github.weichain.module

import cats.syntax.all._
import tshy0931.com.github.weichain.model.{Block, Transaction}
import tshy0931.com.github.weichain._
import BlockChainModule._
import tshy0931.com.github.weichain.model.Block.BlockHeader
import DigestModule._
import MiningModule.difficulty
import cats.data.{OptionT, Validated}
import cats.data.Validated.{Invalid, Valid}
import monix.eval.Task

import scala.concurrent.Future

object ValidationModule {

  type Validation[A] = A => Task[Validated[ValidationError[A], A]]

  case class ValidationError[A](message: String, item: A)

  final case object BlockHeaderValidation {

    type BlockHeaderValidation = (BlockHeader, BlockHeader) => Task[Validated[ValidationError[BlockHeader], BlockHeader]]

    def verifyHeaders: BlockHeaderValidation = (prevHeader, currHeader) =>
      (isValidHash(currHeader), isValidLink(prevHeader, currHeader)) mapN {
        case (Valid(header), Valid(_)) => header.valid
        case (Invalid(err1), _) => err1.invalid
        case (_, Invalid(err2)) => err2.invalid
      }

    /**
      * compute hash of the block header. it should be the same as given in [[BlockHeader.hash]]
      * And the hash value should suffice the difficulty at block creation time.
      */
    def isValidHash: Validation[BlockHeader] = header => Task {
      val hashString = header.computeHash.asString
      if (hashString == header.hash.asString && isValidProofOfWork(hashString)) header.valid
      else ValidationError("invalid header hash", header).invalid
    }

    /**
      * hash of previous header should match prevBlockHash in current header.
      */
    def isValidLink(prevHeader: BlockHeader, currHeader: BlockHeader): Task[Validated[ValidationError[BlockHeader], BlockHeader]] =
      Task.eval {
        if(prevHeader.computeHash.asString == currHeader.prevHeaderHash.asString) currHeader.valid
        else ValidationError("prevBlockHash doesn't match with previous block", currHeader).invalid
      }

    def isValidProofOfWork: String => Boolean = _.startsWith(difficulty)
  }

  final case object TransactionValidation {
    
    def verifyTx: Validation[Transaction] = tx =>
      (isValidSource(tx), isValidSpending(tx)) mapN {
        case (Valid(tx), Valid(_)) => tx.valid
        case (Invalid(err1), _)     => err1.invalid
        case (_, Invalid(err2))     => err2.invalid
      }

    def isValidSource: Validation[Transaction] = tx => {

      val allSourcesValid: Vector[Task[Boolean]] = tx.txIn map { in =>
        for {
          srcBlock <- blockWithHash(in.source.blockHash.asString).value
          srcTx <- Task.now { srcBlock map (_.body.transactions(in.source.txIndex)) }
          srcOut <- Task.now { srcTx map {_.txOut(in.source.outputIndex)} }
        } yield srcOut.exists(out => (out.address sameElements in.address) && out.value >= in.value)
//        val srcBlock: Option[Block] = blockWithHash(in.source.blockHash.asString).value
//        val srcTx: Option[Transaction] = srcBlock map (_.body.transactions(in.source.txIndex))
//        val srcOutput: Option[Transaction.Output] = srcTx map (_.txOut(in.source.outputIndex))
//
//        srcOutput.exists{ srcOut => (srcOut.address sameElements in.address) && srcOut.value >= in.value }
      }

      Task.gatherUnordered(allSourcesValid) map { results =>
        if (results.forall(r => r)) tx.valid else ValidationError("invalid source tx", tx).invalid
      }
    }

    def isValidSpending: Validation[Transaction] =
      tx => Task {
        val totalOut: Double = tx.txOut.map(_.value).sum
        val totalIn: Double = tx.txIn.map(_.value).sum

        if(totalOut <= totalIn) tx.valid
        else ValidationError(s"output ($totalOut) must not be greater than input ($totalIn)", tx).invalid
      }

    def isValidSignature: Validation[Transaction] = ??? //TODO: implement when signature module is done

    def isValidScriptResult: Validation[Transaction] = ??? //TODO: execute scriptSig and scriptPubKey to validate tx
  }
}
