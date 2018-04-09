package tshy0931.com.github.weichain.testdata

import monix.eval.Task
import tshy0931.com.github.weichain._
import tshy0931.com.github.weichain.module.BlockChainModule._
import tshy0931.com.github.weichain.module.DigestModule._
import tshy0931.com.github.weichain.module.MiningModule._
import tshy0931.com.github.weichain.model.{Block, MerkleTree, Transaction}
import tshy0931.com.github.weichain.model.Transaction.{Coinbase, Input, Output}

import scala.concurrent.duration._
import monix.execution.Scheduler.Implicits.global


trait TestApplicationData extends CommonData {

  val testAddress1: Hash = digest("testAddress1")
  val testAddress2: Hash = digest("testAddress2")
  val testAddress3: Hash = digest("testAddress3")
  val testAddress4: Hash = digest("testAddress4")
  val testAddress5: Hash = digest("testAddress5")

  val coinbase1 = Output(
    value = 50.0,
    address = testAddress1,
    blockHash = emptyHash,
    txIndex = 0,
    outputIndex = 0,
    scriptPubKey = "scriptPubKey1",
    coinbase = Some(Coinbase("coinbase to testAddress1"))
  )
  // output 2/3/4 come from coinbase1
  val output2 = Output(
    value = 20.0,
    address = testAddress2,
    blockHash = emptyHash,
    txIndex = 0,
    outputIndex = 0,
    scriptPubKey = "scriptPubKey2"
  )
  val output3 = Output(
    value = 12.0,
    address = testAddress3,
    blockHash = emptyHash,
    txIndex = 0,
    outputIndex = 0,
    scriptPubKey = "scriptPubKey3"
  )
  val output4 = Output(
    value = 17.5,
    address = testAddress3,
    blockHash = emptyHash,
    txIndex = 0,
    outputIndex = 0,
    scriptPubKey = "scriptPubKey3"
  )
  // 0.5 coin is for tx fee

  // coinbase tx
  val tx1 = Transaction(
    hash = testHash,
    version = 1,
    nTxIn = 0,
    txIn = Vector.empty,
    nTxOut = 1,
    txOut = Vector(coinbase1),
    lockTime = 0,
    blockIndex = 1L,
    txFee = 0.1,
    createTime = 1L
  ) updated

  val tx2 = Transaction(
    hash = testHash,
    version = 1,
    nTxIn = 1,
    txIn = Vector(Input(
      source = coinbase1,
      scriptSig = "sig1"
    )),
    nTxOut = 3,
    txOut = Vector(output2, output3, output4),
    lockTime = 0,
    blockIndex = 1L,
    txFee = 0.5,
    createTime = 2L
  ) updated

  val merkleTree1 = MerkleTree.build(Vector(tx1))
  val merkleTree2 = MerkleTree.build(Vector(tx2))

  lazy val blk1 = mineWithTransactions(Vector(tx1), genesisBlock.value) runSyncUnsafe(120 seconds)
  lazy val blk2 = mineWithTransactions(Vector(tx2), genesisBlock.value) runSyncUnsafe(120 seconds)
}
