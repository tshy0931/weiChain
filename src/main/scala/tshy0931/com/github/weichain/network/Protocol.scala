package tshy0931.com.github.weichain.network

import tshy0931.com.github.weichain.model.Transaction

/**
  * This trait defines behaviours required to perform network communications in the blockchain.
  */
trait Protocol {

  def disconnect
  def gossip(transaction: Transaction)
  def broadcastPeers
  def send[A](value: A)

  //TODO: Periodically check peer liveness
  // In order to maintain a connection with a peer,
  // nodes by default will send a message to peers before 30 minutes of inactivity.
  // If 90 minutes pass without a message being received by a peer,
  // the client will assume that connection has closed.
}
