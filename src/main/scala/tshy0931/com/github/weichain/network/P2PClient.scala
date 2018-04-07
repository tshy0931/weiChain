package tshy0931.com.github.weichain.network

import cats.syntax.validated._
import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.{FromEntityUnmarshaller, Unmarshal}
import akka.http.scaladsl.model.HttpMethods._
import akka.stream.ActorMaterializer
import tshy0931.com.github.weichain.module.BlockChainModule._
import tshy0931.com.github.weichain.module.ConfigurationModule._
import tshy0931.com.github.weichain.module.ValidationModule._
import tshy0931.com.github.weichain.module.MiningModule._
import tshy0931.com.github.weichain.module.NetworkModule._
import BlockHeaderValidation._
import TransactionValidation._

import scala.util.{Failure, Success}
import Protocol.Endpoints._
import akka.http.scaladsl.marshalling.{Marshal, ToEntityMarshaller}
import cats.data.Validated.Valid
import monix.eval.Task
import monix.execution.CancelableFuture
import tshy0931.com.github.weichain._
import tshy0931.com.github.weichain.database.{Database, MemPool}
import tshy0931.com.github.weichain.message._
import tshy0931.com.github.weichain.model.Block.BlockHeader
import tshy0931.com.github.weichain.model.{Block, Transaction}
import tshy0931.com.github.weichain.network.P2PClient.Mine
import tshy0931.com.github.weichain.network.Protocol.{BroadcastResponse, EndpointResponse, HeadersResponse, ResponseEnvelope}

import scala.concurrent.duration._

object P2PClient {

  def props = Props[P2PClient]

  case class Mine(blockToBe: Block)
}

class P2PClient extends Actor with ActorLogging {

  import tshy0931.com.github.weichain.codec.CodecModule._
  import tshy0931.com.github.weichain.database.Database._
  import monix.execution.Scheduler.Implicits.global

  implicit val system: ActorSystem = ActorSystem()
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  import monix.execution.Scheduler.Implicits.global

  val http = Http(context.system)

  override def receive: Receive = {

    case Mine(blockToBe) =>
      mine(blockToBe) flatMap { block =>
        MemPool[Address].getAll map {
          _ foreach { peer =>
            request[Block](block, peer.asUri(DOMAIN_DATA, BLOCK)) { BroadcastResponse(block, peer, _) }
          }
        }
      } runAsync
//      mine(blockToBe) runOnComplete {
//        case Success(block) => //TODO broadcast the block to peers immediately
//          MemPool[Address].getAll map {
//            _ foreach { peer =>
//              request[Block](block, peer.asUri(DOMAIN_DATA, BLOCK)) { BroadcastResponse(block, peer, _) }
//            }
//          } runAsync
//        case Failure(error) => log.error("Failed to mine block {}, error {}", blockToBe, error)
//      }

    case EndpointResponse(BLOCKS, peer, HttpResponse(StatusCodes.OK, headers, entity, _)) =>
      // TODO: store relative info to custom headers
//      parse[Block](entity) { block =>
//        //TODO: persist block
//      }

    case EndpointResponse(VERSION, peer, HttpResponse(StatusCodes.OK, _, entity, _)) =>
      parse[Version](entity) { remoteVersion =>
        if (remoteVersion.version > version) {
          log.warning("local version is not latest, remote version {}, local versoin {}", remoteVersion.version, version)
        }
        request[MessageHeader](MessageHeader(), peer.asUri(DOMAIN_CTRL, VERACK)) { EndpointResponse(VERACK, peer, _) }
      }

    case EndpointResponse(VERACK, peer, HttpResponse(StatusCodes.OK, _, entity, _)) =>
      // TODO: start IBD, use headers-first style
      entity.discardBytes(materializer)
      request[MessageHeader](MessageHeader(), peer.asUri(DOMAIN_CTRL, HEADERS)) { EndpointResponse(VERACK, peer, _) }

    case HeadersResponse(headersSentInRequest, HttpResponse(StatusCodes.OK, _, entity, _)) =>

      parse[Headers](entity) { case Headers(count, forkIndex, headers) =>
        Task.eval {
          // TODO: How to trigger async block downloads?
          val lastConfirmedHeader: BlockHeader = if(forkIndex == 0) {
            genesisBlock.value.header
          } else {
            headersSentInRequest(forkIndex - 1)
          }
          val a = count match {
            case 0 => ValidationError("no block header received from peer", headers).invalid
            case _ =>
              headers.foldLeft(lastConfirmedHeader.valid[ValidationError[BlockHeader]]) { (acc, curr) =>
                acc match {
                  case Valid(prev) => verifyBlockHeaders(prev, curr) runSyncUnsafe(30 seconds) //TODO - risky sync call, improve this
                  case invalid => invalid
                }
              }
          }
          // overwrite forked headers
          if (forkIndex < headers.size) {
            headers.takeRight(headers.size - forkIndex) map {
              Database[BlockHeader].save
            }
          }
        } runOnComplete {
          case Success(a) =>
          case Failure(err) =>
        }
      }
  }

  private[this] def parse[A](entity: ResponseEntity)(onSuccess: A => Unit)(implicit um: FromEntityUnmarshaller[A]) = {
    Unmarshal(entity).to[A] onComplete {
      case Success(item) => onSuccess(item)
      case Failure(err)  => log.error("failed parsing block. {}", err)
    }
  }

  private[this] def request[A](entity: A, endpoint: String)(envelope: HttpResponse => ResponseEnvelope)(implicit ma: ToEntityMarshaller[A]): CancelableFuture[Unit] = {
    Task.fromFuture(
      Marshal(entity).to[RequestEntity] flatMap { reqBody =>
        http.singleRequest(HttpRequest(POST, uri = endpoint, entity = reqBody))
      }
    ) map (a => self ! envelope(a)) runAsync
  }
}
