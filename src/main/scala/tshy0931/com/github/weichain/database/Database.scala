package tshy0931.com.github.weichain.database

import monix.eval.Task
import shapeless.the
import tshy0931.com.github.weichain.model.Block.{BlockBody, BlockHeader}
import tshy0931.com.github.weichain.model.Transaction
import tshy0931.com.github.weichain.protobuf.Protobufable
import Identity.Keyspace._

trait Database[A] {

  def save(item: A)(implicit pb: Protobufable[A]): Task[Boolean]

  def find(key: String)(implicit pb: Protobufable[A]): Task[Option[A]]

  def deleteKeys(keys: String*)(implicit pb: Protobufable[A]): Task[Unit]

  def deleteItems(items: A*)(implicit pb: Protobufable[A]): Task[Unit]
}

object Database extends Redis {

  import com.redis.serialization.Parse.Implicits._
  import Identity._

  def apply[A: Database]: Database[A] = the[Database[A]]

  implicit val transactionDB: Database[Transaction] = new Database[Transaction] {

    override def save(item: Transaction)(implicit pb: Protobufable[Transaction]): Task[Boolean] =
      exec(_.set(item.key, pb toProtobuf item))

    override def find(key: String)(implicit pb: Protobufable[Transaction]): Task[Option[Transaction]] =
      exec(_.get[Array[Byte]](s"$TX:$key")) map { _ map pb.fromProtobuf }

    override def deleteKeys(keys: String*)(implicit pb: Protobufable[Transaction]): Task[Unit] =
      exec(_.del(keys.head, keys.tail)) map ( _ => () )

    override def deleteItems(items: Transaction*)(implicit pb: Protobufable[Transaction]): Task[Unit] =
      exec(_.del(items.head.key, items.tail map {_.key}))
  }

  implicit val blockHeaderDB: Database[BlockHeader] = new Database[BlockHeader] {

    override def save(item: BlockHeader)(implicit pb: Protobufable[BlockHeader]): Task[Boolean] =
      exec(_.set(item.key, pb toProtobuf item))

    override def find(key: String)(implicit pb: Protobufable[BlockHeader]): Task[Option[BlockHeader]] =
      exec(_.get[Array[Byte]](s"$BLOCK_HEADER:$key")) map { _ map pb.fromProtobuf }

    override def deleteKeys(keys: String*)(implicit pb: Protobufable[BlockHeader]): Task[Unit] =
      exec(_.del(keys.head, keys.tail)) map ( _ => () )

    override def deleteItems(items: BlockHeader*)(implicit pb: Protobufable[BlockHeader]): Task[Unit] =
      exec(_.del(items.head.key, items.tail map {_.key}))
  }

  implicit val blockDB: Database[BlockBody] = new Database[BlockBody] {

    override def save(item: BlockBody)(implicit pb: Protobufable[BlockBody]): Task[Boolean] =
      exec(_.set(item.key, pb toProtobuf item))

    override def find(key: String)(implicit pb: Protobufable[BlockBody]): Task[Option[BlockBody]] =
      exec(_.get[Array[Byte]](s"$BLOCK_BODY:$key")) map { _ map pb.fromProtobuf }

    override def deleteKeys(keys: String*)(implicit pb: Protobufable[BlockBody]): Task[Unit] =
      exec(_.del(keys.head, keys.tail)) map ( _ => () )

    override def deleteItems(items: BlockBody*)(implicit pb: Protobufable[BlockBody]): Task[Unit] =
      exec(_.del(items.head.key, items.tail map {_.key}))
  }

}
