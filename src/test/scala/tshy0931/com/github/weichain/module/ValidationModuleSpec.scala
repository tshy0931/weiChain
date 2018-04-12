package tshy0931.com.github.weichain.module

import cats.syntax.all._
import org.scalatest._
import ValidationModule._
import cats.data.Validated.{Invalid, Valid}
import monix.eval.Task
import tshy0931.com.github.weichain.database.{Database, Identity}
import tshy0931.com.github.weichain.model.Block.{BlockBody, BlockHeader}
import tshy0931.com.github.weichain.protobuf.Protobufable
import tshy0931.com.github.weichain.testdata.{BlockTestData, CommonData, TransactionTestData}

import scala.util.{Failure, Success}
import scala.concurrent.duration._

class ValidationModuleSpec extends FlatSpec with GivenWhenThen with BeforeAndAfterAll with Matchers with Inside with ValidationModuleFixture {

  import monix.execution.Scheduler.Implicits.global

  behavior of "Transaction validation"

  it should "validate a valid transaction" in {

    verifyWithMockDBs{
      TransactionValidation.verifyTx(validTx3) runOnComplete {
        case Success(Valid(tx))  => tx shouldBe validTx3
        case Success(Invalid(err)) => fail(s"should not be invalid, $err")
        case Failure(err) => fail(err)
      }
    } runSyncUnsafe(30 seconds)
  }

  it should "invalidate a transaction that has output value greater than input value" in {

    TransactionValidation.isValidSpending(txOutputGreaterThanInput) runOnComplete {
      case Success(tx)  => tx shouldBe ValidationError(s"output (6.0) must not be greater than input (3.0)", txOutputGreaterThanInput).invalid
      case Failure(err) => fail(err)
    }
  }

  it should "invalidate a transaction that has invalid input source" in {

    verifyWithMockDBs{
      TransactionValidation.isValidSource(txInvalidInputSourceAddress) runOnComplete {
        case Success(tx)  => tx shouldBe ValidationError("invalid source tx", txInvalidInputSourceAddress).invalid
        case Failure(err) => fail(err)
      }
    } runSyncUnsafe(30 seconds)
  }
}

trait ValidationModuleFixture extends BlockChainModuleFixture
  with BlockTestData
  with TransactionTestData
  with CommonData {

  import Identity._

  def mockDB[A: Identity] = new Database[A] {

    private[this] var db = Map.empty[String, A]

    override def save(item: A)(implicit pb: Protobufable[A]): Task[Boolean] =
      Task.now{db += (item.key -> item); true}

    override def find(key: String)(implicit pb: Protobufable[A]): Task[Option[A]] =
      Task.now{ db.get(key) }

    override def deleteKeys(keys: String*)(implicit pb: Protobufable[A]): Task[Unit] = ???
    override def deleteItems(items: A*)(implicit pb: Protobufable[A]): Task[Unit] = ???
  }

  def verifyWithMockDBs(assertion: => Unit): Task[Unit] = {
    implicit val blockHeaderDB: Database[BlockHeader] = mockDB[BlockHeader]
    implicit val blockBodyDB: Database[BlockBody] = mockDB[BlockBody]

    blockHeaderDB.save(BlockHeader(testHash, 1, testHash, testHash, height = 1))
    (
      blockHeaderDB.save(blk1.header),
      blockHeaderDB.save(blk2.header),
      blockBodyDB.save(blk1.body),
      blockBodyDB.save(blk2.body)
    ) parMapN { (_,_,_,_) =>

      assertion
    }
  }
}
