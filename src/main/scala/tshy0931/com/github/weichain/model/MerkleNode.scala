package tshy0931.com.github.weichain.model

import cats.syntax.option._
import tshy0931.com.github.weichain.SHA256DigestModule

case class MerkleNode(index: Int,
                      hash: Array[Byte],
                      left: Option[MerkleNode],
                      right: Option[MerkleNode])

object MerkleNode extends SHA256DigestModule {

  private def left(i: Int): Int = 2 * i + 1
  private def right(i: Int): Int = 2 * i + 2
  private def parent(i: Int): Int = (i - 1) / 2
  private def isLeaf(i: Int, size: Int): Boolean = i >= size/2

  def build(documents: Vector[String]): Option[MerkleNode] = buildNodeAt(0, documents)

  private def buildNodeAt(index: Int, documents: Vector[String]): Option[MerkleNode] = {

    if(index >= documents.length) None
    else {
      val hash: Array[Byte] = digestor.digest(documents(index).getBytes("UTF-8"))
      MerkleNode(index, hash, buildNodeAt(left(index), documents), buildNodeAt(right(index), documents)).some
    }
  }
}

