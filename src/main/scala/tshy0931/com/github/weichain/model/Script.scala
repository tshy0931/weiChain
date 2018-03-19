package tshy0931.com.github.weichain.model

import tshy0931.com.github.weichain.{SHA256DigestModule, MockSignatureModule}

object Script extends SHA256DigestModule {

  case class Error(msg: String, cmd: String, stack: List[String])

  object P2PKH extends MockSignatureModule {

    def pubKey(pubKey: String) = {
      val pubKeyHash = digestor.digest(pubKey.getBytes("UTF-8")).map("%02x".format(_)).mkString
      s"OP_DUP OP_HASH160 $pubKeyHash OP_EQUALVERIFY OP_CHECKSIG"
    }

    def sig(secretKey: String, pubKey: String) = {
      val signature = sign(secretKey, pubKey)
      s"$signature $pubKey"
    }
  }

  object P2SH extends MockSignatureModule {

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

  object MultiSig extends MockSignatureModule {

    def pubKey(nSig: Int, pubKeys: Vector[String]) =
      s"OP_$nSig ${pubKeys.mkString(" ")} OP_${pubKeys.length} OP_CHECKMULTISIG"

    def sig(signatures: Vector[String]) =
      s"OP_0 ${signatures.mkString(" ")}"
  }
}


