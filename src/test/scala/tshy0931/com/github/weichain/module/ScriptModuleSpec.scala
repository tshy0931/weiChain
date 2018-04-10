package tshy0931.com.github.weichain.module

import cats.data.Validated.Invalid
import cats.syntax.validated._
import org.scalatest.{FlatSpec, GivenWhenThen, Inside, Matchers}
import tshy0931.com.github.weichain.model.Script._

class ScriptModuleSpec extends FlatSpec with GivenWhenThen with Matchers with Inside {

  behavior of "P2PKH scripts"

  val (secretKey, publicKey) = ("testSecretKey", "testPublicKey")

  it should "correctly handle a valid P2PKH script validation" in {

    ScriptModule.run(P2PKH.sig(secretKey, publicKey)) shouldBe s"$publicKey added".valid
    ScriptModule.run(P2PKH.pubKey(publicKey)) shouldBe "ok".valid
  }

  it should "fail to handle an invalid P2PKH script validation" in {
    //TODO: implement test when signature module is implemented
    ScriptModule.run(P2PKH.sig(secretKey, publicKey)) shouldBe s"$publicKey added".valid
    inside(ScriptModule.run(P2PKH.pubKey(publicKey))) { case Invalid(Error(_, cmd, _)) =>
      cmd shouldBe "OP_EQUALVERIFY"
    }
  }

  behavior of "P2SH scripts"

  it should "successfully verify script hash and execute the redeem script" in {
    //TODO
    fail()
  }

  it should "fail validation on invalid redeem script and NOT execute the script" in {
    //TODO
    fail()
  }

  behavior of "MultiSig scripts"

  it should "successfully verify the transaction when sufficient correct signatures are provided" in {
    //TODO blocked by signature module not implemented
//    run(MultiSig.sig(Vector(sign("pk1","pk1"), sign("pk2","pk2")))) shouldBe s"pk2pk2 added".valid
//    run(MultiSig.pubKey(2, Vector("pk1","pk2","pk3"))) shouldBe "ok".valid
  }
}
