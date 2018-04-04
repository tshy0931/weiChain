package tshy0931.com.github.weichain.database

import shapeless.the
import tshy0931.com.github.weichain._
import tshy0931.com.github.weichain.model.Block.{BlockBody, BlockHeader}
import tshy0931.com.github.weichain.model.Transaction
import tshy0931.com.github.weichain.network.Address

trait Identity[A] {

  def identity: A => String
}

object Identity {

  private def instance[A](idFunc: A => String): Identity[A] = new Identity[A] {
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

  implicit val blockHeaderId: Identity[BlockHeader] = instance(blk => s"$BLOCK_HEADER:${blk.hash.asString}")
  implicit val blockBodyId: Identity[BlockBody] = instance(blk => s"$BLOCK_BODY:${blk.headerHash.asString}")
  implicit val txId: Identity[Transaction] = instance(tx => s"$TX:${tx.hash.asString}")
  implicit val addressId: Identity[Address] = instance(addr => s"$ADDRESS_HASH:${addr.asString}")

  implicit class PrimaryKeyOps[A](item: A) {

    def key(implicit pk: Identity[A]) = pk.identity(item)
  }
}
