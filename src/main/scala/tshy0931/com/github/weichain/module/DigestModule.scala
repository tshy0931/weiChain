package tshy0931.com.github.weichain.module

import java.security.MessageDigest
import tshy0931.com.github.weichain.Hash

object DigestModule {

  def digestor: MessageDigest = MessageDigest.getInstance("SHA-256")
  def digest(hash: Hash): Hash = digestor.digest(hash)
  def digest(content: String): Hash = digestor.digest(content.getBytes("UTF-8"))
  def merge(hash1:Hash, hash2:Hash): Hash = {
    val buffer = hash1.toBuffer
    buffer.append(hash2:_*)
    digestor.digest(buffer.toArray)
  }
}