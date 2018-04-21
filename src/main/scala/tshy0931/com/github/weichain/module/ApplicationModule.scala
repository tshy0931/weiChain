package tshy0931.com.github.weichain.module

import cats.syntax.all._
import akka.event.Logging
import tshy0931.com.github.weichain.module.NetworkModule.system
import scala.concurrent.duration._

object ApplicationModule extends App {

  import monix.execution.Scheduler.Implicits.global
  lazy val log = Logging(system, this.getClass)

  val start = (BlockChainModule.start, NetworkModule.start) parMapN {
    (_, _) =>
  } onErrorHandle { err =>
    log.error("Failed to start WeiChain, error: {}", err)
  } runSyncUnsafe(60 seconds)
}
