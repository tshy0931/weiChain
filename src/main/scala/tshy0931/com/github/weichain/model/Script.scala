package tshy0931.com.github.weichain.model

import tshy0931.com.github.weichain.SHA256DigestModule

trait Script {

  def script: String
}

object Script extends SHA256DigestModule {

  case class Error(msg: String, cmd: String, stack: List[String])

  case class PubKey(pubKey: String) extends Script {

    override def script: String = {
      val pubKeyHash = digestor.digest(pubKey.getBytes("UTF-8")).map("%02x".format(_)).mkString
      s"OP_DUP OP_HASH160 $pubKeyHash OP_EQUALVERIFY OP_CHECKSIG"
    }
  }

  case class Sig(pubKey: String) extends Script {
    override def script: String = {
      // TODO: implement secp256k1 signature
      val signature = s"sig_of_$pubKey"
      s"$signature $pubKey"
    }
  }
}


