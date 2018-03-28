package tshy0931.com.github.weichain.module

import java.util.concurrent.ConcurrentHashMap

import akka.http.scaladsl.model.HttpResponse
import com.google.common.base.Charsets
import shapeless._
import com.google.common.hash.{BloomFilter, Funnel, PrimitiveSink}
import tshy0931.com.github.weichain.model.Transaction
import tshy0931.com.github.weichain._
import tshy0931.com.github.weichain.message.{FilterAdd, FilterLoad}
import tshy0931.com.github.weichain.network.Address

import scala.collection.JavaConverters._
import scala.collection.concurrent

/**
  * This module implements Bloom Filters that can be used for
  * - SPV
  * - listen to specific transactions, e.g. ones for a specific wallet
  */
object FilterModule {

  trait FilterPool[T] {

    val pool: concurrent.Map[String, BloomFilter[T]] = new ConcurrentHashMap[String, BloomFilter[T]]().asScala

    def add(owner: String, filter: BloomFilter[T]): Boolean = {
      pool.put(owner, filter)
      pool.contains(owner)
    }

    def get(owner: String): Option[BloomFilter[T]] = pool.get(owner)

    def delete(owner: String): Boolean = {
      pool.remove(owner)
      !pool.contains(owner)
    }
  }

  val activeTxFilters: concurrent.Map[String, BloomFilter[Transaction]] = new ConcurrentHashMap[String, BloomFilter[Transaction]]().asScala
  val activeAddrFilters: concurrent.Map[String, BloomFilter[Address]] = new ConcurrentHashMap[String, BloomFilter[Address]]().asScala

  implicit lazy val txFunnel: Funnel[Transaction] = (from: Transaction, into: PrimitiveSink) => {
    into
      .putString(from.hash.asString, Charsets.UTF_8)
      .putInt(from.version)
  }

  implicit lazy val addressFunnel: Funnel[Address] = (from: Address, into: PrimitiveSink) => {
    into
      .putString(from.host, Charsets.UTF_8)
      .putInt(from.port)
  }

  def newFilter[A](owner: String,
                   expectedInsertions: Int,
                   falsePositiveRate: Double = 0.03): BloomFilter[A] = {

    val filter = BloomFilter.create(the[Funnel[A]], expectedInsertions, falsePositiveRate)
    the[FilterPool[A]].add(owner, filter)
    filter
  }

  def getFilter[A](owner: String): Option[BloomFilter[A]] = {
    the[FilterPool[A]].get(owner)
  }

  def addToFilter[A](message: FilterAdd[A]): HttpResponse = message match {
    case FilterAdd(owner, inserts) =>
      for {
        filter <- getFilter[A](owner)
        item <- inserts
      } yield item -> filter.put(item)

      HttpResponse(200)
  }

  def loadFilter[A](message: FilterLoad[A]): HttpResponse = message match {
    case FilterLoad(owner, initInserts, expectedInserts, falsePosRate) =>
      val filter: BloomFilter[A] = newFilter(owner, expectedInserts, falsePosRate.getOrElse(0.03))
      val failures: List[A] = initInserts.foldLeft(List.empty[A]) { (failed, item) =>
        if(!filter.put(item)){
          item :: failed
        } else {
          failed
        }
      }
      if(failures.nonEmpty) {
        HttpResponse(500, entity =
          s"""Transactions below failed to be inserted to bloomfilter:
             |${failures mkString "\n"}
             |""".stripMargin
        )
      } else {
        HttpResponse(200, entity = s"Tx successfully added to filter for $owner")
      }
    }

  def deleteFilter[A](owner: String): HttpResponse = {
    val ok: Boolean = the[FilterPool[A]].delete(owner)
    if(ok) HttpResponse(200, entity = s"filter deleted for $owner")
    else HttpResponse(500, entity = s"failed to delete filter for $owner")
  }

  implicit class FilterOps[T](filter: BloomFilter[T]) {

    def delete(owner: String): Boolean = the[FilterPool[T]].delete(owner)
  }
}
