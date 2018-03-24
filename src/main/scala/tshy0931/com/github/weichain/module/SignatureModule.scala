package tshy0931.com.github.weichain.module

import java.security.Signature

object SignatureModule {

  val algorithm: String = "SHA1withRSA"

  lazy val signature = Signature.getInstance(algorithm)

  def sign(secretKey: String, document: String): String = {
    s"sig_of_$document"
    //      val spec = new PKCS8EncodedKeySpec(secretKey.getBytes("UTF-8"))
    //      val key = KeyFactory.getInstance("RSA").generatePrivate(spec)
    //      signature.initSign(key)
    //      signature.update(document.getBytes("UTF-8"))
    //      signature.sign.map("%02x".format(_)).mkString
  }
}
