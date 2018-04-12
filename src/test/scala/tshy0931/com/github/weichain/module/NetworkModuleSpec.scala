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
import monix.eval.Task
import tshy0931.com.github.weichain.database.Database
import tshy0931.com.github.weichain.model.Block.{BlockBody, BlockHeader}
import org.mockito.Mockito._
import org.mockito.Matchers._
import org.scalatest.mockito.MockitoSugar
import tshy0931.com.github.weichain.testdata.{BlockTestData, CommonData, TransactionTestData}

class NetworkModuleSpec extends FlatSpec
  with Matchers with ScalatestRouteTest with MockitoSugar
  with BlockTestData with TransactionTestData with CommonData {

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

  it should "respond with block header upon a /test/header/:hash request" in {
    implicit val headerDB: Database[BlockHeader] = mock[Database[BlockHeader]]
    when(headerDB.find("blkhdr:00b98ad248c8de87e6dcebe0cac0894f7eaa92cbc5e52c6f84a32ebbc58c947b")).thenReturn(Task.now(Some(blk1.header)))
    Get("/test/header/00b98ad248c8de87e6dcebe0cac0894f7eaa92cbc5e52c6f84a32ebbc58c947b") ~> routes ~> check {
      val resp = responseAs[BlockHeader]
      resp.hash shouldBe "00b98ad248c8de87e6dcebe0cac0894f7eaa92cbc5e52c6f84a32ebbc58c947b"
    }
  }

  it should "respond with block body upon a /test/block/:hash request" in {
    implicit val headerDB: Database[BlockBody] = mock[Database[BlockBody]]
    when(headerDB.find("blkbdy:00b98ad248c8de87e6dcebe0cac0894f7eaa92cbc5e52c6f84a32ebbc58c947b")).thenReturn(Task.now(Some(blk1.body)))
    Get("/test/block/00b98ad248c8de87e6dcebe0cac0894f7eaa92cbc5e52c6f84a32ebbc58c947b") ~> routes ~> check {
      val resp = responseAs[BlockBody]
      resp.headerHash shouldBe "00b98ad248c8de87e6dcebe0cac0894f7eaa92cbc5e52c6f84a32ebbc58c947b"
    }
  }
}
