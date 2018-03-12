package tshy0931.com.github.weichain.model

import Transaction._
import tshy0931.com.github.weichain.model.Script.{PubKey, Sig}

case class Transaction(metadata: Metadata,
                       inputs: List[Input],
                       totalIn: Double,
                       outputs: List[Output],
                       totalOut: Double,
                       coinbase: Option[String],
                       fee: Double
                      )

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
                      blockIndex: Int,
                      inputSize: Int,
                      outputSize: Int,
                      lockTime: Int,
                      size: Long)

  case class Input(source: Pointer,
                   address: String,
                   value: Double,
                   scriptSig: Sig)

  case class Output(value: Double,
                    address: String,
                    index: Int,
                    scriptPubKey: PubKey)

  case class Pointer(hash: String,
                     blockIndex: Int,
                     transactionIndex: Int,
                     outputIndex: Int)

}
