package tshy0931.com.github.weichain.model

import monix.eval.Task
import org.scalatest._
import org.scalatest.prop.TableDrivenPropertyChecks
import tshy0931.com.github.weichain._
import tshy0931.com.github.weichain.message.MerkleBlock
import tshy0931.com.github.weichain.model.Block.BlockHeader
import tshy0931.com.github.weichain.module.DigestModule._
import monix.execution.Scheduler.Implicits.global
import tshy0931.com.github.weichain.testdata.{CommonData, TransactionTestData}

import scala.util.{Failure, Success}

class MerkleTreeSpec extends FlatSpec with GivenWhenThen with Matchers with Inside with MerkleTreeFixture {

  behavior of "Merkle Tree"

  it should "build correct Merkle Tree from a collection of string documents" in {

    val hashedTx: Vector[Hash] = testTransactions.map(tx => digest(tx.hash))

    testMerkleTree.hashes.size shouldBe 14

    hashedTx(0).asString shouldBe testMerkleTree.hashAt(7).asString
    hashedTx(1).asString shouldBe testMerkleTree.hashAt(8).asString
    hashedTx(2).asString shouldBe testMerkleTree.hashAt(9).asString
    hashedTx(3).asString shouldBe testMerkleTree.hashAt(10).asString
    hashedTx(4).asString shouldBe testMerkleTree.hashAt(11).asString
    hashedTx(5).asString shouldBe testMerkleTree.hashAt(12).asString
    hashedTx(6).asString shouldBe testMerkleTree.hashAt(13).asString

    testMerkleTree.hashAt(3).asString shouldBe merge(testMerkleTree.hashAt(7), testMerkleTree.hashAt(8)).asString
    testMerkleTree.hashAt(4).asString shouldBe merge(testMerkleTree.hashAt(9), testMerkleTree.hashAt(10)).asString
    testMerkleTree.hashAt(5).asString shouldBe merge(testMerkleTree.hashAt(11), testMerkleTree.hashAt(12)).asString
    testMerkleTree.hashAt(6).asString shouldBe merge(testMerkleTree.hashAt(13), testMerkleTree.hashAt(13)).asString

    testMerkleTree.hashAt(0).asString shouldBe merge(testMerkleTree.hashAt(1), testMerkleTree.hashAt(2)).asString
    testMerkleTree.hashAt(1).asString shouldBe merge(testMerkleTree.hashAt(3), testMerkleTree.hashAt(4)).asString
  }

  it should "always generate the same hash for the same document" in {

    val tree1 = MerkleTree.build(testTransactions)
    val tree2 = MerkleTree.build(testTransactions)

    tree1.hashes.map(_.asString) shouldBe tree2.hashes.map(_.asString)
  }

  it should "corretly derive path from merkle root to a given transaction if it exists in this merkle tree" in {

    forAll(testPaths) { (index, expectedPath) =>
      val target = testTransactions(index).hash
      val task: Task[Option[List[Int]]] = testMerkleTree.derivePath(digest(target).asString).value
      task runOnComplete {
        case Success(path) => path should contain(expectedPath)
        case Failure(err)  => fail(err)
      }
    }
  }

  it should "return None as path when the transaction doesn't exist in the merkle tree" in {

    testMerkleTree.derivePath(digest("no such tx").asString).value.runOnComplete {
      case Success(path) => path shouldBe None
      case Failure(err)  => fail(err)
    }
  }

  it should "correctly derive MerkleBlock flags and hashes for a given transaction" in {

    forAll(testFlagsAndHashes) { (txId, flags, hashIndices) =>

      val result: Task[Option[MerkleBlock]] =
        testMerkleTree.deriveMerkleBlockFor(digest(testTransactions(txId).hash).asString)(testBlockHeader, 0L).value

      val hashes: Vector[String] = hashIndices map testMerkleTree.hashAt map (_.asString)

      result runOnComplete {
        case Success(merkleBlock) => inside(merkleBlock) {
          case Some(MerkleBlock(_, _, hs, fs)) =>
            fs shouldBe flags
            (hs map {_.asString}) shouldBe hashes
          }
        case Failure(err)  => fail(err)
      }
    }
  }

  it should "correctly verify that a transaction is in a block" in {

    forAll(testFlagsAndHashes) { (txIndex, flags, hashIndices) =>

      val result: Task[Option[(Hash, Int)]] =
        testMerkleTree.parseMerkleBlock(MerkleBlock(testBlockHeader, 0L, hashIndices map testMerkleTree.hashAt, flags)).value

      result runOnComplete {
        case Success(result) => inside(result) { case Some((rootHash, location)) =>
          rootHash shouldBe testMerkleTree.hashAt(0)
          //TODO - convert index in merkle tree back to index in transaction list
          location shouldBe txIndex
        }
        case Failure(err)  => fail(err)
      }
    }
  }
}

trait MerkleTreeFixture extends TableDrivenPropertyChecks with TransactionTestData with CommonData {

  import tshy0931.com.github.weichain.module.DigestModule._

  val testTransactions: Vector[Transaction] = Vector(
    txValid(digest("1")),
    txValid(digest("2")),
    txValid(digest("3")),
    txValid(digest("4")),
    txValid(digest("5")),
    txValid(digest("6")),
    txValid(digest("7"))
  )

  val testMerkleTree: MerkleTree = MerkleTree.build(testTransactions)

  val testPaths = Table(
    ("target tx index", "expected path"),
    (0, List(0,1,3,7)),
    (1, List(0,1,3,8)),
    (2, List(0,1,4,9)),
    (3, List(0,1,4,10)),
    (4, List(0,2,5,11)),
    (5, List(0,2,5,12)),
    (6, List(0,2,6,13))
  )

  val testFlagsAndHashes = Table(
    ("target tx index", "flags", "indices of hashes"),
    (0, Vector(1,1,1,1,0,0,0), Vector(7,8,4,2)),
    (1, Vector(1,1,1,0,1,0,0), Vector(7,8,4,2)),
    (2, Vector(1,1,0,1,1,0,0), Vector(3,9,10,2)),
    (3, Vector(1,1,0,1,0,1,0), Vector(3,9,10,2)),
    (4, Vector(1,0,1,1,1,0,0), Vector(1,11,12,6)),
    (5, Vector(1,0,1,1,0,1,0), Vector(1,11,12,6)),
    (6, Vector(1,0,1,0,1,1),   Vector(1,5,13))
  )

  val testBlockHeader = BlockHeader(Array.emptyByteArray, 0, Array.emptyByteArray, Array.emptyByteArray, 0L, 0L, 0L)
}
