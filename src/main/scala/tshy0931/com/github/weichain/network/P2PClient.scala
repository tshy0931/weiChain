package tshy0931.com.github.weichain.network

import cats.syntax.all._
import akka.actor.{Actor, ActorLogging, ActorSystem, Props, Scheduler}
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
import cats.data.Validated
import cats.data.Validated.{Invalid, Valid}
import monix.eval.Task
import monix.execution.CancelableFuture
import tshy0931.com.github.weichain._
import tshy0931.com.github.weichain.database.{Database, MemPool}
import tshy0931.com.github.weichain.message._
import tshy0931.com.github.weichain.model.Block.{BlockBody, BlockHeader}
import tshy0931.com.github.weichain.model.{Address, Block, Chain}
import tshy0931.com.github.weichain.network.P2PClient.Mine
import tshy0931.com.github.weichain.network.Protocol.{BroadcastResponse, EndpointResponse, HeadersResponse, ResponseEnvelope}

import scala.concurrent.duration._

object P2PClient {

  def props = Props[P2PClient]

  case class Mine(prevBlockHeader: BlockHeader,
                  rewardAddr: Hash,
                  pubKeyScript: String,
                  coinbaseScript: String)
}

class P2PClient extends Actor with ActorLogging {

  import tshy0931.com.github.weichain.codec.CodecModule._
  import tshy0931.com.github.weichain.database.Database._
  import monix.execution.Scheduler.Implicits.global

  implicit val system: ActorSystem = ActorSystem()
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val scheduler: Scheduler = system.scheduler

  val http = Http(context.system)

  override def preStart(): Unit = {
    log.info("starting P2P Client")
    scheduler.schedule(miningRate, miningRate){
      latestHeader map { self ! Mine(_, rewardAddr, minerPubKeyScript, minerCoinbaseScript) } runAsync
    }
    super.preStart()
  }

  override def postStop(): Unit = {
    log.info("stopping P2P Client")
    super.postStop()
  }

  override def receive: Receive = {

    case Mine(prevBlockHeader, rewardAddr, minerPubKeyScript, minerCoinbaseScript) =>
      log.info("start mining block after header {}", prevBlockHeader.hash)
      mine(prevBlockHeader)(rewardAddr, minerPubKeyScript, minerCoinbaseScript) flatMap { block =>
        (Database[BlockHeader].save(block.header), Database[BlockBody].save(block.body), broadcast(block)) parMapN {
          (headerSaved, bodySaved, _) => log.info("block {} mined, saved and broadcasted", block.header.hash)
        }
      } runAsync

    case EndpointResponse(BLOCKS, peer, HttpResponse(StatusCodes.OK, headers, entity, _)) =>
      // TODO: store relative info to custom headers
      parse[Block](entity) { case Block(header, body) =>
        (Database[BlockHeader].save(header), Database[BlockBody].save(body)) parMapN {
          (headerOk, bodyOk) =>
            if(!headerOk) log.error("Database save failed on block header {}", header)
            if(!bodyOk)   log.error("Database save failed on block body {}", body)
        }
      }

    case EndpointResponse(VERSION, peer, HttpResponse(StatusCodes.OK, _, entity, _)) =>
      parse[Version](entity) { remoteVersion =>
        if (remoteVersion.version > version) {
          log.warning("local version is not latest, remote version {}, local versoin {}", remoteVersion.version, version)
        }
        log.debug("received version response {}, sending verack to {}:{}", remoteVersion, peer.host, peer.port)
        request[MessageHeader](MessageHeader(), peer.asUri(DOMAIN_CTRL, VERACK)) { EndpointResponse(VERACK, peer, _) }
      }

    case EndpointResponse(VERACK, peer, HttpResponse(StatusCodes.OK, _, entity, _)) =>
      // TODO: start IBD, use headers-first style
      entity.discardBytes(materializer)
      log.debug("received verack response from {}:{}, sending verack back", peer.host, peer.port)
      request[MessageHeader](MessageHeader(), peer.asUri(DOMAIN_CTRL, HEADERS)) { EndpointResponse(VERACK, peer, _) }

    case HeadersResponse(headersSentInRequest, peer, HttpResponse(StatusCodes.OK, _, entity, _)) =>

      parse[Headers](entity) { case Headers(count, forkIndex, headers) =>
        Task {
          // TODO: How to trigger async block downloads?
          // TODO: Schedule mining after IBD is done
          val lastConfirmedHeader: BlockHeader = if(forkIndex == 0) {
            genesisBlock.header
          } else {
            headersSentInRequest(forkIndex - 1)
          }
          count match {
            case 0 => ValidationError("no block header received from peer", headers).invalid
            case _ =>
              val validation: Validated[ValidationError[BlockHeader], BlockHeader] =
                headers.foldLeft(lastConfirmedHeader.valid[ValidationError[BlockHeader]]) { (acc, curr) =>
                  acc match {
                    case Valid(prev) => verifyBlockHeaders(prev, curr) runSyncUnsafe(30 seconds) //TODO - risky sync call, improve this
                    case invalid     => invalid
                  }
                }
              validation match {
                case Invalid(err) => log.error("invalid headers in headers response from peer {}, error {}", peer, err)
                case Valid(_)     => Chain[BlockHeader].update(headers, forkIndex)
              }
          }
        } onErrorRecover{
          case error => log.error("Error when updating forked block headers in database, {}", error)
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

  private[this] def broadcast(block: Block): Task[Unit] = {
    MemPool[Address].getAll map {
      _ foreach { peer =>
        request[Block](block, peer.asUri(DOMAIN_DATA, BLOCK)) { BroadcastResponse(block, peer, _) }
      }
    }
  }
}
