package tshy0931.com.github.weichain.module

import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatest._
import tshy0931.com.github.weichain._
import tshy0931.com.github.weichain.model.Block.BlockHeader
import BlockChainModule._
import monix.eval.Task
import monix.execution.Scheduler.Implicits.global
import org.scalatest.mockito.MockitoSugar
import tshy0931.com.github.weichain.model.Chain
import scala.util.{Failure, Success}
import tshy0931.com.github.weichain.protobuf.Protobufable

class BlockChainModuleSpec extends FlatSpec
  with GivenWhenThen
  with Matchers
  with Inside
  with BlockChainModuleFixture {

  behavior of "Blockchain response of getheaders request"

  it should "return header chain starting from genesis block header with given count if no matching header is found" in {

    val headersInQuery = generateBlockHeadersFromNumbers(101, 109)
    searchHeadersAfter(headersInQuery, 10) runOnComplete {
      case Success((forkIndex, followingChain)) =>
        forkIndex shouldBe 0
        followingChain shouldBe headerChain.first(10)
      case Failure(error) => fail(error)
    }
  }

  it should "return header chain starting from genesis block header with given count if only genesis block header is given" in {

    val headersInQuery = Vector(genesisBlock.header)
    searchHeadersAfter(headersInQuery, 20) runOnComplete {
      case Success((forkIndex, followingChain)) =>
        forkIndex shouldBe 0
        followingChain shouldBe headerChain.first(20)
      case Failure(error) => fail(error)
    }
  }

  it should "return location of fork and header chain starting from the forked header with given count" in {

    val headersInQuery = generateBlockHeadersFromNumbers(10, 15) ++ generateBlockHeadersFromNumbers(20, 25)
    searchHeadersAfter(headersInQuery, 10) runOnComplete {
      case Success((forkIndex, followingChain)) =>
        forkIndex shouldBe 6
        followingChain shouldBe headerChain.slice(16, 26)
      case Failure(error) => fail(error)
    }
  }

  it should "return header chain after given headers when there's no fork" in {

    val headersInQuery = generateBlockHeadersFromNumbers(10, 19)
    searchHeadersAfter(headersInQuery, 10) runOnComplete {
      case Success((forkIndex, followingChain)) =>
        forkIndex shouldBe 10
        followingChain shouldBe headerChain.slice(20, 30)
      case Failure(error) => fail(error)
    }
  }
}

trait BlockChainModuleFixture extends TableDrivenPropertyChecks with MockitoSugar {

  private[module] lazy val headerChain: Chain[BlockHeader] = new Chain[BlockHeader] {

    var chain: Vector[BlockHeader] = generateBlockHeadersFromNumbers(0, 100)

    override def name: String = "testHeaderChain"

    override def update(items: Seq[BlockHeader], from: Int)(implicit pb: Protobufable[BlockHeader]): Task[Unit] =
      Task.now {
        val vec = items.toVector
        val head = vec.take(from - 1)
        val tail = vec.takeRight(vec.size - from - items.size)
        chain = head ++ items ++ tail
      }

    override def at(index: Int)(implicit pb: Protobufable[BlockHeader]): Task[Option[BlockHeader]] =
      Task.now(Some(chain(index)))

    override def last(count: Int)(implicit pb: Protobufable[BlockHeader]): Task[Seq[BlockHeader]] =
      Task.now(chain takeRight count)

    override def first(count: Int)(implicit pb: Protobufable[BlockHeader]): Task[Seq[BlockHeader]] =
      Task.now(chain take count)

    override def slice(start: Int, end: Int)(implicit pb: Protobufable[BlockHeader]): Task[Seq[BlockHeader]] =
      Task.now(chain.slice(start, end))

    override def size: Task[Long] =
      Task.now(chain.size)
  }

  private def generateHashesFromNumbers(start: Int, end: Int): Vector[Hash] =
    (start to end) map { _.toString } toVector

  def generateBlockHeadersFromNumbers(start: Int, end: Int): Vector[BlockHeader] =
    generateHashesFromNumbers(start, end) map { hash => BlockHeader(hash, 1, hash, hash, 1L, 1, 1L) }
}