package tshy0931.com.github.weichain.module

import java.util.concurrent.ConcurrentHashMap

import akka.Done
import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.model.{RemoteAddress, StatusCodes}
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
import cats.data.Validated.{Invalid, Valid}
import tshy0931.com.github.weichain.codec.CodecModule._
import tshy0931.com.github.weichain.model.Transaction

import scala.concurrent.Future
import scala.util.Random
import scala.collection.concurrent
import scala.collection.JavaConverters._

object NetworkModule {

  import Routes._
  import tshy0931.com.github.weichain.network.Protocol
  import tshy0931.com.github.weichain.network.Protocol.Endpoints._

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher

  val REGEX_HASH = """[A-Fa-f0-9]{64}""".r

  lazy val log = Logging(system, this.getClass)

  case class ConnectionException(msg: String)

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

  final case object Routes {

    lazy val dataRoute: Route = {
      path( DOMAIN_DATA / BLOCK / REGEX_HASH ) { blockHash =>
        get {
          complete(blockWithHash(blockHash))
        }
      } ~
      path( DOMAIN_DATA / BLOCKS ) {
        post {
          decodeRequest {
            entity(as[Blocks]) { blocks =>
              complete(getBlocksByHashes(blocks.blockHashes))
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
                  complete(400 -> "No block header specified in request.")
                }else{
                  val (forkIndex, headers) = searchHeadersAfter(blockHeaders, count.getOrElse(maxHeadersPerRequest))
                  complete(Headers(headers.size, forkIndex, headers))
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
      //    path( DOMAIN_DATA / "mempool") {
      //      get {
      //        ???
      //      } ~
      //        post {
      //          ???
      //        }
      //    } ~
      path( DOMAIN_DATA / BLOCK / REGEX_HASH / REGEX_HASH / MERKLEBLOCK ) { (blockHash, txHash) =>
        get {
          complete(merkleBlockOf(blockHash, txHash))
        }
      } ~
      path( DOMAIN_DATA / TX / REGEX_HASH ) { txHash =>
        post {
          decodeRequest {
            entity(as[Transaction]) { tx =>
              verifyTx(tx) match {
                case Valid(_) =>
                  //TODO: Decide whether relay this tx
                  memPool.putIfAbsent(txHash, tx)
                  complete(s"tx $txHash received")

                case Invalid(err) => complete(StatusCodes.BadRequest -> err)
              }
            }
          }
        }
      }
    }

    lazy val controlRoute: Route = {
      path( DOMAIN_CTRL / ADDRESS ) {
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
      path( DOMAIN_CTRL / SENDHEADERS ) {
        get {
          extractClientIP { ip =>
            peers.replace(ip.toAddress, PeerProperty(active = true, sendHeaders = true))
            complete("ok")
          }
        }
      } ~
      path( DOMAIN_CTRL / VERACK ) {
        post {
          val header = MessageHeader(
            commandName = "version",
            payloadSize = 0L,
            checksum = digest(digest(emptyHash)).asString
          )
          complete(header)
        }
      } ~
      path( DOMAIN_CTRL / VERSION ) {
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
