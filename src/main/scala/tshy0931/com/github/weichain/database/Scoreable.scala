package tshy0931.com.github.weichain.database

import shapeless.the
import tshy0931.com.github.weichain.model.Transaction

trait Scoreable[A] {

  def score(a: A): Long
}

object Scoreable {

  def apply[A: Scoreable] = the[Scoreable[A]]

  implicit val txScoreable: Scoreable[Transaction] = _.createTime

  implicit class ScoreableOps[A: Scoreable](a: A) {

    def score: Long = Scoreable[A].score(a)
  }
}