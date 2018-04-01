package tshy0931.com.github.weichain.message

/** https://bitcoin.org/en/developer-reference#message-headers
  * f9beb4d9 ................... Start string: Mainnet
    76657261636b000000000000 ... Command name: verack + null padding
    00000000 ................... Byte count: 0
    5df6e0e2 ................... Checksum: SHA256(SHA256(<empty>))
  * @param startString - Magic bytes indicating the originating network; used to seek to next message when stream state is unknown.
  * @param commandName - ASCII string which identifies what message type is contained in the payload. Followed by nulls (0x00) to pad out byte count; for example: version\0\0\0\0\0
  * @param payloadSize - Number of bytes in payload. The current maximum is 32 MiB—messages. Payload size larger than this will be dropped or rejected.
  * @param checksum    - First 4 bytes of SHA256(SHA256(payload)) in internal byte order.
  */
case class MessageHeader(startString: String = "f9beb4d9", // Mainnet
                         commandName: String = "unused",
                         payloadSize: Long = 0L,
                         checksum: String = "")
