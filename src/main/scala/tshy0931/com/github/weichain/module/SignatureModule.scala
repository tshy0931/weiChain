package tshy0931.com.github.weichain.module

import java.security.Signature

object SignatureModule {

  val algorithm: String = "SHA1withRSA"

  lazy val signature = Signature.getInstance(algorithm)

  def sign(secretKey: String, document: String): String = {
    s"sig_of_$document"
    //      val spec = new PKCS8EncodedKeySpec(secretKey.toHash)
    //      val key = KeyFactory.getInstance("RSA").generatePrivate(spec)
    //      signature.initSign(key)
    //      signature.update(document.toHash)
    //      signature.sign
  }
}
