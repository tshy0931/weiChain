package tshy0931.com.github

package object weichain {

  type Hash = String//Array[Byte]

  val emptyHash: Hash = ""

  /*** easter egg! ***/
  type WhatEver[A,B] = (A, B)
  def `¯|_(ツ)_|¯`(left: Int, right: Boolean): WhatEver[Int, Boolean] = (left, right)

  implicit class WhatEverOps(a: Any) {

    def `¯|_(ツ)_|¯`(b: Any): WhatEver[Any, Any] = (a, b)
  }

//  implicit class HashOps(hash: Hash) {
//    def toUTF8String: String = new String(hash, "UTF-8")
//  }
//
//  implicit class StringOps(str: String) {
//    def toHash: Hash = str.getBytes("UTF-8")
//  }
}
