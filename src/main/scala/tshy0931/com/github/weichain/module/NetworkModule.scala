package tshy0931.com.github.weichain.module

import java.util.concurrent.ConcurrentHashMap

import akka.Done
import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.RemoteAddress
import akka.stream.ActorMaterializer
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import tshy0931.com.github.weichain._
import DigestModule._
import BlockChainModule._
import FilterModule._
import ValidationModule.TransactionValidation._
import tshy0931.com.github.weichain.message._
import tshy0931.com.github.weichain.model.Block.BlockHeader
import tshy0931.com.github.weichain.network.{Address, PeerProperty}
import ConfigurationModule._
import akka.pattern.CircuitBreaker
import cats.data.Validated.{Invalid, Valid}
import monix.eval.Task
import monix.execution.CancelableFuture
import tshy0931.com.github.weichain.codec.CodecModule._
import tshy0931.com.github.weichain.database.MemPool
import tshy0931.com.github.weichain.model.{Block, Transaction}

import scala.concurrent.Future
import scala.util.{Failure, Random, Success}
import scala.collection.concurrent
import scala.collection.JavaConverters._
import scala.concurrent.duration._

object NetworkModule {

  import Routes._
  import tshy0931.com.github.weichain.network.Protocol.Endpoints._

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  import monix.execution.Scheduler.Implicits.global

  val REGEX_HASH = """[A-Fa-f0-9]{64}""".r

  lazy val log = Logging(system, this.getClass)

  case class ConnectionException(msg: String)

  private val circuitBreaker: CircuitBreaker = new CircuitBreaker(
    system.scheduler,
    maxFailures = 3,
    callTimeout = 10 seconds,
    resetTimeout = 1 seconds
  )

  var networkBinding: Http.ServerBinding = _

  var peers: concurrent.Map[Address, PeerProperty] = new ConcurrentHashMap[Address, PeerProperty]().asScala

  def start: Unit = bind(hostName, port) map { binding => networkBinding = binding } recover {
   case err => log.error("Failed binding to host {} port {}, error {}", hostName, port, err)
  }

  private def bind(host:String, port:Int): Future[ServerBinding] = Http().bindAndHandle(routes, host, port)

  def stop: Future[Done] = {
    log.info("Closing server binding on host {} port {} ...", hostName, port)
    networkBinding.unbind()
  }

  final object Routes {

    lazy val dataRoute: Route = {
      path( DOMAIN_DATA / BLOCK / REGEX_HASH / PathEnd ) { blockHash =>
        get {
          val findBlock: CancelableFuture[Option[Block]] = blockWithHash(blockHash).value.runAsync
          onCompleteWithBreaker(circuitBreaker)(findBlock) {
            case Success(Some(block)) => complete(OK -> block)
            case Success(None)        => complete(NotFound -> s"no block with hash $blockHash")
            case Failure(err)         => complete(InternalServerError -> err)
          }
        }
      } ~
      path( DOMAIN_DATA / BLOCKS / PathEnd ) {
        post {
          decodeRequest {
            entity(as[Blocks]) { blocks =>
              val tasks: Vector[Task[Option[Block]]] = blocks.blockHashes map { hash =>
                blockWithHash(hash.asString).value
              }
              onCompleteWithBreaker(circuitBreaker)(Task.sequence(tasks).runAsync) {
                case Success(blocks) => complete(OK -> blocks)
                case Failure(err)    => complete(InternalServerError -> err)
              }
            }
          }
        }
      } ~
      path( DOMAIN_DATA / HEADERS / PathEnd ) {
        post {
          decodeRequest {
            parameter('count.as[Int].?) { count =>
              //          headerValueByName("X-Header-Count-Requested") { count =>
              entity(as[Vector[BlockHeader]]) { blockHeaders =>
                if(blockHeaders.isEmpty){
                  complete(BadRequest -> "No block header specified in request.")
                } else {
                  val task: Task[(Int, Vector[BlockHeader])] = searchHeadersAfter(blockHeaders, count.getOrElse(maxHeadersPerRequest))
                  onCompleteWithBreaker(circuitBreaker)(task.runAsync) {
                    case Success((forkIndex, headers)) => complete(Headers(headers.size, forkIndex, headers))
                    case Failure(err)                  => complete(InternalServerError -> err)
                  }
                }
              }
            }
          }
        }
      } ~
      //    path( DOMAIN_DATA / "inv") {
      //      get {
      //        ???
      //      } ~
      //        post {
      //          ???
      //        }
      //    } ~
      path( DOMAIN_DATA / "mempool") {
        get {
          val txInMemPool: Task[Seq[Transaction]] = MemPool[Transaction].getAll
          onCompleteWithBreaker(circuitBreaker)(txInMemPool.runAsync) {
            case Success(txs) => complete(MemPoolMsg(txs.size, txs))
            case Failure(err) => complete(InternalServerError -> err)
          }
        }
      } ~
      path( DOMAIN_DATA / BLOCK / REGEX_HASH / REGEX_HASH / MERKLEBLOCK / PathEnd ) { (blockHash, txHash) =>
        get {
          val task: Task[Option[MerkleBlock]] = merkleBlockOf(blockHash, txHash).value
          onCompleteWithBreaker(circuitBreaker)(task.runAsync) {
            case Success(Some(merkleBlock)) => complete(OK -> merkleBlock)
            case Success(None)              => complete(NotFound -> s"merkle block not found for block $blockHash tx $txHash")
            case Failure(err)               => complete(InternalServerError -> err)
          }
        }
      } ~
      path( DOMAIN_DATA / TX / PathEnd ) {
        post {
          decodeRequest {
            entity(as[Transaction]) { tx =>
              onCompleteWithBreaker(circuitBreaker)(verifyTx(tx).runAsync) {
                case Success(Valid(tx))    =>
                  MemPool[Transaction].put(tx, tx.createTime)
                  complete(OK -> s"transaction ${tx.hash} is validated and received.")
                case Success(Invalid(err)) => complete(BadRequest -> err)
                case Failure(err)          => complete(InternalServerError -> err)
              }
            }
          }
        }
      }
    }

    lazy val controlRoute: Route = {
      path( DOMAIN_CTRL / ADDRESS / PathEnd ) {
        get {
          complete(peers.keys.toVector)
          //      complete(Marshal(peers.keys.toList).to[List[Address]])
        } ~
        post {
          decodeRequest {
            entity(as[Vector[Address]]) { addrList =>
              peers ++= addrList map { addr => (addr, PeerProperty(active = true, sendHeaders = false))}
              complete("peers received.")
            }
          }
        }
      } ~
        //    path( DOMAIN_CTRL / "alert") {
        //      get {
        //        ???
        //      } ~
        //        post {
        //          ???
        //        }
        //    } ~
        //    path( DOMAIN_CTRL / "feefilter") {
        //      get {
        //        ???
        //      } ~
        //        post {
        //          ???
        //        }
        //    } ~
      path( DOMAIN_CTRL / FILTERADD / PathEnd ) {
        post {
          decodeRequest {
            parameter('type) {
              case "tx" =>
                entity(as[FilterAdd]) { payload =>
                  complete(addToFilter(payload))
                }
//              case "address" =>
//                entity(as[FilterAdd[Address]]) { payload =>
//                  complete(addToFilter[Address](payload))
//                }
            }
          }
        }
      } ~
      path( DOMAIN_CTRL / FILTERCLEAR / REGEX_HASH / PathEnd ) { owner =>
        delete {
          parameter('type) {
            case "tx" => complete(deleteTxFilter(owner))
//            case "address" => complete(deleteFilter[Address](owner))
          }
        }
      } ~
      path( DOMAIN_CTRL / FILTERLOAD / PathEnd ) {
        post {
          decodeRequest {
            parameter('type) {
              case "tx"      =>
                entity(as[FilterLoad]) { payload =>
                  complete(loadFilter(payload))
                }
//              case "address" =>
//                entity(as[FilterLoad[Address]]) { payload =>
//                  complete(loadFilter[Address](payload))
//                }
              case other     => complete(400 -> s"Unsupported filter type: $other")
            }
          }
        }
      } ~
      path( DOMAIN_CTRL / PING / PathEnd ) {
        get {
          complete("pong")
        }
      } ~
      //      path( DOMAIN_CTRL / "reject") {
      //        get {
      //          ???
      //        } ~
      //          post {
      //            ???
      //          }
      //      } ~
      path( DOMAIN_CTRL / SENDHEADERS / PathEnd ) {
        get {
          extractClientIP { ip =>
            peers.replace(ip.toAddress, PeerProperty(active = true, sendHeaders = true))
            complete("ok")
          }
        }
      } ~
      path( DOMAIN_CTRL / VERACK / PathEnd ) {
        post {
          val header = MessageHeader(
            commandName = "version",
            payloadSize = 0L,
            checksum = digest(digest(emptyHash)).asString
          )
          complete(header)
        }
      } ~
      path( DOMAIN_CTRL / VERSION / PathEnd ) {
        extractClientIP { ip =>
          post {
            entity(as[Version]) { ver =>

              peers.putIfAbsent(ip.toAddress, PeerProperty(active = true, sendHeaders = false))
              val msg = Version(version,services, System.currentTimeMillis(), Random.nextLong(), chainHeight, relay)
              //            val header = MessageHeader(commandName = "version", payloadSize = 0L, checksum = digest(digest(msg.toString.getBytes("UTF-8"))))
              complete(msg)
            }
          }
        }
      }
    }

    lazy val routes: Route = dataRoute ~ controlRoute
  }

  implicit class remoteAddressOps(remoteAddress: RemoteAddress) {

    def toAddress: Address = {
      remoteAddress.toOption map { addr => Address(addr.getHostName, remoteAddress.getPort) } get
    }
  }
}
