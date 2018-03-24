package tshy0931.com.github.weichain.module

import cats.data.Validated
import cats.data.Validated.{Invalid, Valid}
import cats.syntax.validated._
import tshy0931.com.github.weichain.model.Script.Error

import SignatureModule._
import DigestModule._

object ScriptModule {

  protected var stack: List[String] = Nil

  /**
    * run a given script
    * @param script
    * @return Error if the script execution failed, otherwise a return value
    */
  def run(script: String): Validated[Error, String] = script.split(" ").foldLeft[Validated[Error, String]]("ok".valid) {
    case (err: Invalid[Error], _) => err
    case (_: Valid[String], cmd) => exec(cmd)
  }

  def exec: PartialFunction[String, Validated[Error, String]] = {

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
        val hash = digest(head.getBytes("UTF-8")).map("%02x".format(_)).mkString
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

    /** MultiSig Operations **/

    case "OP_CHECKMULTISIG" =>
      stack.headOption.fold[Validated[Error, String]](Error("stack is empty", "OP_CHECKMULTISIG", stack).invalid){ head =>
        val nKey = head.toInt
        val pubKeys: List[String] = stack.tail.take(nKey)
        stack = stack.drop(nKey+1)
        val nSig = stack.head.toInt
        val signatures: List[String] = stack.tail.take(nSig+1)
        stack = head :: stack
        val isValid = signatures.forall(sig => pubKeys.exists(key => sign(key, key) == sig))
        stack = stack.drop(nSig+2)
        if(isValid) "ok".valid else Error("Multisig failed", "OP_CHECKMULTISIG", stack).invalid
      }

    case "OP_0" =>
      "ok".valid

    case "OP_1" =>
      stack = "1" :: stack
      "ok".valid

    case "OP_2" =>
      stack = "2" :: stack
      "ok".valid

    case "OP_3" =>
      stack = "3" :: stack
      "ok".valid

    case "OP_4" =>
      stack = "4" :: stack
      "ok".valid

    case "OP_5" =>
      stack = "5" :: stack
      "ok".valid

    case "OP_6" =>
      stack = "6" :: stack
      "ok".valid

    case "OP_7" =>
      stack = "7" :: stack
      "ok".valid

    case "OP_8" =>
      stack = "8" :: stack
      "ok".valid

    case "OP_9" =>
      stack = "9" :: stack
      "ok".valid

    case "OP_10" =>
      stack = "10" :: stack
      "ok".valid

    case "OP_11" =>
      stack = "11" :: stack
      "ok".valid

    case "OP_12" =>
      stack = "12" :: stack
      "ok".valid

    case "OP_13" =>
      stack = "13" :: stack
      "ok".valid

    case "OP_14" =>
      stack = "14" :: stack
      "ok".valid

    case "OP_15" =>
      stack = "15" :: stack
      "ok".valid

    case "OP_16" =>
      stack = "16" :: stack
      "ok".valid

    /** value to push to stack **/

    case value =>
      stack = value :: stack
      s"$value added".valid



//    case err => Error(s"undefined command: $err", err, stack).asLeft

  }
}
