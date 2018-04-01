package tshy0931.com.github.weichain.model

import tshy0931.com.github.weichain._
import module.DigestModule._
import module.SignatureModule._

object Script {

  case class Error(msg: String, cmd: String, stack: List[String])

  object P2PKH {

    def pubKey(pubKey: String) = {
      val pubKeyHash = digest(pubKey)
      s"OP_DUP OP_HASH160 ${pubKeyHash.asString} OP_EQUALVERIFY OP_CHECKSIG"
    }

    def sig(secretKey: String, pubKey: String) = {
      val signature = sign(secretKey, pubKey)
      s"$signature $pubKey"
    }
  }

  object P2SH {

    //TODO - how to distinguish between P2SH address and P2PKH address?

    def pubKey(redeemScriptHash: String) = {
      s"OP_HASH160 $redeemScriptHash OP_EQUAL"
    }

    def sig(signatures: Vector[String], redeemScript: String) = {
      s"${signatures.mkString(" ")} $redeemScript"
    }

    def redeemMultiSig(nSigs: Int, pubKeys: Vector[String]) =
      s"OP_$nSigs ${pubKeys.mkString(" ")} OP_${pubKeys.length} OP_CHECKMULTISIG"
  }

  object MultiSig {

    def pubKey(nSig: Int, pubKeys: Vector[String]) =
      s"OP_$nSig ${pubKeys.mkString(" ")} OP_${pubKeys.length} OP_CHECKMULTISIG"

    def sig(signatures: Vector[String]) =
      s"OP_0 ${signatures.mkString(" ")}"
  }
}


