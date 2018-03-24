package tshy0931.com.github.weichain.module

import com.typesafe.config.{Config, ConfigFactory}
import tshy0931.com.github.weichain.network.Address

object ConfigurationModule {

  val config: Config = ConfigFactory.load()

  val version: Int = config.getInt("version")
  val services: Long = config.getLong("services")
  val relay: Boolean = config.getBoolean("relay")

  val hostName = config.getString("net.host")
  val port = config.getInt("net.port")

  val seeds = List(
    Address("localhost", 8334),
    Address("localhost", 8335),
    Address("localhost", 8336)
  )

  val maxHeadersPerRequest: Int = config.getInt("blockchain.max-headers-per-request")

}
