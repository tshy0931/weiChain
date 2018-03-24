package tshy0931.com.github.weichain.module

import java.util.concurrent.ConcurrentHashMap

import akka.Done
import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.model.{HttpResponse, RemoteAddress}
import akka.stream.ActorMaterializer
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import tshy0931.com.github.weichain._
import DigestModule._
import BlockChainModule._
import tshy0931.com.github.weichain.message.{Blocks, Headers, MessageHeader, Version}
import tshy0931.com.github.weichain.model.Block.BlockHeader
import tshy0931.com.github.weichain.network.Address
import ConfigurationModule._
import spray.json.DefaultJsonProtocol
import tshy0931.com.github.weichain.codec.CodecModule

import scala.concurrent.Future
import scala.util.Random
import scala.collection.concurrent
import scala.collection.JavaConverters._

object NetworkModule extends CodecModule {

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher

  lazy val log = Logging(system, this.getClass)

  case class ConnectionException(msg: String)

  var networkBinding: Http.ServerBinding = _

  var peers: concurrent.Map[Address, Boolean] = new ConcurrentHashMap[Address, Boolean]().asScala

  def start: Unit = bind(hostName, port) map { binding => networkBinding = binding } recover {
   case err => log.error("Failed binding to host {} port {}, error {}", hostName, port, err)
  }

  private def bind(host:String, port:Int): Future[ServerBinding] = Http().bindAndHandle(routes, host, port)

  def stop: Future[Done] = {
    log.info("Closing server binding on host {} port {} ...", hostName, port)
    networkBinding.unbind()
  }

  lazy val routes: Route = dataRoute ~ controlRoute

  val dataRoute = {
    path("data" / "block" / IntNumber) { blockId =>
      get {
        ???
      }
    } ~
    path("data" / "blocks") {
      post {
        decodeRequest {
          entity(as[Blocks]) { blocks =>
            complete(getBlocksByHashes(blocks.blockHashes))
          }
        }
      }
    } ~
    path("data" / "headers") {
      post {
        decodeRequest {
          headerValueByName("X-Header-Count-Requested") { count =>
            entity(as[Vector[BlockHeader]]) { blockHeaders =>

              if(blockHeaders.isEmpty){
                complete(400 -> "No block headers found.")
              }else{
                val (forkIndex, headers) = searchHeadersAfter(blockHeaders, count.toInt)
                complete(Headers(headers.size, forkIndex, headers))
              }
            }
          }
        }
      }
    } //~
//    path("data" / "inv") {
//      get {
//        ???
//      } ~
//        post {
//          ???
//        }
//    } ~
//    path("data" / "mempool") {
//      get {
//        ???
//      } ~
//        post {
//          ???
//        }
//    } ~
//    path("data" / "inv") {
//      get {
//        ???
//      } ~
//        post {
//          ???
//        }
//    } ~
//    path("data" / "merkleblock") {
//      get {
//        ???
//      } ~
//        post {
//          ???
//        }
//    } ~
//    path("data" / "notfound") {
//      get {
//        ???
//      } ~
//        post {
//          ???
//        }
//    } ~
//    path("data" / "tx") {
//      get {
//        ???
//      } ~
//        post {
//          ???
//        }
//    } ~
//    path("data" / PathEnd) {
//      get {
//        ???
//      }
//    }
  }

  val controlRoute = {
    path("control" / "addr") {
      get {
        complete(peers.keys.toVector)
//      complete(Marshal(peers.keys.toList).to[List[Address]])
      } ~
      post {
        decodeRequest {
          entity(as[Vector[Address]]) { addrList =>
            peers ++= addrList map { addr => (addr, true)}
            complete("peers received.")
          }
        }
      }
    } ~
//      path("control" / "alert") {
//        get {
//          ???
//        } ~
//          post {
//            ???
//          }
//      } ~
//      path("control" / "feefilter") {
//        get {
//          ???
//        } ~
//          post {
//            ???
//          }
//      } ~
//      path("control" / "filteradd") {
//        get {
//          ???
//        } ~
//          post {
//            ???
//          }
//      } ~
//      path("control" / "filterclear") {
//        get {
//          ???
//        } ~
//          post {
//            ???
//          }
//      } ~
//      path("control" / "filterload") {
//        get {
//          ???
//        } ~
//          post {
//            ???
//          }
//      } ~
//      path("control" / "ping") {
//        get {
//          ???
//        } ~
//          post {
//            ???
//          }
//      } ~
//      path("control" / "pong") {
//        get {
//          ???
//        } ~
//          post {
//            ???
//          }
//      } ~
//      path("control" / "reject") {
//        get {
//          ???
//        } ~
//          post {
//            ???
//          }
//      } ~
//      path("control" / "sendheaders") {
//        get {
//          ???
//        } ~
//          post {
//            ???
//          }
//      } ~
      path("control" / "verack") {
        post {
          val header = MessageHeader(commandName = "version", payloadSize = 0L, checksum = digest(digest("".getBytes("UTF-8"))))
          complete("verack")
        }
      } ~
      path("control" / "version") {
        extractClientIP { ip =>
          post {
              entity(as[Version]) { ver =>

                peers.putIfAbsent(ip.toAddress, true)
                val msg = Version(version,services, System.currentTimeMillis(), Random.nextLong(), chainHeight, relay)
//                val header = MessageHeader(commandName = "version", payloadSize = 0L, checksum = digest(digest(msg.toString.getBytes("UTF-8"))))
                complete(msg)
              }
          }
        }
      }
  }

  implicit class remoteAddressOps(remoteAddress: RemoteAddress) {

    def toAddress: Address = {
      remoteAddress.toOption map { addr => Address(addr.getHostName, remoteAddress.getPort) } get
    }
  }
}
