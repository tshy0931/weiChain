package tshy0931.com.github.weichain.module

import org.scalatest.{FlatSpec, Inside, Matchers}
import MiningModule._
import BlockChainModule._
import tshy0931.com.github.weichain.model.Block._
import monix.execution.Scheduler.Implicits.global
import tshy0931.com.github.weichain.testdata.TestApplicationData

import scala.util.{Failure, Success}
import scala.concurrent.duration._

class MiningModuleSpec extends FlatSpec with Matchers with Inside with MiningModuleFixture {

  behavior of "Mining"

  it should "find valid nonce when finished mining a block" in {
    val block = mineWithTransactions(Vector(validTx1), genesisBlockHeader) runSyncUnsafe(60 seconds)

    block.header.hash startsWith difficulty shouldBe true
    applyNonce(
      computeHash(genesisHash, merkleTree1.root),
      block.header.nonce
    ) shouldBe block.header.hash
  }
}

trait MiningModuleFixture extends TestApplicationData