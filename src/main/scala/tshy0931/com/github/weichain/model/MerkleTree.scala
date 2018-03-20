package tshy0931.com.github.weichain.model

import tshy0931.com.github.weichain._
import scala.collection.mutable

case class MerkleTree(hashes: Vector[Hash]) {

  private lazy val lookup: Map[String, Int] = hashes.map(hashToString).zipWithIndex.toMap

  def indexOf(txHash: Hash): Option[Int] = lookup.get(txHash)
}

object MerkleTree extends SHA256DigestModule {

  def build(documents: Vector[String]): MerkleTree = {

    var tree: Vector[Hash] = documents map { d => digestor.digest(d.getBytes("UTF-8"))}
    var toMerge: Vector[Hash] = tree

    while(toMerge.length > 1){
      val newLayer = (toMerge.grouped(2) map { pair =>
        val hash1: mutable.Buffer[Byte] = pair.head.toBuffer
        hash1.append(pair.last:_*)
        digestor.digest(hash1.toArray)
      }).toVector
      tree = newLayer ++: tree
      toMerge = newLayer
    }
    MerkleTree(tree)
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

    private[model] def isLeft(parent: Int, child: Int): Boolean = (parent << 1) + 1 == child
    private[model] def isRight(parent: Int, child: Int): Boolean = (parent << 1) + 2 == child
    private[model] def isLeaf(i: Int): Boolean = left(i).isEmpty

    def size:Int = tree.hashes.length
    def hashAt(index: Int): Hash = tree.hashes(index)

    def derivePath(targetTx: Hash): Option[List[Int]] = {
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

    def deriveFlagsAndHashes(targetTx: Hash): Option[(Vector[Int], Vector[Hash])] = {

      derivePath(targetTx) map { path =>

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

        (flags, hashes)
      }
    }
  }
}

