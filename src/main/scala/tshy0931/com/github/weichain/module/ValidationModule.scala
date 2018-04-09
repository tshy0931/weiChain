package tshy0931.com.github.weichain.module

import cats.syntax.all._
import tshy0931.com.github.weichain.model.{MerkleTree, Transaction}
import tshy0931.com.github.weichain._
import BlockChainModule._
import tshy0931.com.github.weichain.model.Block.{BlockBody, BlockHeader}
import MiningModule.difficulty
import cats.data.Validated
import cats.data.Validated.{Invalid, Valid}
import monix.eval.Task

object ValidationModule {

  type Validation[A] = A => Task[Validated[ValidationError[A], A]]

  case class ValidationError[A](message: String, item: A)

  final case object BlockHeaderValidation {

    type BlockHeaderValidation = (BlockHeader, BlockHeader) => Task[Validated[ValidationError[BlockHeader], BlockHeader]]

    def verifyBlockHeaders: BlockHeaderValidation = (prevHeader, currHeader) =>
      (isValidHash(currHeader), isValidLink(prevHeader, currHeader)) parMapN {
        case (Valid(header), Valid(_)) => header.valid
        case (Invalid(err1), _) => err1.invalid
        case (_, Invalid(err2)) => err2.invalid
      }

    /**
      * compute hash of the block header. it should be the same as given in [[BlockHeader.hash]]
      * And the hash value should suffice the difficulty at block creation time.
      */
    def isValidHash: Validation[BlockHeader] = header => Task {
      val hashString = header.computeNoncedHash.asString
      if (hashString == header.hash.asString && isValidProofOfWork(hashString)) header.valid
      else ValidationError("invalid header hash", header).invalid
    }

    /**
      * hash of previous header should match prevBlockHash in current header.
      */
    def isValidLink(prevHeader: BlockHeader, currHeader: BlockHeader): Task[Validated[ValidationError[BlockHeader], BlockHeader]] =
      Task.eval {
        if(prevHeader.computeNoncedHash.asString == currHeader.prevHeaderHash.asString) currHeader.valid
        else ValidationError("prevBlockHash doesn't match with previous block", currHeader).invalid
      }

    def isValidProofOfWork: String => Boolean = _.startsWith(difficulty)
  }

  final case object BlockBodyValidation {

    import TransactionValidation._

    type BlockBodyValidation = BlockBody => Task[Validated[ValidationError[BlockBody], BlockBody]]

    def verifyBlockBody: BlockBodyValidation = body =>
      (hasValidTx(body), hasValidMerkleRoot(body), hasValidSize(body)) parMapN {
        case (Valid(_), Valid(_), Valid(_)) => body.valid
        case (err @ Invalid(_), _, _) => err
        case (_, err @ Invalid(_), _) => err
        case (_, _, err @ Invalid(_)) => err
      }

    def hasValidTx: BlockBodyValidation = body => Task.defer {
      Task gatherUnordered { body.transactions map { verifyTx } } map { list =>
      // TODO: any chance to use EitherT here?
//        val eitherList: List[Either[ValidationError[Transaction], Transaction]] = list map { _.toEither }
//        val et = EitherT(eitherList)
      if(list collect { case Invalid(err) => err } nonEmpty){
        ValidationError[BlockBody]("Block body contains invalid tx", body).invalid
      } else {
        body.valid
      }}
    }

    def hasValidMerkleRoot: BlockBodyValidation = body => Task {
      val merkleTree = MerkleTree.build(body.transactions)
      if(merkleTree.root.asString == body.merkleTree.root.asString) {
        body.valid
      } else {
        ValidationError[BlockBody]("invalid merkle root", body).invalid
      }
    }

    def hasValidSize: BlockBodyValidation = body => Task {
      if(body.nTx == body.transactions.size) body.valid
      else ValidationError("nTx is not equal to actual number of tx", body).invalid
    }
  }

  final case object TransactionValidation {
    
    def verifyTx: Validation[Transaction] = tx =>
      (isValidSource(tx), isValidSpending(tx)) mapN {
        case (Valid(tx), Valid(_)) => tx.updated.valid
        case (Invalid(err1), _)    => err1.invalid
        case (_, Invalid(err2))    => err2.invalid
      }

    def isValidSource: Validation[Transaction] = tx => {

      val allSourcesValid: Vector[Task[Boolean]] = tx.txIn map { in =>
        for {
          srcBlock <- blockWithHash(in.source.blockHash.asString).value
          srcTx <- Task.now { srcBlock map (_.body.transactions(in.source.txIndex)) }
          srcOut <- Task.now { srcTx map {_.txOut(in.source.outputIndex)} }
        } yield srcOut.exists(out => (out.address sameElements in.source.address) && out.value == in.source.value)
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
        val totalIn: Double = tx.txIn.map(_.source.value).sum

        if(totalOut <= totalIn) tx.valid
        else ValidationError(s"output ($totalOut) must not be greater than input ($totalIn)", tx).invalid
      }

    def isValidSignature: Validation[Transaction] = ??? //TODO: implement when signature module is done

    def isValidScriptResult: Validation[Transaction] = ??? //TODO: execute scriptSig and scriptPubKey to validate tx
  }
}
