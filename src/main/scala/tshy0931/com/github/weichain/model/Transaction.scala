package tshy0931.com.github.weichain.model

import Transaction._
import tshy0931.com.github.weichain.{ScriptPubKey, ScriptSig}

case class Transaction(metadata: Metadata,
                       inputs: List[Input],
                       outputs: List[Output])

object Transaction {

  /**
    *
    * @param transactionHash
    * @param version
    * @param inputSize
    * @param outputSize
    * @param lockTime block index or real-world timestamp before which this transaction cannot be published.
    * @param size
    */
  case class Metadata(transactionHash: String,
                      version: Int,
                      inputSize: Int,
                      outputSize: Int,
                      lockTime: Int,
                      size: Long)

  case class Input(source: Pointer,
                   coinbase: Option[String],
                   scriptSig: Option[ScriptSig])

  case class Output(value: Double,
                    scriptPubKey: ScriptPubKey)

  case class Pointer(hash: String,
                     index: Int)

}
