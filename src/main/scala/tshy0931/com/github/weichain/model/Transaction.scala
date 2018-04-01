package tshy0931.com.github.weichain.model

import Transaction._
import shapeless.tag.@@
import tshy0931.com.github.weichain.Hash

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
case class Transaction(hash: Hash,
                       version: Int,
                       nTxIn: Int,
                       txIn: Vector[Input],
                       nTxOut: Int,
                       txOut: Vector[Output],
                       lockTime: Int,
                       blockIndex: Long,
                       txFee: Double)

object Transaction {

  type BlockCountTag
  type EpochTimeMillisTag
  type BlockCount = Long @@ BlockCountTag
  type EpochTimeMillis = Long @@ EpochTimeMillisTag

  case class Input(value: Double,
                   source: Output,
                   address: Hash,
                   scriptSig: String,
                   sequence: Long = 0xffffffff)

  case class Output(value: Double,
                    address: Hash,
                    blockHash: Hash,
                    txIndex: Int,
                    outputIndex: Int,
                    scriptPubKey: String,
                    coinbase: Option[Coinbase] = None)

  case class Coinbase(script: String)
}
