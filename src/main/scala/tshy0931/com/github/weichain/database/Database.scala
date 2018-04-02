package tshy0931.com.github.weichain.database

import com.redis._
import monix.eval.Task
import tshy0931.com.github.weichain.model.Block.{BlockBody, BlockHeader}
import tshy0931.com.github.weichain.model.Transaction
import tshy0931.com.github.weichain.module.ConfigurationModule._
import tshy0931.com.github.weichain.protobuf.Protobufable


trait Database[A] {

  def save(item: A)(implicit pb: Protobufable[A]): Task[Boolean]

  def find(key: String)(implicit pb: Protobufable[A]): Task[Option[A]]

  def delete(keys: String*)(implicit pb: Protobufable[A]): Task[Unit]
}

object Database {

  import com.redis.serialization.Parse.Implicits._
  import Identity._

  implicit lazy val clients: RedisClientPool = new RedisClientPool(redisHost, redisPort)

  private def exec[A, B](f: RedisClient => A)(implicit pb: Protobufable[B]) = {
    clients withClient f
  }

  implicit lazy val transactionDB: Database[Transaction] = new Database[Transaction] {

    override def save(item: Transaction)(implicit pb: Protobufable[Transaction]): Task[Boolean] =
      Task.eval( exec(_.set(item.key, pb toProtobuf item)) )

    override def find(key: String)(implicit pb: Protobufable[Transaction]): Task[Option[Transaction]] =
      Task.eval( exec(_.get[Array[Byte]](key)) ) map { _ map pb.fromProtobuf }

    override def delete(keys: String*)(implicit pb: Protobufable[Transaction]): Task[Unit] =
      Task.eval( exec(_.del(keys.head, keys.tail)) ) map ( _ => () )
  }

  implicit lazy val blockHeaderDB: Database[BlockHeader] = new Database[BlockHeader] {

    override def save(item: BlockHeader)(implicit pb: Protobufable[BlockHeader]): Task[Boolean] =
      Task.eval( exec(_.set(item.key, pb toProtobuf item)) )

    override def find(key: String)(implicit pb: Protobufable[BlockHeader]): Task[Option[BlockHeader]] =
      Task.eval( exec(_.get[Array[Byte]](key)) ) map { _ map pb.fromProtobuf }

    override def delete(keys: String*)(implicit pb: Protobufable[BlockHeader]): Task[Unit] =
      Task.eval( exec(_.del(keys.head, keys.tail)) ) map ( _ => () )
  }

  implicit lazy val blockDB: Database[BlockBody] = new Database[BlockBody] {

    override def save(item: BlockBody)(implicit pb: Protobufable[BlockBody]): Task[Boolean] =
      Task.eval( exec(_.set(item.key, pb toProtobuf item)) )

    override def find(key: String)(implicit pb: Protobufable[BlockBody]): Task[Option[BlockBody]] =
      Task.eval( exec(_.get[Array[Byte]](key)) ) map { _ map pb.fromProtobuf }

    override def delete(keys: String*)(implicit pb: Protobufable[BlockBody]): Task[Unit] =
      Task.eval( exec(_.del(keys.head, keys.tail)) ) map ( _ => () )
  }
}
