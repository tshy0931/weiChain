package tshy0931.com.github.weichain.testdata

import tshy0931.com.github.weichain._
import tshy0931.com.github.weichain.model.{MerkleTree, Transaction}
import tshy0931.com.github.weichain.model.Transaction.{Coinbase, Input, Output}

trait TransactionTestData { this: CommonData =>

  def txValid(hash: Hash = testHash) = Transaction(
    hash = hash,
    version = 1,
    nTxIn = 2,
    txIn = Vector(
      txInput(1, 1.0, Output(1.2, 1.toString, 1.toString, 0, 0, "scriptPubKey 1")),
      txInput(2, 2.0, Output(2.0, 1.toString, 1.toString, 1, 1, "scriptPubKey 2")),
    ),
    nTxOut = 3,
    txOut = Vector(
      Output(1.0, 1.toString, 1.toString, 1, 1, "scriptPubKey 1"),
      Output(1.0, 2.toString, 2.toString, 2, 2, "scriptPubKey 2"),
      Output(1.0, 300.toString, 3.toString, 1, 1, "scriptPubKey 300")
    ),
    lockTime = 1,
    blockIndex = 1L,
    txFee = 0.1,
    createTime = 0L)

  val coinbase1 = Output(
    value = 50.0,
    address = testAddress(1),
    txIndex = 0,
    outputIndex = 0,
    scriptPubKey = "scriptPubKey1",
    coinbase = Some(Coinbase("coinbase to testAddress1"))
  )
  // output 2/3/4 come from coinbase1
  val output2 = Output(
    value = 20.0,
    address = testAddress(2),
    txIndex = -1,
    outputIndex = 0,
    scriptPubKey = "scriptPubKey2"
  )
  val output3 = Output(
    value = 12.0,
    address = testAddress(3),
    txIndex = -1,
    outputIndex = 1,
    scriptPubKey = "scriptPubKey3"
  )
  val output4 = Output(
    value = 17.5,
    address = testAddress(4),
    txIndex = -1,
    outputIndex = 2,
    scriptPubKey = "scriptPubKey4"
  )
  val output5 = Output(
    value = 10.0,
    address = testAddress(5),
    txIndex = -1,
    outputIndex = 0,
    scriptPubKey = "scriptPubKey5"
  )
  val output6 = Output(
    value = 27.0,
    address = testAddress(6),
    txIndex = -1,
    outputIndex = 1,
    scriptPubKey = "scriptPubKey6"
  )

  // coinbase tx
  val validTx1 = Transaction(
    hash = testHash,
    version = 1,
    nTxIn = 0,
    txIn = Vector.empty,
    nTxOut = 1,
    txOut = Vector(coinbase1),
    blockIndex = -1L,
    txFee = 0.1,
    createTime = 1L
  ) updated

  val validTx2 = Transaction(
    hash = testHash,
    version = 1,
    nTxIn = 1,
    txIn = Vector(Input(
      source = coinbase1,
      scriptSig = "sig1"
    )),
    nTxOut = 3,
    txOut = Vector(output2, output3, output4),
    blockIndex = -1L,
    txFee = 0.5,
    createTime = 2L
  ) updated

  val validTx3 = Transaction(
    version = 1,
    nTxIn = 2,
    txIn = Vector(
      Input(output2, "sig3"),
      Input(output4, "sig3")
    ),
    nTxOut = 2,
    txOut = Vector(output5, output6)
  ) updated

  val merkleTree1 = MerkleTree.build(Vector(validTx1))
  val merkleTree2 = MerkleTree.build(Vector(validTx2))

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
    address = number.toString,
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
      Output(1.0, 1.toString, 1.toString, 1, 1, "scriptPubKey 1"),
      Output(2.0, 2.toString, 2.toString, 2, 2, "scriptPubKey 2"),
      Output(2.0, 300.toString, 3.toString, 1, 1, "scriptPubKey 300")
    ),
    lockTime = 1,
    blockIndex = 1L,
    txFee = 0.1,
    createTime = 0L
  ) updated
}
