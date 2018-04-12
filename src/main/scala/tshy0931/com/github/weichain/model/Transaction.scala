package tshy0931.com.github.weichain.model

import Transaction._
import monocle.function.all._
import monocle.{Lens, Traversal}
import monocle.macros.GenLens
import tshy0931.com.github.weichain._
import tshy0931.com.github.weichain.module.DigestModule._

/** Example: https://bitcoin.org/en/developer-reference#raw-transaction-format
  * 01000000 ................................... Version

    01 ......................................... Number of inputs
    |
    | 7b1eabe0209b1fe794124575ef807057
    | c77ada2138ae4fa8d6c4de0398a14f3f ......... Outpoint TXID
    | 00000000 ................................. Outpoint index number
    |
    | 49 ....................................... Bytes in sig. script: 73
    | | 48 ..................................... Push 72 bytes as data
    | | | 30450221008949f0cb400094ad2b5eb3
    | | | 99d59d01c14d73d8fe6e96df1a7150de
    | | | b388ab8935022079656090d7f6bac4c9
    | | | a94e0aad311a4268e082a725f8aeae05
    | | | 73fb12ff866a5f01 ..................... Secp256k1 signature
    |
    | ffffffff ................................. Sequence number: UINT32_MAX

    01 ......................................... Number of outputs
    | f0ca052a01000000 ......................... Satoshis (49.99990000 BTC)
    |
    | 19 ....................................... Bytes in pubkey script: 25
    | | 76 ..................................... OP_DUP
    | | a9 ..................................... OP_HASH160
    | | 14 ..................................... Push 20 bytes as data
    | | | cbc20a7664f2f69e5355aa427045bc15
    | | | e7c6c772 ............................. PubKey hash
    | | 88 ..................................... OP_EQUALVERIFY
    | | ac ..................................... OP_CHECKSIG

    00000000 ................................... locktime: 0 (a block height)
  */
case class Transaction(hash: Hash = emptyHash,
                       version: Int,
                       nTxIn: Int,
                       txIn: Vector[Input],
                       nTxOut: Int,
                       txOut: Vector[Output],
                       lockTime: Int = 0,
                       blockIndex: Long = -1,
                       txFee: Double = 0.0,
                       createTime: Long = System.currentTimeMillis)

object Transaction {

  case class Input(source: Output,
                   scriptSig: String,
                   sequence: Long = 0xffffffff)

  case class Output(value: Double,
                    address: Hash,
                    blockHash: Hash = emptyHash,
                    txIndex: Int = -1,
                    outputIndex: Int,
                    scriptPubKey: String,
                    coinbase: Option[Coinbase] = None)

  case class Coinbase(script: String)

  lazy val hashLens: Lens[Transaction, Hash] = GenLens[Transaction](_.hash)
  lazy val txFeeLens: Lens[Transaction, Double] = GenLens[Transaction](_.txFee)
  lazy val outputLens: Lens[Transaction, Vector[Output]] = GenLens[Transaction](_.txOut)
  lazy val outputBlockHashLens: Lens[Output, Hash] = GenLens[Output](_.blockHash)
  lazy val outputTxIndexLens: Lens[Output, Int] = GenLens[Output](_.txIndex)
  lazy val outputIndexLens: Lens[Output, Int] = GenLens[Output](_.outputIndex)

  lazy val txOutputTraversal: Traversal[Transaction, Output] = outputLens composeTraversal each
  lazy val txOutputBlockHashTraversal: Traversal[Transaction, Hash] = txOutputTraversal composeLens outputBlockHashLens
  lazy val txOutputTxIndexTraversal: Traversal[Transaction, Int] = txOutputTraversal composeLens outputTxIndexLens
  lazy val txOutputIndexTraversal: Traversal[Transaction, Int] = txOutputTraversal composeLens outputIndexLens

  implicit class TransactionOps(tx: Transaction) {

    def updated: Transaction = {
      val hash = digest(digest(s"""${tx.createTime}
                       |${tx.lockTime}
                       |${tx.txOut map outputHash mkString}
                       |${tx.txIn map inputHash mkString}
                       |""".stripMargin))
      hashLens.set(hash)(tx)
    }
  }

  private def outputHash(output: Output): Hash =
    digest(digest(s"""${output.value}
                     |${output.address}
                     |${output.scriptPubKey}
                     |${output.coinbase}
                     |""".stripMargin))

  private def inputHash(input: Input): Hash =
    digest(digest(s"""${outputHash(input.source)}
                     |${input.scriptSig}
                     |${input.sequence}
                     |""".stripMargin))
}
