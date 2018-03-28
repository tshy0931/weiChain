package tshy0931.com.github

package object weichain {

  type Hash = Array[Byte]

  val emptyHash: Hash = "".getBytes("UTF-8")

  implicit class HashOps(hash: Hash) {

    def asString: String = hash.map("%02x".format(_)).mkString
  }
}
