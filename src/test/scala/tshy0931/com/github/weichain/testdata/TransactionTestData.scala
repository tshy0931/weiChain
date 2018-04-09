package tshy0931.com.github.weichain.testdata

import tshy0931.com.github.weichain.Hash
import tshy0931.com.github.weichain.model.Transaction
import tshy0931.com.github.weichain.model.Transaction.{Input, Output}

trait TransactionTestData { this: CommonData =>

  def txValid(hash: Hash = testHash) = Transaction(
    hash = hash,
    version = 1,
    nTxIn = 2,
    txIn = Vector(
      txInput(1, 1.0, Output(1.2, 1.toString.getBytes("UTF-8"), 1.toString.getBytes("UTF-8"), 0, 0, "scriptPubKey 1")),
      txInput(2, 2.0, Output(2.0, 1.toString.getBytes("UTF-8"), 1.toString.getBytes("UTF-8"), 1, 1, "scriptPubKey 2")),
    ),
    nTxOut = 3,
    txOut = Vector(
      Output(1.0, 1.toString.getBytes("UTF-8"), 1.toString.getBytes("UTF-8"), 1, 1, "scriptPubKey 1"),
      Output(1.0, 2.toString.getBytes("UTF-8"), 2.toString.getBytes("UTF-8"), 2, 2, "scriptPubKey 2"),
      Output(1.0, 300.toString.getBytes("UTF-8"), 3.toString.getBytes("UTF-8"), 1, 1, "scriptPubKey 300")
    ),
    lockTime = 1,
    blockIndex = 1L,
    txFee = 0.1,
    createTime = 0L)

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
    txFee = 0.1,
    createTime = 0L)

  def txInput(number: Int, value: Double, source: Output) = Input(
    source = source,
    scriptSig = s"scriptSig $number"
  )

  def txOutput(number: Int, value: Double) = Output(
    value = value,
    address = number.toString.getBytes("UTF-8"),
    blockHash = number.toString.getBytes("UTF-8"),
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
      Output(1.0, 1.toString.getBytes("UTF-8"), 1.toString.getBytes("UTF-8"), 1, 1, "scriptPubKey 1"),
      Output(2.0, 2.toString.getBytes("UTF-8"), 2.toString.getBytes("UTF-8"), 2, 2, "scriptPubKey 2"),
      Output(2.0, 300.toString.getBytes("UTF-8"), 3.toString.getBytes("UTF-8"), 1, 1, "scriptPubKey 300")
    ),
    lockTime = 1,
    blockIndex = 1L,
    txFee = 0.1,
    createTime = 0L)
}
