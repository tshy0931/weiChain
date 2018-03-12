package tshy0931.com.github.weichain.network

import cats.data.Validated
import tshy0931.com.github.weichain.model.Transaction
import Verification._

/**
  * This trait defines behaviours of how to verify a transaction
  */
trait Verification {

  def isValid(transaction: Transaction)(implicit newTransactions: Vector[Transaction]): Boolean
  def verify(transaction: Transaction)(implicit newTransactions: Vector[Transaction]): Validated[Error, Transaction]
}

object Verification {

  /**
    * 1. verify no double-spend.
    * 2. verify the same transaction is not already received.
    * 3. verify transaction signature.
    * 4. verify transaction is valid with current blockchain.
    * 5. script matches a whitelist (ensure script is valid).
    */
  final case object SimpleVerification extends Verification {

    override def isValid(transaction: Transaction)(implicit newTransactions: Vector[Transaction]): Boolean = ???

    override def verify(transaction: Transaction)(implicit newTransactions: Vector[Transaction]): Validated[Error, Transaction] = ???
  }

  case class Error(message: String, transaction: Transaction)
}