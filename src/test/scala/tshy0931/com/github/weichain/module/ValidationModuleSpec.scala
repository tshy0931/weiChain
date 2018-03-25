package tshy0931.com.github.weichain.module

import cats.syntax.validated._
import org.scalatest._
import ValidationModule._
import BlockChainModule._
import org.mockito.Matchers.{eq => eql, _}
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import tshy0931.com.github.weichain.model.Block.{BlockBody, BlockHeader}
import tshy0931.com.github.weichain.model.{Block, MerkleTree, Transaction}
import tshy0931.com.github.weichain.model.Transaction.{Input, Output}
import tshy0931.com.github.weichain.Hash

class ValidationModuleSpec extends FlatSpec with GivenWhenThen with BeforeAndAfterAll with Matchers with MockitoSugar with Inside with ValidationModuleFixture {

  override def beforeAll(): Unit = {
    bestLocalBlockChain.putIfAbsent("1", testBlock)
    super.beforeAll()
  }

  override def afterAll(): Unit = {
    bestLocalBlockChain.clear()
    super.afterAll()
  }

  behavior of "Transaction validation"

  it should "validate a valid transaction" in {

    TransactionValidation.verify(txValid) shouldBe txValid.valid
  }

  it should "invalidate a transaction that has output value greater than input value" in {

    TransactionValidation.isValidSpending(txOutputGreaterThanInput) shouldBe ValidationError(s"output (6.0) must not be greater than input (3.0)", txOutputGreaterThanInput).invalid
  }

  it should "invalidate a transaction that has invalid input source" in {

    TransactionValidation.isValidSource(txInvalidInputSourceAddress) shouldBe ValidationError("invalid source tx", txInvalidInputSourceAddress).invalid
  }
}

trait ValidationModuleFixture extends BlockChainModuleFixture {

  val testHash: Hash = "".getBytes("UTF-8")

  val testBlock = Block(
    header = BlockHeader(testHash, 1, testHash, testHash, 1L, 1L, 1L),
    body = BlockBody(
      MerkleTree(Vector.empty[Hash], 0),
      3,
      3L,
      Vector(
        Transaction(testHash, 1, 0, Vector.empty, 3, Vector(txOutput(1, 2.0), txOutput(2, 0.2)), 0, 1L, 0.0),
        Transaction(testHash, 1, 0, Vector.empty, 3, Vector(txOutput(1, 2.0), txOutput(2, 2.0)), 0, 1L, 0.0),
        Transaction(testHash, 1, 0, Vector.empty, 3, Vector(txOutput(1, 0.1), txOutput(2, 0.1)), 0, 1L, 0.0)
      )
    )
  )

  val txValid = Transaction(
    hash = testHash,
    version = 1,
    nTxIn = 2,
    txIn = Vector(
      txInput(1, 1.0, Output(1.2, 1.toString.getBytes("UTF-8"), 1.toString, 0, 0, "scriptPubKey 1")),
      txInput(2, 2.0, Output(2.0, 1.toString.getBytes("UTF-8"), 1.toString, 1, 1, "scriptPubKey 2")),
    ),
    nTxOut = 3,
    txOut = Vector(
      Output(1.0, 1.toString.getBytes("UTF-8"), 1.toString, 1, 1, "scriptPubKey 1"),
      Output(1.0, 2.toString.getBytes("UTF-8"), 2.toString, 2, 2, "scriptPubKey 2"),
      Output(1.0, 300.toString.getBytes("UTF-8"), 3.toString, 1, 1, "scriptPubKey 300")
    ),
    lockTime = 1,
    blockIndex = 1L,
    txFee = 0.1)

  val txOutputGreaterThanInput = Transaction(
    hash = testHash,
    version = 1,
    nTxIn = 2,
    txIn = Vector(
      txInput(1, 1.0, txOutput(1, 0.5)),
      txInput(2, 2.0, txOutput(1, 0.5))
    ),
    nTxOut = 3,
    txOut = Vector(
      txOutput(1, 2.0),
      txOutput(2, 2.0),
      txOutput(3, 2.0),
    ),
    lockTime = 1,
    blockIndex = 1L,
    txFee = 0.1)

  def txInput(number: Int, value: Double, source: Output) = Input(
    value = value,
    source = source,
    address = number.toString.getBytes("UTF-8"),
    scriptSig = s"scriptSig $number"
  )

  def txOutput(number: Int, value: Double) = Output(
    value = value,
    address = number.toString.getBytes("UTF-8"),
    blockHash = number.toString,
    txIndex = number,
    outputIndex = number,
    scriptPubKey = s"scriptPubKey $number"
  )

  val txInvalidInputSourceAddress = Transaction(
    hash = testHash,
    version = 1,
    nTxIn = 3,
    txIn = Vector(
      txInput(1, 1.0, txOutput(1, 50.0)),
      txInput(2, 2.0, txOutput(2, 0.5)),
      txInput(3, 3.0, txOutput(100, 100.0))
    ),
    nTxOut = 3,
    txOut = Vector(
      Output(1.0, 1.toString.getBytes("UTF-8"), 1.toString, 1, 1, "scriptPubKey 1"),
      Output(2.0, 2.toString.getBytes("UTF-8"), 2.toString, 2, 2, "scriptPubKey 2"),
      Output(2.0, 300.toString.getBytes("UTF-8"), 3.toString, 1, 1, "scriptPubKey 300")
    ),
    lockTime = 1,
    blockIndex = 1L,
    txFee = 0.1)
}
