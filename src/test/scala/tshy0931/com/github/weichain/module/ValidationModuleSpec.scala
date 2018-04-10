package tshy0931.com.github.weichain.module

import cats.syntax.validated._
import org.scalatest._
import ValidationModule._
import tshy0931.com.github.weichain.testdata.{CommonData, TransactionTestData}

import scala.util.{Failure, Success}

class ValidationModuleSpec extends FlatSpec with GivenWhenThen with BeforeAndAfterAll with Matchers with Inside with ValidationModuleFixture {

  import monix.execution.Scheduler.Implicits.global

  behavior of "Transaction validation"

  it should "validate a valid transaction" in {

    TransactionValidation.verifyTx(txValid()) runOnComplete {
      case Success(tx)  => tx shouldBe txValid().valid
      case Failure(err) => fail(err)
    }
  }

  it should "invalidate a transaction that has output value greater than input value" in {

    TransactionValidation.isValidSpending(txOutputGreaterThanInput) runOnComplete {
      case Success(tx)  => tx shouldBe ValidationError(s"output (6.0) must not be greater than input (3.0)", txOutputGreaterThanInput).invalid
      case Failure(err) => fail(err)
    }
  }

  it should "invalidate a transaction that has invalid input source" in {

    TransactionValidation.isValidSource(txInvalidInputSourceAddress) runOnComplete {
      case Success(tx)  => tx shouldBe ValidationError("invalid source tx", txInvalidInputSourceAddress).invalid
      case Failure(err) => fail(err)
    }
  }
}

trait ValidationModuleFixture extends BlockChainModuleFixture
  with TransactionTestData
  with CommonData
