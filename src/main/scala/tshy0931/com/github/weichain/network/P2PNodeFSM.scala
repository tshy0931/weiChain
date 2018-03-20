package tshy0931.com.github.weichain.network

import akka.actor.{ActorLogging, FSM, Props, Stash}
import tshy0931.com.github.weichain.network.P2PNodeFSM._
import scala.concurrent.duration._

object P2PNodeFSM {

  sealed trait NodeState
  case object Disconnected extends NodeState
  case object Connecting extends NodeState
  case object Connected extends NodeState
  case object InitialBlockDownload extends NodeState
  case object ListeningToBroadcast extends NodeState
  case object Mining extends NodeState

  sealed trait Data
  case object Uninitialized extends Data
  case class Connection(host: String, port: Int) extends Data
  case class OpenConnections(conns: Set[(String, Int)]) extends Data

  sealed trait Command
  case class Connect(host: String, port: Int) extends Command
  case class DisConnect(host: String, port: Int) extends Command


  val props = Props[P2PNodeFSM]
}

class P2PNodeFSM extends FSM[NodeState, Data] with ActorLogging with Stash {

  startWith(Disconnected, Uninitialized)

  when(Disconnected) {
    case Event(Connect(host, port), _) =>
      goto(Connecting) using Connection(host, port) forMax(30 seconds)
    case _ =>
      stay
  }

//  when(Connected) {
//    case Event(DisConnect(host, port), OpenConnections(conns)) =>
//      if(conns.contains((host, port))){
//        //TODO: close connection to host:port
//      }
//  }
}
