package tshy0931.com.github.weichain.database

import cats.data.OptionT
import com.redis._
import monix.eval.Task
import monix.execution.CancelableFuture
import tshy0931.com.github.weichain.model.Block.{BlockBody, BlockHeader}
import tshy0931.com.github.weichain.model.Transaction
import tshy0931.com.github.weichain.module.ConfigurationModule._
import tshy0931.com.github.weichain.protobuf.Protobufable


trait Database[A] {

  def save(item: A)(implicit pb: Protobufable[A]): CancelableFuture[Boolean]

  def find(key: String)(implicit pb: Protobufable[A]): OptionT[CancelableFuture, A]

  def delete(keys: String*)(implicit pb: Protobufable[A]): CancelableFuture[Unit]
}

object Database {

  import monix.execution.Scheduler.Implicits.global
  import com.redis.serialization.Parse.Implicits._
  import Identity._

  implicit lazy val clients: RedisClientPool = new RedisClientPool(redisHost, redisPort)

  private def exec[A, B](f: RedisClient => A)(implicit pb: Protobufable[B]) = {
    clients withClient f
  }

  implicit lazy val transactionDB: Database[Transaction] = new Database[Transaction] {

    override def save(item: Transaction)(implicit pb: Protobufable[Transaction]): CancelableFuture[Boolean] = {
      Task.eval(
        exec(_.set(item.key, pb toProtobuf item))
      ) runAsync
    }

    override def find(key: String)(implicit pb: Protobufable[Transaction]): OptionT[CancelableFuture, Transaction] =
      OptionT(
        Task.eval(
          exec(_.get[Array[Byte]](key))
        ) map { _ map pb.fromProtobuf } runAsync
      )

    override def delete(keys: String*)(implicit pb: Protobufable[Transaction]): CancelableFuture[Unit] = {
      Task.eval(exec(_.del(keys.head, keys.tail))).map(_ => ()) runAsync
    }
  }

  implicit lazy val blockHeaderDB: Database[BlockHeader] = new Database[BlockHeader] {

    override def save(item: BlockHeader)(implicit pb: Protobufable[BlockHeader]): CancelableFuture[Boolean] = {
      Task.eval(
        exec(_.set(item.key, pb toProtobuf item))
      ) runAsync
    }

    override def find(key: String)(implicit pb: Protobufable[BlockHeader]): OptionT[CancelableFuture, BlockHeader] = {
      OptionT(
        Task.eval(
          exec(_.get[Array[Byte]](key))
        ) map { _ map pb.fromProtobuf } runAsync
      )
    }

    override def delete(keys: String*)(implicit pb: Protobufable[BlockHeader]): CancelableFuture[Unit] = {
      Task.eval(exec(_.del(keys.head, keys.tail))) map (_ => ()) runAsync
    }
  }

  implicit lazy val blockDB: Database[BlockBody] = new Database[BlockBody] {

    override def save(item: BlockBody)(implicit pb: Protobufable[BlockBody]): CancelableFuture[Boolean] = {
      Task.eval(
        exec(_.set(item.key, pb toProtobuf item))
      ) runAsync
    }

    override def find(key: String)(implicit pb: Protobufable[BlockBody]): OptionT[CancelableFuture, BlockBody] = {
      OptionT(
        Task.eval(
          exec(_.get[Array[Byte]](key))
        ) map { _ map pb.fromProtobuf } runAsync
      )
    }

    override def delete(keys: String*)(implicit pb: Protobufable[BlockBody]): CancelableFuture[Unit] = {
      Task.eval(exec(_.del(keys.head, keys.tail))) map (_ => ()) runAsync
    }

  }
}
