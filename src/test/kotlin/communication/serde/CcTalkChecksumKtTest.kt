package communication.serde

import be.inotek.communication.CcTalkChecksumTypes
import be.inotek.communication.packet.CcTalkPacketBuilder
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

@OptIn(ExperimentalUnsignedTypes::class)
class CcTalkChecksumTest {

  @Test
  fun `test Simple8 checksum calculation from specification example`() {
    // Example from spec section 7.12: [2][0][1][242][11]
    val packet = CcTalkPacketBuilder()
      .destination(2u)
      .source(1u)
      .header(242u)
      .checksumType(CcTalkChecksumTypes.Simple8)
      .build()

    packet.setCheckSum()
    assertEquals(11u, packet.checksum.toUInt())
    assertTrue(packet.isChecksumValid())
  }

  @Test
  fun `test Simple8 checksum for ACK message from specification`() {
    // Example from spec section 8: ACK message
    val packet = CcTalkPacketBuilder()
      .destination(1u)
      .source(2u)
      .header(0u)
      .checksumType(CcTalkChecksumTypes.Simple8)
      .build()

    packet.setCheckSum()
    assertTrue(packet.isChecksumValid())

    // Verify total sum is zero (modulo 256)
    val sum = packet.destination.toInt() +
        packet.dataLength.toInt() +
        packet.source.toInt() +
        packet.header.toInt() +
        packet.checksum.toInt()
    assertEquals(0, sum % 256)
  }

  @Test
  fun `test Simple8 checksum with data bytes from specification example`() {
    // Example from spec section 7.12:
    // [1][3][2][0][78][97][188][143]
    val packet = CcTalkPacketBuilder()
      .destination(1u)
      .source(2u)
      .header(0u)
      .data(ubyteArrayOf(78u, 97u, 188u))
      .checksumType(CcTalkChecksumTypes.Simple8)
      .build()

    packet.setCheckSum()
    assertEquals(143u, packet.checksum.toUInt())
    assertTrue(packet.isChecksumValid())
  }

  @Test
  fun `test CRC16 checksum calculation`() {
    val packet = CcTalkPacketBuilder()
      .destination(40u)  // Example bill validator address
      .source(0u)  // Will be replaced by CRC LSB
      .header(242u)  // Request serial number
      .checksumType(CcTalkChecksumTypes.CRC16)
      .build()

    packet.setCrc16Checksum()
    assertTrue(packet.isCrc16ChecksumValid())

    // Verify CRC bytes are different
    assertNotEquals(packet.source, packet.checksum)
  }

  @Test
  fun `test checksum validation with corrupted data`() {
    val packet = CcTalkPacketBuilder()
      .destination(2u)
      .source(1u)
      .header(242u)
      .checksumType(CcTalkChecksumTypes.Simple8)
      .build()

    packet.setCheckSum()
    val originalChecksum = packet.checksum

    // Corrupt the header
    packet.header = (packet.header.toInt() + 1).toUByte()
    assertFalse(packet.isChecksumValid())

    // Restore header but corrupt checksum
    packet.header = (packet.header.toInt() - 1).toUByte()
    packet.checksum = (originalChecksum.toInt() + 1).toUByte()
    assertFalse(packet.isChecksumValid())
  }

  @Test
  fun `test CRC16 checksum validation with corrupted data`() {
    val packet = CcTalkPacketBuilder()
      .destination(40u)
      .source(0u)
      .header(242u)
      .checksumType(CcTalkChecksumTypes.CRC16)
      .build()

    packet.setCrc16Checksum()
    val originalSource = packet.source
    val originalChecksum = packet.checksum

    // Corrupt the header
    packet.header = 0u
    assertFalse(packet.isCrc16ChecksumValid())

    // Restore header but corrupt CRC bytes
    packet.header = (packet.header.toInt() - 1).toUByte()
    packet.source = (originalSource.toInt() + 1).toUByte()
    assertFalse(packet.isCrc16ChecksumValid())

    packet.source = originalSource
    packet.checksum = (originalChecksum.toInt() + 1).toUByte()
    assertFalse(packet.isCrc16ChecksumValid())
  }

  @Test
  fun `test real world message sequence`() {
    // Test both sides of a typical message exchange

    // Host sends request for serial number
    val request = CcTalkPacketBuilder()
      .destination(2u)
      .source(1u)
      .header(242u)
      .checksumType(CcTalkChecksumTypes.Simple8)
      .build()

    request.setCheckSum()
    assertTrue(request.isChecksumValid())
    assertEquals(11u, request.checksum.toUInt())

    // Device responds with serial number
    val response = CcTalkPacketBuilder()
      .destination(1u)
      .source(2u)
      .header(0u)
      .data(ubyteArrayOf(78u, 97u, 188u))
      .checksumType(CcTalkChecksumTypes.Simple8)
      .build()

    response.setCheckSum()
    assertTrue(response.isChecksumValid())
    assertEquals(143u, response.checksum.toUInt())
  }
}