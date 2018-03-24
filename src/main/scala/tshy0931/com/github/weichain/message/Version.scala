package tshy0931.com.github.weichain.message

/** https://bitcoin.org/en/developer-reference#version
  * 72110100 ........................... Protocol version: 70002
  0100000000000000 ................... Services: NODE_NETWORK
  bc8f5e5400000000 ................... Epoch time: 1415483324

  0100000000000000 ................... Receiving node's services
  00000000000000000000ffffc61b6409 ... Receiving node's IPv6 address
  208d ............................... Receiving node's port number

  0100000000000000 ................... Transmitting node's services
  00000000000000000000ffffcb0071c0 ... Transmitting node's IPv6 address
  208d ............................... Transmitting node's port number

  128035cbc97953f8 ................... Nonce

  0f ................................. Bytes in user agent string: 15
  2f5361746f7368693a302e392e332f ..... User agent: /Satoshi:0.9.3/

  cf050500 ........................... Start height: 329167
  01 ................................. Relay flag: true
  * @param version
  * @param services
  * @param timestamp
  * @param nonce
  * @param startHeight
  * @param relay
  */
case class Version(version: Int,
                   services: Long,
                   timestamp: Long,
                   nonce: Long,
                   startHeight: Long,
                   relay: Boolean)
