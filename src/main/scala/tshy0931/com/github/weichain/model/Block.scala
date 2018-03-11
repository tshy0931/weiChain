package tshy0931.com.github.weichain.model

case class Block()

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
                  transactions: List[Transaction],
                  merkleTree: List[String])
}
