package tshy0931.com.github.weichain.module

import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatest._
import tshy0931.com.github.weichain._
import tshy0931.com.github.weichain.model.Block.BlockHeader
import BlockChainModule._
import monix.execution.Scheduler.Implicits.global

import scala.util.{Failure, Success}

class BlockChainModuleSpec extends FlatSpec with GivenWhenThen with Matchers with BeforeAndAfterAll with Inside with BlockChainModuleFixture {

  override def beforeAll(): Unit = {
    bestLocalHeaderChain.value.set(generateBlockHeadersFromNumbers(0, 100))
  }

  override def afterAll(): Unit = {
    bestLocalHeaderChain.value.set(Vector.empty)
  }

  behavior of "Blockchain response of getheaders request"

  it should "return header chain starting from genesis block header with given count if no matching header is found" in {

    val headersInQuery = generateBlockHeadersFromNumbers(101, 109)
    searchHeadersAfter(headersInQuery, 10) runOnComplete {
      case Success((forkIndex, followingChain)) =>
        forkIndex shouldBe 0
        followingChain shouldBe bestLocalHeaderChain.value.get.take(10)
      case Failure(error) => fail(error)
    }
  }

  it should "return header chain starting from genesis block header with given count if only genesis block header is given" in {

    val headersInQuery = Vector(genesisBlock.value.header)
    searchHeadersAfter(headersInQuery, 20) runOnComplete {
      case Success((forkIndex, followingChain)) =>
        forkIndex shouldBe 0
        followingChain shouldBe bestLocalHeaderChain.value.get.take(20)
      case Failure(error) => fail(error)
    }
  }

  it should "return location of fork and header chain starting from the forked header with given count" in {

    val headersInQuery = generateBlockHeadersFromNumbers(10, 15) ++ generateBlockHeadersFromNumbers(20, 25)
    searchHeadersAfter(headersInQuery, 10) runOnComplete {
      case Success((forkIndex, followingChain)) =>
        forkIndex shouldBe 6
        followingChain shouldBe bestLocalHeaderChain.value.get.slice(16, 26)
      case Failure(error) => fail(error)
    }
  }

  it should "return header chain after given headers when there's no fork" in {

    val headersInQuery = generateBlockHeadersFromNumbers(10, 19)
    searchHeadersAfter(headersInQuery, 10) runOnComplete {
      case Success((forkIndex, followingChain)) =>
        forkIndex shouldBe 10
        followingChain shouldBe bestLocalHeaderChain.value.get.slice(20, 30)
      case Failure(error) => fail(error)
    }
  }
}

trait BlockChainModuleFixture extends TableDrivenPropertyChecks {

  private def generateHashesFromNumbers(start: Int, end: Int): Vector[Hash] =
    (start to end) map { _.toString.getBytes("UTF-8") } toVector

  def generateBlockHeadersFromNumbers(start: Int, end: Int): Vector[BlockHeader] =
    generateHashesFromNumbers(start, end) map { hash => BlockHeader(hash, 1, hash, hash, 1L, 1L, 1L) }
}