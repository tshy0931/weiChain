package tshy0931.com.github

package object weichain {

  type Hash = Array[Byte]

  val emptyHash: Hash = "".getBytes("UTF-8")

  /*** easter egg! ***/
//  type WhatEver[A,B] = (A, B)
//  def `¯\_(ツ)_/¯`(left: Int, right: Boolean): WhatEver[Int, Boolean] = (left, right)


  implicit class HashOps(hash: Hash) {

    def asString: String = hash.map("%02x".format(_)).mkString
  }
}
