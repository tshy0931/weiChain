package tshy0931.com.github.weichain.model

import tshy0931.com.github.weichain._
import tshy0931.com.github.weichain.message.MerkleBlock
import tshy0931.com.github.weichain.model.Block.BlockHeader
import cats.syntax.option._
import tshy0931.com.github.weichain.module.DigestModule._

case class MerkleTree(hashes: Vector[Hash], nTx: Int) {

  private lazy val lookup: Map[String, Int] = hashes.map(hashToString).zipWithIndex.toMap

  def indexOf(txHash: Hash): Option[Int] = lookup.get(txHash)
}

object MerkleTree {

  def build(documents: Vector[String]): MerkleTree = {

    var tree: Vector[Hash] = documents map { d => digest(d.getBytes("UTF-8"))}
    var toMerge: Vector[Hash] = tree

    while(toMerge.length > 1){
      val newLayer = (toMerge.grouped(2) map { pair => merge(pair.head, pair.last) }).toVector
      tree = newLayer ++: tree
      toMerge = newLayer
    }

    MerkleTree(tree, documents.size)
  }

  implicit class MerkleTreeOps(tree: MerkleTree) {

    private[model] def left(i: Int): Option[Int] = {
      val left = (i << 1) + 1
      if(left < size) Some(left) else None
    }
    private[model] def right(i: Int): Option[Int] = {
      val right = (i << 1) + 2
      if(right < size) Some(right) else None
    }
    private[model] def parent(i: Int): Option[Int] = if(i < size) Some((i - 1) >> 1) else None

    private[model] def isLeft(i: Int): Boolean = (i & 1)  == 1
    private[model] def isRight(i: Int): Boolean = (i & 1) == 0
    private[model] def isLeaf(i: Int): Boolean = left(i).isEmpty

    def size:Int = tree.hashes.length
    def hashAt(index: Int): Hash = tree.hashes(index)

    def derivePath(targetTx: String): Option[List[Int]] = {
      tree.lookup.get(targetTx) map { index =>
        var curr = index
        var path = List(index)
        while(curr > 0){
          curr = parent(curr).get
          path = curr :: path
        }
        path
      }
    }

    def deriveMerkleBlockFor(targetTxHash: String)(blockHeader: BlockHeader, nTx: Long): Option[MerkleBlock] = {

      derivePath(targetTxHash) map { path =>

        var stack: List[Int] = List(0)
        var currentIndex: Int = 0
        var currentFlag: Int = -1
        var remainingPath: List[Int] = path
        var flags: Vector[Int] = Vector.empty[Int]
        var hashes: Vector[Hash] = Vector.empty[Hash]

        while(stack.nonEmpty) {
          currentIndex = stack.head
          stack = stack.tail
          currentFlag = if(remainingPath.nonEmpty && currentIndex == remainingPath.head) 1 else 0

          if(isLeaf(currentIndex) || currentFlag == 0) {
            hashes = hashes :+ hashAt(currentIndex)
          }

          if(currentFlag == 1){

            right(currentIndex) foreach {right => stack = right :: stack}
            left(currentIndex) foreach {left => stack = left :: stack}
            remainingPath = remainingPath.tail
          }
          flags = flags :+ currentFlag
        }

        MerkleBlock(blockHeader, nTx, hashes, flags)
      }
    }

    def parseMerkleBlock(merkleBlock: MerkleBlock): Option[(Hash, Int)] = {
      // TODO - Support invalid cases like transaction not found and root hash not matching
      val (rootHash, location, _, _) = parseFlagsAndHashes(0, merkleBlock.flags, merkleBlock.hashes)
      location map { l => (rootHash, l - (size - tree.nTx)) }
    }

    private def parseFlagsAndHashes(index: Int, flags: Vector[Int], hashes: Vector[Hash]): (Hash, Option[Int], Vector[Int], Vector[Hash]) = {

      if(flags.head == 1 && !isLeaf(index)){
        // flag = 1 and Non-TXID Node
        // The hash needs to be computed.
        // Process the left child node to get its hash;
        // process the right child node to get its hash;
        // then concatenate the two hashes as 64 raw bytes and hash them to get this node’s hash.

        val (leftHash, location1, remainingFlags1, remainingHashes1) = parseFlagsAndHashes(left(index).get, flags.tail, hashes)
        val (rightHash, location2, remainingFlags2, remainingHashes2) =
          right(index).map(parseFlagsAndHashes(_, remainingFlags1, remainingHashes1))
                      .orElse((leftHash, None, remainingFlags1, remainingHashes1).some)
                      .get

        (merge(leftHash, rightHash), location1 orElse location2, remainingFlags2, remainingHashes2)

      } else if(flags.head == 1 && isLeaf(index)){
        // flag = 1 and TXID Node
        // Use the next hash as this node’s TXID, and mark this transaction as matching the filter.
        (hashes.head, Some(index), flags.tail, hashes.tail)

      } else {
        // flag = 0
        // if TXID, use the next hash as this node’s TXID, but this transaction didn’t match the filter.
        // if Non-TXID, use the next hash as this node’s hash. Don’t process any descendant nodes.
        (hashes.head, None, flags.tail, hashes.tail)
      }
    }
  }
}

