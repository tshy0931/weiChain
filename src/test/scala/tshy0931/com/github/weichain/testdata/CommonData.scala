package tshy0931.com.github.weichain.testdata

import tshy0931.com.github.weichain._
import tshy0931.com.github.weichain.module.DigestModule.digest

trait CommonData {

  val testHash: Hash = ""

  def testAddress(no: Int): Hash = digest(s"testAddress$no")
}
