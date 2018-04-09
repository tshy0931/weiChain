package tshy0931.com.github.weichain.database

import tshy0931.com.github.weichain._
import com.redis.RedisClient.{ASC, DESC}
import shapeless.the
import tshy0931.com.github.weichain.model.{Address, Transaction}
import Scoreable._
import monix.eval.Task
import tshy0931.com.github.weichain.protobuf.Protobufable

trait MemPool[A] {

  def name: String
  def getAll: Task[Seq[A]]
  def getLatest(count: Int = 1): Task[Seq[A]]
  def getEarliest(count: Int = 1): Task[Seq[A]]
  def put(a: A, score: Long): Task[Unit]
  def delete(a: A): Task[Unit]
  def deleteItemsBefore(score: Long): Task[Unit]
  def deleteItemsAfter(score: Long): Task[Unit]
}

object MemPool extends Redis {

  import Protobufable._
  import com.redis.serialization.Parse.Implicits._

  def apply[A: MemPool]: MemPool[A] = the[MemPool[A]]

  private def decode[A: Protobufable](str: Array[Byte]): A = Protobufable[A].fromProtobuf(str)
  private def encode[A: Protobufable](a: A): Array[Byte]   = Protobufable[A].toProtobuf(a)

  implicit val txMemPool = new MemPool[Transaction] {

    override def name: String = "txmempool"

    override def getAll: Task[Seq[Transaction]] =
      exec { _.zrange[Array[Byte]](name) map { _ map decode[Transaction] } getOrElse Seq.empty }

    override def getLatest(count: Int): Task[Seq[Transaction]] =
      exec { _.zrange[Array[Byte]](name, 0, count-1, DESC) map { _ map decode[Transaction] } getOrElse Seq.empty }

    override def getEarliest(count: Int): Task[Seq[Transaction]] =
      exec { _.zrange[Array[Byte]](name, 0, count-1, ASC) map { _ map decode[Transaction] } getOrElse Seq.empty }

    override def put(tx: Transaction, score: Long): Task[Unit] =
      exec { _.zadd(name, tx.score, encode(tx)) }

    override def delete(tx: Transaction): Task[Unit] =
      exec { _.zrem(name, encode(tx)) }

    override def deleteItemsBefore(score: Long): Task[Unit] =
      exec { _.zremrangebyscore(name, end = score) }

    override def deleteItemsAfter(score: Long): Task[Unit] =
      exec { _.zremrangebyscore(name, start = score) }
  }

  implicit val peerMemPool = new MemPool[Address] {

    override def name: String = "peermempool"

    override def getAll: Task[Seq[Address]] =
      exec { _.zrange[Array[Byte]](name) map { _ map decode[Address] } getOrElse Seq.empty }

    override def getLatest(count: Int): Task[Seq[Address]] =
      exec { _.zrange[Array[Byte]](name, 0, count-1, DESC) map { _ map decode[Address] } getOrElse Seq.empty }

    override def getEarliest(count: Int): Task[Seq[Address]] =
      exec { _.zrange[Array[Byte]](name, 0, count-1, ASC) map { _ map decode[Address] } getOrElse Seq.empty }

    override def put(address: Address, score: Long): Task[Unit] =
      exec { _.zadd(name, System.currentTimeMillis, encode(address)) }

    override def delete(address: Address): Task[Unit] =
      exec { _.zrem(name, encode(address)) }

    override def deleteItemsBefore(timeMillis: Long): Task[Unit] =
      exec { _.zremrangebyscore(name, end = timeMillis) }

    override def deleteItemsAfter(timeMillis: Long): Task[Unit] =
      exec { _.zremrangebyscore(name, start = timeMillis) }
  }
}