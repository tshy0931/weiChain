package tshy0931.com.github

import java.security.MessageDigest

package object weichain {

  /**
    * https://en.bitcoin.it/wiki/Pay_to_script_hash
    * Pay to script hash (P2SH) transactions were standardised in BIP 16.
    * They allow transactions to be sent to a script hash (address starting with 3)
    * instead of a public key hash (addresses starting with 1).
    * To spend bitcoins sent via P2SH, the recipient must provide a script matching the script hash
    * and data which makes the script evaluate to true.
    * @param value
    */
  class Pay2ScriptHash(val value: String) extends AnyVal {
    //TODO: to understand and implement P2SH
  }

  trait DigestModule {
    def digestor: MessageDigest
  }
  trait SHA256DigestModule extends DigestModule {
    override def digestor: MessageDigest = MessageDigest.getInstance("SHA-256")
  }
}
