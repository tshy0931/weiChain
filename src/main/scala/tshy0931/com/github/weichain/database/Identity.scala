package tshy0931.com.github.weichain.database

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

  implicit val blockHeaderId: Identity[BlockHeader] = instance(blk => s"blk:${blk.hash.asString}")
  implicit val blockBodyId: Identity[BlockBody] = instance(blk => s"blk:${blk.headerHash.asString}")
  implicit val txId: Identity[Transaction] = instance(tx => s"tx:${tx.hash.asString}")
  implicit val addressId: Identity[Address] = instance(addr => s"addr:${addr.asString}")

  implicit class PrimaryKeyOps[A](item: A) {

    def key(implicit pk: Identity[A]) = pk.identity(item)
  }
}
