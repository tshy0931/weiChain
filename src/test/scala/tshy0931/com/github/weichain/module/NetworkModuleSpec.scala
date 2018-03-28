package tshy0931.com.github.weichain.module

import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.http.scaladsl.testkit._
import akka.http.scaladsl.server._
import akka.testkit.TestProbe
import org.scalatest.{FlatSpec, Inside, Matchers}
import tshy0931.com.github.weichain.message.Version
import NetworkModule._
import akka.http.scaladsl.model._

class NetworkModuleSpec extends FlatSpec with Matchers with Inside with ScalatestRouteTest with RouteTestResultComponent {

  behavior of "data API"

  it should "respond with version message upon receiving a version request" in {

    val testProbe = TestProbe()
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

//    Post("control/version", HttpEntity(ContentTypes.`application/json`, payload)) ~> Route.seal(controlRoute) ~> check {
//      responseAs[Version] shouldBe version
//    }
  }

}

trait NetworkModuleFixture {

}