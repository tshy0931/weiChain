package tshy0931.com.github.weichain.model

import tshy0931.com.github.weichain.model.Block.{Body, Header}

case class Block(header: Header, body: Body)

object Block {

  case class Header(hash: String,
                    version: Int,
                    prev: String,
                    time: Long,
                    bits: Long,
                    nonce: Long
                    )
  case class Body(merkleRoot: String,
                  transactionCount: Int,
                  size: Long,
                  transactions: Vector[Transaction],
                  merkleTree: List[String])
}
