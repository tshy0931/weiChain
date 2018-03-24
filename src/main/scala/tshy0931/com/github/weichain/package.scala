package tshy0931.com.github

package object weichain {

  type Hash = Array[Byte]

  implicit def hashToString(array: Hash): String = array.map("%02x".format(_)).mkString

}
