package tshy0931.com.github.weichain.module

import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest.{FlatSpec, Matchers}
import tshy0931.com.github.weichain.message.Version
import tshy0931.com.github.weichain.codec.CodecModule._
import NetworkModule._
import Routes._
import akka.http.scaladsl.model._

class NetworkModuleSpec extends FlatSpec with Matchers with ScalatestRouteTest {

  behavior of "data API"

  it should "respond with version message upon receiving a version request" in {

    val version = Version(1, 0L, 0L, 0L, 0L, true)
    val payload: String =
      """{
        |   "version": 1,
        |   "services": 1,
        |   "timestamp": 1,
        |   "startHeight": 1,
        |   "nonce": 1,
        |   "relay": 1
        |}
        |""".stripMargin

//    Post("control/version", HttpEntity(ContentTypes.`application/json`, payload)) ~> routes ~> check {
//      responseAs[Version] shouldBe version
//    }
  }

}
