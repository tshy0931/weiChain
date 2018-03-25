package tshy0931.com.github.weichain.module

import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.http.scaladsl.server._
import Directives._
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatest.{FlatSpec, GivenWhenThen, Inside, Matchers}
import tshy0931.com.github.weichain.message.Version
import tshy0931.com.github.weichain.codec.CodecModule._

class NetworkModuleSpec extends FlatSpec with GivenWhenThen with Matchers with Inside with ScalatestRouteTest {

  import NetworkModule._

  behavior of "data API"

  it should "respond with version message upon receiving a version request" in {

    val version = Version(1, 0L, 0L, 0L, 0L, true)
    val payload =
      """{
        |   "version": 1,
        |   "services": 1,
        |   "timestamp": 1,
        |   "startHeight": 1,
        |   "nonce": 1,
        |   "relay": 1
        |}
        |""".stripMargin

//    Post("control/version", HttpEntity(ContentTypes.`application/json`, payload)) ~> controlRoute ~> check {
//      responseAs[Version] shouldEqual version
//    }
  }

}

trait NetworkModuleFixture extends TableDrivenPropertyChecks {

}