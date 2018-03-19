package tshy0931.com.github.weichain.network

import Node._
import tshy0931.com.github.weichain.model.Transaction

/**
  * This defines the behaviours required by a node in the p2p blockchain network.
  */
class Node(val address: Address,
           var neighbors: Vector[Node],
           var inbox: Vector[Transaction])

object Node {

  /**
    * Seed DNS to contact when joining the P2P network
    */
  val seeds: Vector[String] = Vector(
    "192.168.0.1:8333"
  )

  case class Address()

  implicit class NodeOps(node: Node) extends Protocol {

    override def send[A](value: A): Unit = ???

    override def broadcastPeers: Unit = node.neighbors.foreach(n => send(node.neighbors.filterNot(_.address == n.address)))

    override def connect(nodes: Vector[Node]): Unit = ???

    override def disconnect: Unit = ???

    override def gossip(transaction: Transaction): Unit = ???
  }
}
