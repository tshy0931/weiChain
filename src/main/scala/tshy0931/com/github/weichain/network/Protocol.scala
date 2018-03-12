package tshy0931.com.github.weichain.network

import tshy0931.com.github.weichain.model.Transaction

/**
  * This trait defines behaviours required to perform network communications in the blockchain.
  */
trait Protocol {

  def connect(nodes: Vector[Node])
  def disconnect
  def gossip(transaction: Transaction)
}
