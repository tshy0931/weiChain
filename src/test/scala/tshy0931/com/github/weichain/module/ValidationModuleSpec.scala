package tshy0931.com.github.weichain.module

import cats.syntax.validated._
import org.scalatest._
import ValidationModule._
import tshy0931.com.github.weichain.testdata.{CommonData, TransactionTestData}

class ValidationModuleSpec extends FlatSpec with GivenWhenThen with BeforeAndAfterAll with Matchers with Inside with ValidationModuleFixture {

  behavior of "Transaction validation"

  it should "validate a valid transaction" in {

    TransactionValidation.verifyTx(txValid()) shouldBe txValid().valid
  }

  it should "invalidate a transaction that has output value greater than input value" in {

    TransactionValidation.isValidSpending(txOutputGreaterThanInput) shouldBe ValidationError(s"output (6.0) must not be greater than input (3.0)", txOutputGreaterThanInput).invalid
  }

  it should "invalidate a transaction that has invalid input source" in {

    TransactionValidation.isValidSource(txInvalidInputSourceAddress) shouldBe ValidationError("invalid source tx", txInvalidInputSourceAddress).invalid
  }
}

trait ValidationModuleFixture extends BlockChainModuleFixture
  with TransactionTestData
  with CommonData
