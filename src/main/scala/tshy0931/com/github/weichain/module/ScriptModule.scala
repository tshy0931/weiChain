package tshy0931.com.github.weichain.module

import cats.data.Validated
import cats.data.Validated.{Invalid, Valid}
import cats.syntax.validated._
import tshy0931.com.github.weichain.model.Script.Error
import tshy0931.com.github.weichain.{DigestModule, SHA256DigestModule}

trait ScriptModule {

  /**
    * a partial function that defines supported instructions.
    * @return
    */
  def exec: PartialFunction[String, Validated[Error, String]]

  /**
    * run a given script
    * @param script
    * @return Error if the script execution failed, otherwise a return value
    */
  def run(script: String): Validated[Error, String] = script.split(" ").foldLeft[Validated[Error, String]]("ok".valid) {
    case (err: Invalid[Error], _) => err
    case (_: Valid[String], cmd) => exec(cmd)
  }

  protected var stack: List[String] = Nil
}

object ScriptModule extends SHA256DigestModule {


  trait SimpleScriptModule extends ScriptModule { this: DigestModule =>

    override def exec: PartialFunction[String, Validated[Error, String]] = {

      case "OP_EQUAL" =>
        stack match {
          case (v1:String) :: (v2:String) :: tail =>
            stack = tail
            (v1 == v2).toString.valid

          case _ =>
            Error(s"not enough elements to compare", "OP_EQUAL", stack).invalid
        }

      case "OP_DUP" =>
        stack.headOption.fold[Validated[Error, String]](Error("No element to duplicate, stack is empty", "OP_DUP", stack).invalid){ head =>
          stack = head :: stack
          "ok".valid
        }

      case "OP_HASH160" =>
        stack.headOption.fold[Validated[Error, String]](Error("No element to HASH160, stack is empty", "OP_HASH160", stack).invalid){ head =>
          val hash = digestor.digest(head.getBytes("UTF-8")).map("%02x".format(_)).mkString
          stack = hash :: stack.tail
          "ok".valid
        }

      case "OP_EQUALVERIFY" =>
        stack match {
          case (v1:String) :: (v2:String) :: tail =>
            stack = tail
            if(v1 == v2) true.toString.valid else Error(s"$v1 not equal to $v2", "OP_EQUALVERIFY", stack).invalid

          case _ =>
            Error(s"not enough elements to compare", "OP_EQUALVERIFY", stack).invalid
        }

      case "OP_CHECKSIG" => // TODO - implement check sig
        stack match {
          case key :: sig :: tail =>
            stack = tail
            if(sig == s"sig_of_$key"){
              "ok".valid
            } else {
              Error("Signature is invalid", "OP_CHECKSIG", stack).invalid
            }

          case _ => Error("not enough elements", "OP_CHECKSIG", stack).invalid
        }

      case "CLEAR" =>
        stack = Nil
        "ok".valid

      case value =>
        stack = value :: stack
        s"$value added".valid

//      case err => Error(s"undefined command: $err", err, stack).asLeft
    }
  }
}
