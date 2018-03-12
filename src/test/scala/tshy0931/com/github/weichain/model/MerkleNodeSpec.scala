package tshy0931.com.github.weichain.model

import org.scalatest._

class MerkleNodeSpec extends FlatSpec with GivenWhenThen with Matchers with Inside {

  behavior of "Merkle Tree"

  it should "build correct Merkle Tree from a collection of string documents" in {

    val documents = Vector(
      "transaction - 01",
      "transaction - 02",
      "transaction - 03",
      "transaction - 04",
      "transaction - 05",
      "transaction - 06",
      "transaction - 07",
      "transaction - 08"
    )

    inside (MerkleNode.build(documents)) { case Some(MerkleNode(0, _, left, right)) =>

      inside (left) { case Some(MerkleNode(index, _, left, right)) =>
        index shouldBe 1

        inside (left) { case Some(MerkleNode(index, _, left, right)) =>
          index shouldBe 3

          inside (left) { case Some(MerkleNode(index, _, left, right)) =>
            index shouldBe 7
          }
          right shouldBe None
        }
        inside (right) { case Some(MerkleNode(index, _, left, right)) =>
          index shouldBe 4
          left shouldBe None
          right shouldBe None
        }
      }

      inside (right) { case Some(MerkleNode(index, _, left, right)) =>
        index shouldBe 2

        inside (left) { case Some(MerkleNode(index, _, left, right)) =>
          index shouldBe 5
          left shouldBe None
          right shouldBe None
        }
        inside (right) { case Some(MerkleNode(index, _, left, right)) =>
          index shouldBe 6
          left shouldBe None
          right shouldBe None
        }
      }
    }
  }

  it should "always generate the same hash for the same document" in {

    val documents = Vector(
      "transaction - 01",
      "transaction - 02",
      "transaction - 03"
    )

    val tree1 = MerkleNode.build(documents)
    val tree2 = MerkleNode.build(documents)

    tree1.get.hash shouldBe tree2.get.hash
    tree1.get.left.get.hash shouldBe tree2.get.left.get.hash
    tree1.get.right.get.hash shouldBe tree2.get.right.get.hash
  }
}
