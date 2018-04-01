package tshy0931.com.github.weichain.network

case class Address(host: String, port: Int)

object Address {

  implicit class AddressOps(address: Address) {

    def asString: String = s"${address.host}:${address.port}"
    def asUri(path: String*): String = path mkString (asString, "/", "")
  }
}

