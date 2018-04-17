package tshy0931.com.github.weichain.codec

import akka.event.Logging
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.{DefaultJsonProtocol, JsString, JsValue, RootJsonFormat}
import tshy0931.com.github.weichain._
import tshy0931.com.github.weichain.message._
import tshy0931.com.github.weichain.model.Block.{BlockBody, BlockHeader}
import tshy0931.com.github.weichain.model.{Address, Block, MerkleTree, Transaction}
import tshy0931.com.github.weichain.model.Transaction.{Coinbase, Input, Output}
import tshy0931.com.github.weichain.module.NetworkModule.system
import tshy0931.com.github.weichain.module.ValidationModule.ValidationError

object CodecModule extends SprayJsonSupport with DefaultJsonProtocol {

  lazy val log = Logging(system, this.getClass)

//  implicit val byteArrayFormat = new RootJsonFormat[Hash] {
//    override def read(json: JsValue): Hash = json match {
//      case JsString(hash) => hash
//      case _ => emptyHash
//    }
//
//    override def write(hash: Hash): JsValue = JsString(hash)
//  }

  implicit val versionFormat = jsonFormat6(Version)
  implicit val coinbaseFormat = jsonFormat1(Coinbase)
  implicit val txOutputFormat = jsonFormat7(Output.apply)
  implicit val txInputFormat = jsonFormat3(Input)
  implicit val transactionFormat = jsonFormat10(Transaction.apply)
  implicit val blockHeaderFormat = jsonFormat7(BlockHeader)
  implicit val merkleTreeFormat = jsonFormat(MerkleTree.apply, "hashes", "nTx")
  implicit val blockBodyFormat = jsonFormat5(BlockBody)
  implicit val addressFormat = jsonFormat2(Address.apply)
  implicit val merkleBlockFormat = jsonFormat4(MerkleBlock)
  implicit val headersFormat = jsonFormat3(Headers)
  implicit val blocksFormat = jsonFormat2(Blocks)
  implicit val blockFormat = jsonFormat2(Block.apply)
  implicit val filterLoadFormat = jsonFormat4(FilterLoad)
  implicit val filterAddFormat = jsonFormat2(FilterAdd)
  implicit val messageHeaderFormat = jsonFormat4(MessageHeader)
  implicit val txValidationErrorFormat = jsonFormat2(ValidationError[Transaction])
  implicit val blkHeaderValidationErrorFormat = jsonFormat2(ValidationError[BlockHeader])
  implicit val blkBodyValidationErrorFormat = jsonFormat2(ValidationError[BlockBody])
  implicit val memPoolFormat = jsonFormat2(MemPoolMsg)

}