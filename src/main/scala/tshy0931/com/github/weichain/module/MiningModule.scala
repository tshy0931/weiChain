package tshy0931.com.github.weichain.module

import tshy0931.com.github.weichain.DigestModule

/**
  * This trait defines functionality required for mining a valid block.
  */
trait MiningModule {
  //TODO: improve design of difficulty
  /**
    * Find the nonce that will make the hash of given hash and the nonce meet the required difficulty.
    * @param hash the hash to find a nonce against.
    * @param difficulty least number of '0's required at the beginning of hash of the given hash and the nonce found.
    * @return the hash of given hash and the nonce found.
    */
  def findNonce(hash: Array[Byte], difficulty: Int): (Int, Array[Byte])

  /**
    * Verify if the given nonce fulfills the required difficulty.
    * @param nonce
    * @param hash
    * @param difficulty
    * @return
    */
  def verify(nonce: Int, hash: Array[Byte], difficulty: Int): Boolean
}

object MiningModule {

  trait SimpleMiningModule extends MiningModule { this: DigestModule =>

    override def findNonce(hash: Array[Byte], difficulty: Int): (Int, Array[Byte]) = {

      var nonce: Int = 0
      var result: Array[Byte] = Array.emptyByteArray
      do {
        nonce += 1
        result = digestor.digest(hash :+ nonce.toByte)

      } while(!verify(nonce, result, difficulty))

      (nonce, result)
    }

    override def verify(nonce: Int, hash: Array[Byte], difficulty: Int): Boolean = hash.take(difficulty).forall(_ == 0)
  }
}
