package tshy0931.com.github.weichain.module

import java.nio.charset.StandardCharsets

import com.google.common.hash.HashFunction
import com.google.common.hash.Hashing.sha256

object DigestModule {

  def digestor: HashFunction = sha256
  def digest(content: String): String = digestor.hashString(content, StandardCharsets.UTF_8).toString
  def merge(hash1: String, hash2: String): String = digestor.hashString(hash1+hash2, StandardCharsets.UTF_8).toString
}