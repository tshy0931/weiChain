package tshy0931.com.github.weichain.module

import com.typesafe.config.{Config, ConfigFactory}
import monix.eval.Coeval
import monix.execution.Scheduler
import monix.execution.schedulers.SchedulerService
import tshy0931.com.github.weichain.model.Address
import scala.concurrent.duration._

object ConfigurationModule {

  val config: Config = ConfigFactory.load()

  val version: Int = config.getInt("version.no")
  val versionStartHeight: Int = config.getInt("version.startHeight")
  val services: Long = config.getLong("services")
  val relay: Boolean = config.getBoolean("relay")

  val hostName: String = config.getString("net.host")
  val port: Int = config.getInt("net.port")

  val seeds = List(
    Address("localhost", 8334),
    Address("localhost", 8335),
    Address("localhost", 8336)
  )

  val maxHeadersPerRequest: Int = config.getInt("blockchain.max-headers-per-request")

  val redisHost: String = config.getString("db.redis.host")
  val redisPort: Int = config.getInt("db.redis.port")

  val SCHEDULER_FOR_BLOCK_DOWNLOAD: String = "block-download-scheduler"
  val blockDownloadScheduler: Coeval[SchedulerService] =
    Coeval.evalOnce(Scheduler.io(name = SCHEDULER_FOR_BLOCK_DOWNLOAD))

  val miningRate = 30 seconds
  val rewardAddr = config.getString("mine.rewardAddr")
  val minerPubKeyScript = config.getString("mine.pubKeyScript")
  val minerCoinbaseScript = config.getString("mine.coinbaseScript")
}
