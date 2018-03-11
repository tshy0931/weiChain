package tshy0931.com.github.weichain.model

import java.security.MessageDigest

import cats.syntax.either._
import tshy0931.com.github.weichain.{DigestModule, SHA256DigestModule, ScriptPubKey, ScriptSig}

/**
  * Stack-based simple scripting language for
  * a) compute hash
  * b) compute signatures
  * c) verify signatures
  * @tparam A Type that supports above behaviours
  */
trait Script[A] {
  this: DigestModule =>

  import Script._

  private lazy val commands: String => List[String] = _ split " " toList
  protected var stack: List[String] = Nil
  def digestor: MessageDigest

  def eval: PartialFunction[String, Either[Error, String]]
  def exec(input: String): Either[Error, String] = commands(input).foldLeft("".asRight[Error]){
    case (err: Left[Error, String], _) => err
    case (_, cmd) => eval(cmd) orElse Error(s"Failed to execute command $cmd").asLeft
  }
}

object Script {

  case class Error(msg: String)

  implicit val scriptSig = new Script[ScriptSig] with SHA256DigestModule {

    def eval = {
      case "command A" => ???
      case "command B" => ???
    }
  }

  implicit val scriptPubKey = new Script[ScriptPubKey] with SHA256DigestModule {

    def eval = {
      case "OP_EQUAL" =>
        stack match {
          case (v1:String) :: (v2:String) :: _ =>
            (if(v1 == v2) "ok" else "nok").asRight[Error]
          case _ =>
            Error(s"not enough elements to compare, stack: $stack").asLeft[String]
        }

      case "OP_DUP" =>
        stack.headOption.fold(Error("No element to duplicate, stack is empty").asLeft[String]){ head =>
            stack = head :: stack
          "ok".asRight[Error]
        }

      case "OP_HASH160" =>
        stack.headOption.fold(Error("No element to HASH160, stack is empty").asLeft[String]){ head =>
          val hash = digestor.digest(head.getBytes("UTF-8")).map("%02x".format(_)).mkString
          stack = hash :: stack.tail
          "ok".asRight[Error]
        }

      case "OP_CHECKSIG" => ???
      case sig if isSig(sig) => ???
      case pubKey if isPubKey(pubKey) => ???
      case err => Error(s"undefined command $err").asLeft
    }

    private def isSig(cmd: String) = true
    private def isPubKey(cmd: String) = true
  }
}


