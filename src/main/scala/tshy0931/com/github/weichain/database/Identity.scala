package tshy0931.com.github.weichain.database

import org.slf4j.LoggerFactory
import shapeless.the
import tshy0931.com.github.weichain.model.Block.{BlockBody, BlockHeader}
import tshy0931.com.github.weichain.model.{Address, Transaction}

trait Identity[A] {
  def keyspace: String
  def identity: A => String
}

object Identity {

  private def instance[A](keyspace: String, idFunc: A => String): Identity[A] = new Identity[A] {

    override def keyspace: String = keyspace

    override def identity: A => String = idFunc
  }

  object Keyspace {
    val SORTEDSET_BLOCK_HEADER = "bhsset"
    val SORTEDSET_BLOCK_BODY   = "bbsset"
    val BLOCK_HEADER           = "blkhdr"
    val BLOCK_BODY             = "blkbdy"
    val TX                     = "tx"
    val ADDRESS_HASH           = "addrhm"
  }

  import Keyspace._

  def apply[A: Identity] = the[Identity[A]]

//  def keyOf[A: Identity]: String => String = key => {
//    val id: Identity[A] = Identity[A]
//    val a = s"${id.keyspace}:$key"
//    LoggerFactory.getLogger(this.getClass).info(a)
//    a
//  }

  implicit val blockHeaderId: Identity[BlockHeader] = instance(BLOCK_HEADER, blk => s"$BLOCK_HEADER:${blk.hash}")
  implicit val blockBodyId: Identity[BlockBody] = instance(BLOCK_BODY, blk => s"$BLOCK_BODY:${blk.headerHash}")
  implicit val txId: Identity[Transaction] = instance(TX, tx => s"$TX:${tx.hash}")
  implicit val addressId: Identity[Address] = instance(ADDRESS_HASH, addr => s"$ADDRESS_HASH:$addr")

  implicit class PrimaryKeyOps[A](item: A) {

    def key(implicit pk: Identity[A]) = pk.identity(item)
  }

  implicit class StringKeyOps(hash: String) {

    def toKeyOf[A](implicit pk: Identity[A]): String = s"${pk.keyspace}:$hash"
  }
}
