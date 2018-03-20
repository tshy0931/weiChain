package tshy0931.com.github.weichain.model

import tshy0931.com.github.weichain._
import org.scalatest._
import org.scalatest.prop.TableDrivenPropertyChecks

class MerkleTreeSpec extends FlatSpec with GivenWhenThen with Matchers with Inside with MerkleTreeFixture {

  behavior of "Merkle Tree"

  it should "build correct Merkle Tree from a collection of string documents" in {

    testMerkleTree.hashes.size shouldBe 14
  }

  it should "always generate the same hash for the same document" in {

    val tree1 = MerkleTree.build(testTransactions)
    val tree2 = MerkleTree.build(testTransactions)

    tree1.hashes.map(hashToString) shouldBe tree2.hashes.map(hashToString)
  }

  it should "corretly derive path from merkle root to a given transaction if it exists in this merkle tree" in {

    forAll(testPaths) { (index, expectedPath) =>
      val target: String = testTransactions(index)
      val path: Option[List[Int]] = testMerkleTree.derivePath(digestor.digest(target.getBytes("UTF-8")))
      path should contain(expectedPath)
    }
  }

  it should "return None as path when the transaction doesn't exist in the merkle tree" in {

    testMerkleTree.derivePath(digestor.digest("no such tx".getBytes("UTF-8"))) shouldBe None
  }

  it should "correctly derive MerkleBlock flags and hashes for a given transaction" in {

    forAll(testFlagsAndHashes) { (txId, flags, hashIndices) =>

      val result = testMerkleTree.deriveFlagsAndHashes(digestor.digest(testTransactions(txId).getBytes("UTF-8")))
      val hashes: Vector[String] = hashIndices map testMerkleTree.hashAt map hashToString
      inside(result) { case Some((fs, hs)) =>
        fs shouldBe flags
        (hs map hashToString) shouldBe hashes
      }
    }
  }
}

trait MerkleTreeFixture extends SHA256DigestModule with TableDrivenPropertyChecks {

  val testTransactions: Vector[String] = Vector(
    "transaction - 01",
    "transaction - 02",
    "transaction - 03",
    "transaction - 04",
    "transaction - 05",
    "transaction - 06",
    "transaction - 07"
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
}
