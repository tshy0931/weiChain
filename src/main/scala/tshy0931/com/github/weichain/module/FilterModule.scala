package tshy0931.com.github.weichain.module

import java.util.concurrent.ConcurrentHashMap

import akka.http.scaladsl.model.HttpResponse
import com.google.common.base.Charsets
import shapeless.the
import com.google.common.hash.{BloomFilter, Funnel, PrimitiveSink}
import tshy0931.com.github.weichain.model.{Address, Transaction}
import tshy0931.com.github.weichain._
import tshy0931.com.github.weichain.message.{FilterAdd, FilterLoad}

import scala.collection.JavaConverters._
import scala.collection.concurrent

/**
  * This module implements Bloom Filters that can be used for
  * - SPV
  * - listen to specific transactions, e.g. ones for a specific wallet
  */
object FilterModule {

  case class FilterPool[T]() {

    private val pool: concurrent.Map[String, BloomFilter[T]] = new ConcurrentHashMap[String, BloomFilter[T]]().asScala

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

  def apply[A: FilterPool] = the[FilterPool[A]]

  implicit val txFilterPool: FilterPool[Transaction] = FilterPool[Transaction]
  implicit val addrFilterPool: FilterPool[Address] = FilterPool[Address]

  val activeTxFilters: concurrent.Map[String, BloomFilter[Transaction]] = new ConcurrentHashMap[String, BloomFilter[Transaction]]().asScala
  val activeAddrFilters: concurrent.Map[String, BloomFilter[Address]] = new ConcurrentHashMap[String, BloomFilter[Address]]().asScala

  implicit val txFunnel: Funnel[Transaction] = (from: Transaction, into: PrimitiveSink) => {
    into
      .putString(from.hash.asString, Charsets.UTF_8)
      .putInt(from.version)
  }

  implicit val addressFunnel: Funnel[Address] = (from: Address, into: PrimitiveSink) => {
    into
      .putString(from.host, Charsets.UTF_8)
      .putInt(from.port)
  }

  def newTxFilter(owner: String,
                  expectedInsertions: Int,
                  falsePositiveRate: Double = 0.03): BloomFilter[Transaction] = {

    val filter = BloomFilter.create[Transaction](txFunnel, expectedInsertions, falsePositiveRate)
    FilterPool[Transaction].add(owner, filter)
    filter
  }

  def getTxFilter(owner: String): Option[BloomFilter[Transaction]] = {
    FilterPool[Transaction].get(owner)
  }

  def addToFilter(message: FilterAdd): HttpResponse = message match {
    case FilterAdd(owner, inserts) =>
      getTxFilter(owner) map { filter => inserts map filter.put }
      HttpResponse(200, entity = "items added to filter")
  }

  def loadFilter(message: FilterLoad): HttpResponse = message match {
    case FilterLoad(owner, initInserts, expectedInserts, falsePosRate) =>
      val filter: BloomFilter[Transaction] = newTxFilter(owner, expectedInserts, falsePosRate.getOrElse(0.03))
      val failures: List[Transaction] = initInserts.foldLeft(List.empty[Transaction]) { (failed, item) =>
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

  def deleteTxFilter(owner: String): HttpResponse = {
    val ok: Boolean = FilterPool[Transaction].delete(owner)
    if(ok) HttpResponse(200, entity = s"filter deleted for $owner")
    else HttpResponse(500, entity = s"failed to delete filter for $owner")
  }

}
