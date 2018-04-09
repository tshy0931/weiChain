package tshy0931.com.github.weichain.module

import akka.event.Logging
import tshy0931.com.github.weichain.module.NetworkModule.system

import scala.util.{Failure, Success}

object ApplicationModule extends App {

  import monix.execution.Scheduler.Implicits.global
  lazy val log = Logging(system, this.getClass)

  NetworkModule.start.onComplete {
    case Success(_)   => log.info("WeiChain client successfully started.")
    case Failure(err) => log.error("Failed to start WeiChain, error: {}", err)
  }
}
