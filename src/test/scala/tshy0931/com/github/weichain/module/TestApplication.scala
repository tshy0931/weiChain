package tshy0931.com.github.weichain.module

import cats.syntax.all._
import akka.event.Logging
import monix.eval.Task
import tshy0931.com.github.weichain.database.{Database, MemPool}
import tshy0931.com.github.weichain.model.{Block, Transaction}
import tshy0931.com.github.weichain.module.NetworkModule.system
import tshy0931.com.github.weichain.model.Block.{BlockBody, BlockHeader}
import tshy0931.com.github.weichain.testdata.TestApplicationData

import scala.concurrent.duration._
import scala.util.{Failure, Success}

object TestApplication extends App
  with ValidationModuleFixture
  with TestApplicationData {

  import monix.execution.Scheduler.Implicits.global
  lazy val log = Logging(system, this.getClass)

  val start = (loadTestTx, loadTestBlock) parMapN {
    (_, _) => NetworkModule.start.onComplete {
      case Success(_)   => log.info("WeiChain client successfully started.")
      case Failure(err) => log.error("Failed to start WeiChain, error: {}", err)
    }
  } runSyncUnsafe(240 seconds)

  private def loadTestTx: Task[List[Unit]] = Task gatherUnordered {
    Seq(tx1, tx2) map { tx => MemPool[Transaction].put(tx, tx.createTime)}
  }

  private def loadTestBlock: Task[List[Unit]] = Task gatherUnordered {
    Seq(blk1, blk2) map { case Block(header, body) =>
      (Database[BlockHeader].save(header), Database[BlockBody].save(body)) parMapN {
        case (true, true) => log.info("test blocks loaded")
        case _ => log.error("Initialization failed on loading test blocks")
      }
    }
  }
}
