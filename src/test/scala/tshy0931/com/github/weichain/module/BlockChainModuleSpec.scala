package tshy0931.com.github.weichain.module

import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatest._
import tshy0931.com.github.weichain.{Hash, hashToString}
import tshy0931.com.github.weichain.model.Block.BlockHeader
import BlockChainModule._

class BlockChainModuleSpec extends FlatSpec with GivenWhenThen with Matchers with BeforeAndAfterAll with Inside with BlockChainModuleFixture {

  override def beforeAll(): Unit = {
    bestLocalHeaderChain = generateBlockHeadersFromNumbers(0, 100)
  }

  override def afterAll(): Unit = {
    bestLocalHeaderChain = Vector.empty
  }

  behavior of "Blockchain response of getheaders request"

  it should "return header chain starting from genesis block header with given count if no matching header is found" in {

    val headersInQuery = generateBlockHeadersFromNumbers(101, 109)
    val (forkIndex, followingChain) = searchHeadersAfter(headersInQuery, 10)
    forkIndex shouldBe 0
    followingChain shouldBe bestLocalHeaderChain.take(10)
  }

  it should "return header chain starting from genesis block header with given count if only genesis block header is given" in {

    val headersInQuery = Vector(genesisBlock.header)
    val (forkIndex, followingChain) = searchHeadersAfter(headersInQuery, 20)
    forkIndex shouldBe 0
    followingChain shouldBe bestLocalHeaderChain.take(20)
  }

  it should "return location of fork and header chain starting from the forked header with given count" in {

    val headersInQuery = generateBlockHeadersFromNumbers(10, 15) ++ generateBlockHeadersFromNumbers(20, 25)
    val (forkIndex, followingChain) = searchHeadersAfter(headersInQuery, 10)
    forkIndex shouldBe 6
    followingChain shouldBe bestLocalHeaderChain.slice(16, 26)
  }

  it should "return header chain after given headers when there's no fork" in {

    val headersInQuery = generateBlockHeadersFromNumbers(10, 19)
    val (forkIndex, followingChain) = searchHeadersAfter(headersInQuery, 10)
    forkIndex shouldBe 10
    followingChain shouldBe bestLocalHeaderChain.slice(20, 30)
  }
}

trait BlockChainModuleFixture extends TableDrivenPropertyChecks {

  private def generateHashesFromNumbers(start: Int, end: Int): Vector[Hash] =
    (start to end) map { _.toString.getBytes("UTF-8") } toVector

  def generateBlockHeadersFromNumbers(start: Int, end: Int): Vector[BlockHeader] =
    generateHashesFromNumbers(start, end) map { hash => BlockHeader(hash, 1, hash, hash, 1L, 1L, 1L) }

}