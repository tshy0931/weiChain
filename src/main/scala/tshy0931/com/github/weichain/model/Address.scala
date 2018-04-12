package tshy0931.com.github.weichain.model

case class Address(host: String, port: Int)

object Address {

  implicit class AddressOps(address: Address) {

    def toUTF8String: String = s"${address.host}:${address.port}"
    def asUri(path: String*): String = path mkString (toUTF8String, "/", "")
  }
}

