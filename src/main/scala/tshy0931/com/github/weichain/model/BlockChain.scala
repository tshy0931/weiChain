package tshy0931.com.github.weichain.model

object BlockChain {

  type BlockChain = BlockChain.type

  var version: Int = _
  val initialHash: Array[Byte] = "Wei".getBytes
  val chain: Vector[Block] = Vector.empty[Block]

  def latestBlock: Option[Block] = chain.lastOption
  def blockAt(index: Int): Block = chain(index)
}