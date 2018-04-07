package tshy0931.com.github.weichain.module

import java.net.InetAddress

import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.http.scaladsl.model.headers._
import org.scalatest.{FlatSpec, Matchers}
import tshy0931.com.github.weichain.message.Version
import tshy0931.com.github.weichain.codec.CodecModule._
import NetworkModule.Routes._
import akka.http.scaladsl.model._
import cats.syntax.all._

class NetworkModuleSpec extends FlatSpec with Matchers with ScalatestRouteTest {

  behavior of "data API"

  it should "respond with 'pong' upon receiving a ping request" in {
    Get("/control/ping") ~> controlRoute ~> check {
      responseAs[String] shouldEqual "pong"
    }
  }

  it should "respond with version message upon receiving a version request" in {

    val payload: String =
      """{
        |   "version": 1,
        |   "services": 1,
        |   "timestamp": 1,
        |   "nonce": 1,
        |   "startHeight": 1,
        |   "relay": true
        |}""".stripMargin
    val remoteAddress: ModeledHeader = `Remote-Address`(RemoteAddress(InetAddress.getByName("localhost"), 9090.some))

    Post("/control/version", HttpEntity(ContentTypes.`application/json`, payload)).withHeaders(remoteAddress) ~> controlRoute ~> check {
      val resp = responseAs[Version]
      resp.version shouldBe 1
      resp.services shouldBe 1
      resp.startHeight shouldBe 1
      resp.relay shouldBe true
    }
  }


}
