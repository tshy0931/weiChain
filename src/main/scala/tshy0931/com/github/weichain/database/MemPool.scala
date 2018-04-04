package tshy0931.com.github.weichain.database

import tshy0931.com.github.weichain._
import com.redis.RedisClient.{ASC, DESC}
import shapeless.the
import tshy0931.com.github.weichain.model.Transaction
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

  def apply[A: MemPool]: MemPool[A] = the[MemPool[A]]

  implicit val txMemPool = new MemPool[Transaction] {

    private def decode[A: Protobufable](str: String): A = Protobufable[A].fromProtobuf(str.getBytes("UTF-8"))
    private def encode[A: Protobufable](a: A): String   = Protobufable[A].toProtobuf(a).asString

    override def name: String = "txmempool"

    override def getAll: Task[Seq[Transaction]] =
      exec { _.zrange(name) map { _ map decode[Transaction] } getOrElse Seq.empty[Transaction] }

    override def getLatest(count: Int): Task[Seq[Transaction]] =
      exec { _.zrange(name, 0, count-1, DESC) map { _ map decode[Transaction] } getOrElse Seq.empty[Transaction] }

    override def getEarliest(count: Int): Task[Seq[Transaction]] =
      exec { _.zrange(name, 0, count-1, ASC) map { _ map decode[Transaction] } getOrElse Seq.empty[Transaction] }

    override def put(tx: Transaction, score: Long): Task[Unit] =
      exec { _.zadd(name, tx.score, encode(tx)) }

    override def delete(tx: Transaction): Task[Unit] =
      exec { _.zrem(name, encode(tx)) }

    override def deleteItemsBefore(score: Long): Task[Unit] =
      exec { _.zremrangebyscore(name, end = score) }

    override def deleteItemsAfter(score: Long): Task[Unit] =
      exec { _.zremrangebyscore(name, start = score) }
  }
}