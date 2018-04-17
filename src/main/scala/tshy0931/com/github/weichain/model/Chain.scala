package tshy0931.com.github.weichain.model

import akka.event.Logging
import monix.eval.Task
import shapeless.the
import tshy0931.com.github.weichain.database.Redis
import tshy0931.com.github.weichain.model.Block.BlockHeader
import tshy0931.com.github.weichain.module.NetworkModule.system
import tshy0931.com.github.weichain.protobuf.Protobufable

trait Chain[A] {

  def name: String
  def append(item: A, prev: A)(implicit pb: Protobufable[BlockHeader]): Task[Unit]
  def update(items: Seq[BlockHeader], from: Int)(implicit pb: Protobufable[BlockHeader]): Task[Unit]
  def at(index: Int)(implicit pb: Protobufable[BlockHeader]): Task[Option[A]]
  def last(count: Int)(implicit pb: Protobufable[BlockHeader]): Task[Seq[A]]
  def first(count: Int)(implicit pb: Protobufable[BlockHeader]): Task[Seq[A]]
  def slice(start: Int, end: Int)(implicit pb: Protobufable[BlockHeader]): Task[Seq[A]]
  def size: Task[Long]
}

object Chain extends Redis {

  def apply[A: Chain]: Chain[A] = the[Chain[A]]

  lazy val log = Logging(system, this.getClass)

  import com.redis.serialization.Parse.Implicits._

  implicit val headerChain: Chain[BlockHeader] = new Chain[BlockHeader] {

    override def name: String = "hc"

    override def append(item: BlockHeader, prev: BlockHeader)(implicit pb: Protobufable[BlockHeader]): Task[Unit] =
      exec{ _.zadd(name, prev.height+1, pb toProtobuf item) }

    override def update(items: Seq[BlockHeader], from: Int)(implicit pb: Protobufable[BlockHeader]): Task[Unit] =
      Task.gather( items map { item => exec{ _.zadd(name, item.height, pb toProtobuf item)} }) map { _ => ()} onErrorRecover {
        case err =>
          log.error("Failed to update block headers from {} on header chain, headers: {}, error: {}", from, items, err)
      }

    override def at(index: Int)(implicit pb: Protobufable[BlockHeader]): Task[Option[BlockHeader]] =
      slice(index, index+1) map { _.headOption }

    override def last(count: Int)(implicit pb: Protobufable[BlockHeader]): Task[Seq[BlockHeader]] =
      exec{ _.zrange[Array[Byte]](name, -count, -1) map { _ map pb.fromProtobuf } getOrElse Seq.empty }

    override def first(count: Int)(implicit pb: Protobufable[BlockHeader]): Task[Seq[BlockHeader]] =
      exec{ _.zrange[Array[Byte]](name, 0, count-1) map { _ map pb.fromProtobuf } getOrElse Seq.empty }

    override def slice(start: Int, end: Int)(implicit pb: Protobufable[BlockHeader]): Task[Seq[BlockHeader]] =
      exec{ _.zrange[Array[Byte]](name, start, end-1) map { _ map pb.fromProtobuf } getOrElse Seq.empty }

    override def size: Task[Long] = exec{ _.zcard(name) getOrElse 0L }
  }
}