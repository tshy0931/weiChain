package tshy0931.com.github.weichain.network

import akka.http.scaladsl.model.HttpResponse
import tshy0931.com.github.weichain.model.Block.BlockHeader
import tshy0931.com.github.weichain.model.Transaction

/**
  * This trait defines behaviours required to perform network communications in the blockchain.
  */
object Protocol {

  sealed trait ResponseEnvelope
  final case class EndpointResponse(endpoint: String, peer: Address, response: HttpResponse) extends ResponseEnvelope
  final case class HeadersResponse(headersSentInRequest: Vector[BlockHeader], response: HttpResponse) extends ResponseEnvelope
  final case class TxResponse(peer: Address, response: HttpResponse) extends ResponseEnvelope

  object Endpoints {

    val DOMAIN_DATA = "data"
    val BLOCK = "block"
    val BLOCKS = "blocks"
    val TX = "tx"
    val HEADERS = "headers"
    val MERKLEBLOCK = "merkleblock"

    val DOMAIN_CTRL = "control"
    val ADDRESS = "addr"
    val FILTERADD = "filteradd"
    val FILTERCLEAR = "filterclear"
    val FILTERLOAD = "filterload"
    val PING = "ping"
    val PONG = "pong"
    val SENDHEADERS = "sendheaders"
    val VERACK = "verack"
    val VERSION = "version"
  }

  //TODO: Periodically check peer liveness
  // In order to maintain a connection with a peer,
  // nodes by default will send a message to peers before 30 minutes of inactivity.
  // If 90 minutes pass without a message being received by a peer,
  // the client will assume that connection has closed.
}
