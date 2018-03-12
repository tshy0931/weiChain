package tshy0931.com.github.weichain.module

import cats.data.Validated.Invalid
import cats.syntax.validated._
import org.scalatest.{FlatSpec, GivenWhenThen, Inside, Matchers}
import tshy0931.com.github.weichain.SHA256DigestModule
import tshy0931.com.github.weichain.model.Script.{Error, PubKey, Sig}
import tshy0931.com.github.weichain.module.ScriptModule.SimpleScriptModule

class ScriptModuleSpec extends FlatSpec with ScriptModuleFixture with GivenWhenThen with Matchers with Inside {

  behavior of "P2PKH Scripts"

  it should "correctly handle a valid P2PKH script validation" in {

    run(Sig("pubkey").script) shouldBe "pubkey added".valid
    run(PubKey("pubkey").script) shouldBe "ok".valid
  }

  it should "fail to handle an invalid P2PKH script validation" in {

    run(Sig("pubkey").script) shouldBe "pubkey added".valid
    inside(run(PubKey("wrong_pubkey").script)) { case Invalid(Error(_, cmd, stack)) =>
      cmd shouldBe "OP_EQUALVERIFY"
    }
  }
}

trait ScriptModuleFixture extends SimpleScriptModule with SHA256DigestModule {


}
