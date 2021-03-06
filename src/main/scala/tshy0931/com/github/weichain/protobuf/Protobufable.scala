package tshy0931.com.github.weichain.protobuf

import java.io.ByteArrayInputStream

import cats.syntax.option._
import com.google.protobuf.ByteString
import tshy0931.com.github.weichain.model.Block.{BlockBody, BlockHeader}
import tshy0931.com.github.weichain.model.{Address, MerkleTree, Transaction}
import tshy0931.com.github.weichain.model.Transaction.{Coinbase, Input, Output}
import tshy0931.com.github.weichain.model.proto.model._
import monocle.Iso
import shapeless.the

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

  def apply[A: Protobufable]: Protobufable[A] = the[Protobufable[A]]

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
    txFee = proto.txFee,
    createTime = proto.createTime
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
    txFee = tx.txFee,
    createTime = tx.createTime
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
      proto.source map outputIso.get get,
      proto.scriptSig,
      proto.sequence
  )}
  { input => InputProto(
      input.source.some map outputIso.reverseGet,
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
      height = proto.height,
      nonce = proto.nonce
  )}
  { header => BlockHeaderProto(
      hash = header.hash,
      version = header.version,
      prevHeaderHash = header.prevHeaderHash,
      merkleRoot = header.merkleRoot,
      time = header.time,
      height = header.height,
      nonce = header.nonce
  )}

  val blockBodyIso = Iso[BlockBodyProto, BlockBody]
  { proto => BlockBody(
      headerHash = proto.headerHash,
      merkleTree = (proto.merkleTree map merkleTreeIso.get) getOrElse MerkleTree.empty.value,
      nTx = proto.nTx,
      size = proto.size,
      transactions = proto.transactions map txIso.get toVector
  )}
  { body => BlockBodyProto(
      headerHash = body.headerHash,
      merkleTree = body.merkleTree.some map merkleTreeIso.reverseGet,
      nTx = body.nTx,
      size = body.size,
      transactions = body.transactions map txIso.reverseGet
  )}

  val merkleTreeIso = Iso[MerkleTreeProto, MerkleTree]
  { proto => MerkleTree(
      hashes = proto.hashes toVector,
      nTx = proto.nTx
  )}
  { tree => MerkleTreeProto(
      hashes = tree.hashes,
      nTx = tree.nTx
  )}

  val addressIso = Iso[AddressProto, Address]
  { proto => Address(
      host = proto.host,
      port = proto.port
  )}
  { addr => AddressProto(
      host = addr.host,
      port = addr.port
  )}

  implicit val txProtobufable: Protobufable[Transaction] = instance[Transaction]
    { txIso reverseGet _ toByteString }
    { ba => txIso get TransactionProto.parseFrom(new ByteArrayInputStream(ba)) }

  implicit val blkHeaderProtobufable: Protobufable[BlockHeader] = instance[BlockHeader]
    { blockHeaderIso reverseGet _ toByteString }
    { blockHeaderIso get BlockHeaderProto.parseFrom(_) }

  implicit val blkBodyProtobufable: Protobufable[BlockBody] = instance[BlockBody]
    { blockBodyIso reverseGet _ toByteString }
    { blockBodyIso get BlockBodyProto.parseFrom(_) }

  implicit val addressProtobufable: Protobufable[Address] = instance[Address]
    { addressIso reverseGet _ toByteString }
    { addressIso get AddressProto.parseFrom(_) }
}