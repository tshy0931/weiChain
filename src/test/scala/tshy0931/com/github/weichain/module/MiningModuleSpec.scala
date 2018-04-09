package tshy0931.com.github.weichain.module

import tshy0931.com.github.weichain._
import org.scalatest.{FlatSpec, GivenWhenThen, Inside, Matchers}
import MiningModule._
import DigestModule._
import BlockChainModule._
import tshy0931.com.github.weichain.model.Block._
import monix.execution.Scheduler.Implicits.global
import tshy0931.com.github.weichain.testdata.TestApplicationData

import scala.util.{Failure, Success}

class MiningModuleSpec extends FlatSpec with GivenWhenThen with Matchers with Inside with MiningModuleFixture {

  behavior of "Mining"

  it should "find valid nonce when finished mining a block" in {
    mineWithTransactions(Vector(tx1), genesisBlock.value) runOnComplete {
      case Success(block) =>
        block.header.hash.asString startsWith difficulty shouldBe true
        applyNonce(computeHash(genesisHash, merkleTree1.root), block.header.nonce).asString shouldBe block.header.hash.asString
      case Failure(error) => fail(error)
    }
  }
}

trait MiningModuleFixture extends TestApplicationData