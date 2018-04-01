package tshy0931.com.github.weichain.module

import DigestModule._

/**
  * This trait defines functionality required for mining a valid block.
  */
object MiningModule {

  /**
    * Difficulty of mining, number of '0's required in a valid block header hash
    * should vary regarding specific rules.
    *
    */
  def difficulty: String = "00"

  def findNonce(hash: Array[Byte], difficulty: Int): (Int, Array[Byte]) = {

    var nonce: Int = 0
    var result: Array[Byte] = Array.emptyByteArray
    do {
      nonce += 1
      result = digest(hash :+ nonce.toByte)

    } while(!verify(nonce, result, difficulty))

    (nonce, result)
  }

  def verify(nonce: Int, hash: Array[Byte], difficulty: Int): Boolean = hash.take(difficulty).forall(_ == 0)

}
