package tshy0931.com.github.weichain.module

import akka.Done
import akka.actor.{ActorRef, ActorSystem}
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
import tshy0931.com.github.weichain.model.Block.{BlockBody, BlockHeader}
import ConfigurationModule._
import ValidationModule.BlockBodyValidation._
import akka.pattern.CircuitBreaker
import cats.data.Validated
import cats.data.Validated.{Invalid, Valid}
import cats.syntax.all._
import monix.eval.Task
import tshy0931.com.github.weichain.codec.CodecModule._
import tshy0931.com.github.weichain.database.{Database, MemPool}
import tshy0931.com.github.weichain.model.{Address, Block, Transaction}
import tshy0931.com.github.weichain.network.P2PClient
import tshy0931.com.github.weichain.network.P2PClient.Mine

import scala.concurrent.Future
import scala.util.{Failure, Random, Success}
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

  val client: ActorRef = system.actorOf(P2PClient.props)

  var networkBinding: Http.ServerBinding = _

  def start: Task[Unit] = {
    (loadSeedPeers, Task fromFuture bind(hostName, port)) parMapN {
      (_, binding) => networkBinding = binding
    } recover {
      case err => log.error("Failed binding to host {} port {}, error {}", hostName, port, err)
    }
  }

  private def loadSeedPeers: Task[List[Unit]] = Task.gatherUnordered {
    seeds map { MemPool[Address].put(_, System.currentTimeMillis) }
  }

  private def bind(host:String, port:Int): Future[ServerBinding] = Http().bindAndHandle(routes, host, port)

  def stop: Future[Done] = {
    log.info("Closing server binding on host {} port {} ...", hostName, port)
    networkBinding.unbind()
  }

  final object Routes {

    lazy val dataRoute: Route = {
      path( DOMAIN_DATA / BLOCK ) {
        post {
          decodeRequest {
            entity(as[Block]) { block =>
              onCompleteWithBreaker(circuitBreaker)(verifyBlockBody(block.body) runAsync) {
                case Success(Valid(blockBody)) => complete(OK -> s"block with hash ${blockBody.headerHash} is received")
                case Success(Invalid(error))   => complete(BadRequest -> s"invalid block body, error: $error")
                case Failure(err)              => complete(InternalServerError -> err)
              }
            }
          }
        }
      } ~
      path( DOMAIN_DATA / BLOCK / REGEX_HASH ) { blockHash =>
        get {
          val findBlock: Task[Option[Block]] = blockWithHash(blockHash).value
          onCompleteWithBreaker(circuitBreaker)(findBlock runAsync) {
            case Success(Some(block)) => complete(OK -> block)
            case Success(None)        => complete(NotFound -> s"no block with hash $blockHash")
            case Failure(err)         => complete(InternalServerError -> err)
          }
        }
      } ~
      path( DOMAIN_DATA / BLOCKS ) {
        post {
          decodeRequest {
            entity(as[Blocks]) { blocks =>
              val tasks: Vector[Task[Option[Block]]] = blocks.blockHashes map { hash =>
                blockWithHash(hash).value
              }
              onCompleteWithBreaker(circuitBreaker)(Task.sequence(tasks).runAsync) {
                case Success(blocks) => complete(OK -> blocks)
                case Failure(err)    => complete(InternalServerError -> err)
              }
            }
          }
        }
      } ~
      path( DOMAIN_DATA / HEADERS ) {
        post {
          decodeRequest {
            parameter('count.as[Int].?) { count =>
              //          headerValueByName("X-Header-Count-Requested") { count =>
              entity(as[Vector[BlockHeader]]) { blockHeaders =>
                if(blockHeaders.isEmpty){
                  complete(BadRequest -> "No block header specified in request.")
                } else {
                  val task: Task[(Int, Seq[BlockHeader])] = searchHeadersAfter(blockHeaders, count.getOrElse(maxHeadersPerRequest))
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
      path( DOMAIN_DATA / MEMPOOL ) {
        get {
          val txInMemPool: Task[Seq[Transaction]] = MemPool[Transaction].getAll
          onCompleteWithBreaker(circuitBreaker)(txInMemPool.runAsync) {
            case Success(txs) => complete(MemPoolMsg(txs.size, txs))
            case Failure(err) => complete(InternalServerError -> err)
          }
        }
      } ~
      path( DOMAIN_DATA / BLOCK / REGEX_HASH / TX / REGEX_HASH / MERKLEBLOCK ) { (blockHash, txHash) =>
        get {
          val task: Task[Option[MerkleBlock]] = merkleBlockOf(blockHash, txHash).value
          onCompleteWithBreaker(circuitBreaker)(task.runAsync) {
            case Success(Some(merkleBlock)) => complete(OK -> merkleBlock)
            case Success(None)              => complete(NotFound -> s"merkle block not found for block $blockHash tx $txHash")
            case Failure(err)               => complete(InternalServerError -> err)
          }
        }
      } ~
      path( DOMAIN_DATA / TX ) {
        post {
          decodeRequest {
            entity(as[Transaction]) { tx =>
              val verifyThenCache = verifyTx(tx) flatMap {
                case valid @ Valid(tx) =>
                  MemPool[Transaction].put(tx, tx.createTime) map { _ => valid }
                case invalid @ Invalid(_) => Task.now(invalid)
              }
              onCompleteWithBreaker(circuitBreaker)(verifyThenCache runAsync) {
                case Success(Valid(tx))    =>
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
      path( DOMAIN_CTRL / ADDRESS ) {
        get {
          val peers = MemPool[Address].getAll
          onCompleteWithBreaker(circuitBreaker)(peers runAsync) {
            case Success(addrs) => complete(OK -> addrs)
            case Failure(err)   => complete(InternalServerError -> err)
          }
        } ~
        post {
          decodeRequest {
            entity(as[Vector[Address]]) { addrList =>
              val time = System.currentTimeMillis
              val task = Task.gatherUnordered( addrList map { addr => MemPool[Address].put(addr, time) } )
              onCompleteWithBreaker(circuitBreaker)(task runAsync) {
                case Success(_)   => complete(OK -> "peer addresses saved.")
                case Failure(err) => complete(InternalServerError -> err)
              }
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
      path( DOMAIN_CTRL / FILTERADD ) {
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
      path( DOMAIN_CTRL / FILTERCLEAR / REGEX_HASH ) { owner =>
        delete {
          parameter('type) {
            case "tx" => complete(deleteTxFilter(owner))
//            case "address" => complete(deleteFilter[Address](owner))
          }
        }
      } ~
      path( DOMAIN_CTRL / FILTERLOAD ) {
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
      path( DOMAIN_CTRL / PING ) {
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
//      path( DOMAIN_CTRL / SENDHEADERS ) {
//        get {
//          extractClientIP { ip =>
//            peers.replace(ip.toAddress, PeerProperty(active = true, sendHeaders = true))
//            complete("ok")
//          }
//        }
//      } ~
      path( DOMAIN_CTRL / VERACK ) {
        post {
          val header = MessageHeader(
            commandName = "version",
            checksum = digest(digest(emptyHash))
          )
          complete(header)
        }
      } ~
      path( DOMAIN_CTRL / VERSION ) {
        extractClientIP { ip =>
          post {
            entity(as[Version]) { _ =>
              val task: Task[Version] = MemPool[Address].put(ip.toAddress, System.currentTimeMillis) map { _ =>
                Version(version, services, System.currentTimeMillis(), Random.nextLong(), versionStartHeight, relay)
              }

              onCompleteWithBreaker(circuitBreaker)(task runAsync) {
                case Success(msg) => complete(OK -> msg)
                case Failure(err) => complete(InternalServerError -> err)
              }
            }
          }
        }
      }
    }

    lazy val testRoute: Route = {
      path("test" / "mine" ) {
        get {
          onCompleteWithBreaker(circuitBreaker)(latestHeader map { header => client ! Mine(header)} runAsync) {
            case Success(blk) => complete(OK)
            case Failure(err) => complete(InternalServerError -> err)
          }
        }
      } ~
      path("test" / "block" / REGEX_HASH ) { blockHash =>
        get {
          onCompleteWithBreaker(circuitBreaker)(Database[BlockBody].find(blockHash) runAsync) {
            case Success(Some(msg)) => complete(OK -> msg)
            case Success(None) => complete(NotFound -> "no such block")
            case Failure(err) => complete(InternalServerError -> err)
          }
        }
      } ~
      path("test" / "header" / REGEX_HASH ) { blockHash =>
        get {
          onCompleteWithBreaker(circuitBreaker)(Database[BlockHeader].find(blockHash) runAsync) {
            case Success(Some(msg)) => complete(OK -> msg)
            case Success(None) => complete(NotFound -> "no such block")
            case Failure(err) => complete(InternalServerError -> err)
          }
        }
      }
    }

    lazy val routes: Route = dataRoute ~ controlRoute ~ testRoute
  }

  implicit class remoteAddressOps(remoteAddress: RemoteAddress) {

    def toAddress: Address = {
      remoteAddress.toOption map { addr => Address(addr.getHostName, remoteAddress.getPort) } get
    }
  }
}
