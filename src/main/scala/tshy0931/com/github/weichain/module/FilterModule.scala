package tshy0931.com.github.weichain.module

import java.util.Random

import DigestModule._


/**
  * This module implements Bloom Filters that can be used for
  * - SPV
  * - listen to specific transactions, e.g. ones for a specific wallet
  */
object FilterModule {

  case class BloomFilter(size: Int, salt: Int) {

    lazy val seeds: Vector[Long] = {
      val random = new Random()
      (0 until size) map { _ => random.nextLong() + salt } toVector
    }
  }



  implicit class FilterOps(filter: BloomFilter) {

//    def
  }
}
