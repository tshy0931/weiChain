package tshy0931.com.github.weichain.module

import tshy0931.com.github.weichain._
import org.scalatest.{FlatSpec, GivenWhenThen, Inside, Matchers}
import MiningModule._
import BlockChainModule._
import DigestModule._
import monix.execution.Scheduler.Implicits.global

import scala.util.{Failure, Success}

class MiningModuleSpec extends FlatSpec with GivenWhenThen with Matchers with Inside {

  behavior of "Mining"

  it should "find valid nonce when finished mining a block" in {
    mine(genesisBlock.value) runOnComplete {
      case Success(block) =>
        block.header.hash.asString startsWith difficulty shouldBe true
        digest(block.header.computeHash :+ block.header.nonce.toByte).asString shouldBe block.header.hash.asString
      case Failure(error) => fail(error)
    }
  }
}