package tshy0931.com.github.weichain.protobuf

import cats.syntax.option._
import com.google.protobuf.ByteString
import tshy0931.com.github.weichain.model.Block.BlockHeader
import tshy0931.com.github.weichain.model.Transaction
import tshy0931.com.github.weichain.model.Transaction.{Coinbase, Input, Output}
import tshy0931.com.github.weichain.model.proto.model._
import monocle._

trait Protobufable[A] {

  def toProtobuf: A => Array[Byte]

  def fromProtobuf: Array[Byte] => A
}

object Protobufable {

  private implicit def byteStringToByteArray(str: ByteString): Array[Byte] = str.toByteArray
  private implicit def byteArrayToByteString(arr: Array[Byte]): ByteString = ByteString.copyFrom(arr)

  private def instance[A](to: A => Array[Byte])(from: Array[Byte] => A) = new Protobufable[A] {
    override def toProtobuf: A => Array[Byte] = to
    override def fromProtobuf: Array[Byte] => A = from
  }

  val txIso = Iso[TransactionProto, Transaction]
  { proto => Transaction(
      hash = proto.hash,
      version = proto.version,
      nTxIn = proto.nTxIn,
      txIn = proto.txIn map inputIso.get toVector,
      nTxOut = proto.nTxOut,
      txOut = proto.txOut map outputIso.get toVector,
      lockTime = proto.lockTime,
      blockIndex = proto.blockIndex,
      txFee = proto.txFee
  )}
  { tx => TransactionProto(
      hash = tx.hash,
      version = tx.version,
      nTxIn = tx.nTxIn,
      txIn = tx.txIn map inputIso.reverseGet,
      nTxOut = tx.nTxOut,
      txOut = tx.txOut map outputIso.reverseGet,
      lockTime = tx.lockTime,
      blockIndex = tx.blockIndex,
      txFee = tx.txFee
  )}

  val coinbaseIso = Iso[CoinbaseProto, Coinbase]
  { proto => Coinbase(proto.script) }
  { cb => CoinbaseProto(cb.script) }

  val outputIso = Iso[OutputProto, Output]
  { proto => Output(
      proto.value,
      proto.address,
      proto.blockHash,
      proto.txIndex,
      proto.outputIndex,
      proto.scriptPubKey,
      proto.coinbase map coinbaseIso.get
  )}
  { output => OutputProto(
      output.value,
      output.address,
      output.blockHash,
      output.txIndex,
      output.outputIndex,
      output.scriptPubKey,
      output.coinbase map coinbaseIso.reverseGet
  )}

  val inputIso = Iso[InputProto, Input]
  { proto => Input(
      proto.value,
      proto.source map outputIso.get get,
      proto.address,
      proto.scriptSig,
      proto.sequence
  )}
  { input => InputProto(
      input.value,
      input.source.some map outputIso.reverseGet,
      ByteString.copyFrom(input.address),
      input.scriptSig,
      input.sequence
  )}

  val blockHeaderIso = Iso[BlockHeaderProto, BlockHeader]
  { proto => BlockHeader(
      hash = proto.hash,
      version = proto.version,
      prevHeaderHash = proto.prevHeaderHash,
      merkleRoot = proto.merkleRoot,
      time = proto.time,
      nBits = proto.nBits,
      nonce = proto.nonce
  )}
  { header => BlockHeaderProto(
      hash = header.hash,
      version = header.version,
      prevHeaderHash = header.prevHeaderHash,
      merkleRoot = header.merkleRoot,
      time = header.time,
      nBits = header.nBits,
      nonce = header.nonce
  )}

  implicit val txProtobufable: Protobufable[Transaction] = instance[Transaction]
    { txIso reverseGet _ toByteArray }
    { txIso get TransactionProto.parseFrom(_) }

  implicit val blkHeaderProtobufable: Protobufable[BlockHeader] = instance[BlockHeader]
    { blockHeaderIso reverseGet _ toByteArray }
    { blockHeaderIso get BlockHeaderProto.parseFrom(_) }

}