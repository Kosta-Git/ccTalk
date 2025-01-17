package cctalk.packet

import be.inotek.communication.CcTalkChecksumTypes
import be.inotek.communication.packet.CcTalkPacket
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalUnsignedTypes::class)
class CcTalkPacketTest {

  @Test
  fun `test basic packet creation`() {
    val packet = CcTalkPacket.build {
      data(ubyteArrayOf(1u, 2u, 3u))
      destination(2u)
      source(1u)
      header(242u)
      checksum(7u)
      checksumType(CcTalkChecksumTypes.CRC16)
    }

    assertEquals(2u, packet.destination)
    assertEquals(1u, packet.source)
    assertEquals(242u, packet.header)
    assertEquals(3u, packet.dataLength)
    assertTrue(packet.data.contentEquals(ubyteArrayOf(1u, 2u, 3u)))
    assertEquals(7u, packet.checksum)
  }

  @Test
  fun `test packet with empty data`() {
    val packet = CcTalkPacket.build {
      destination(2u)
      source(1u)
      header(242u)
      checksum(7u)
    }

    assertEquals(0u, packet.dataLength)
    assertTrue(packet.data.isEmpty())
  }

  @Test
  fun `test packet with max data length`() {
    val maxData = UByteArray(250) { it.toUByte() }
    val packet = CcTalkPacket.build {
      data(maxData)
      destination(2u)
      source(1u)
      header(242u)
      checksum(7u)
    }

    assertEquals(250u, packet.dataLength)
    assertTrue(packet.data.contentEquals(maxData))
  }

  @Test
  fun `test packet data length exceeds max throws exception`() {
    assertThrows<IllegalArgumentException> {
      val tooLongData = UByteArray(251) { it.toUByte() }
      CcTalkPacket.build {
        destination(2u)
        source(1u)
        header(242u)
        data(tooLongData)
      }
    }
  }

  @Test
  fun `test clear packet`() {
    val packet = CcTalkPacket.build {
      data(ubyteArrayOf(1u, 2u, 3u))
      source(1u)
      header(242u)
      checksum(7u)
    }

    packet.clear()

    assertEquals(0u, packet.dataLength)
    assertEquals(0u, packet.destination)
    assertEquals(0u, packet.source)
    assertEquals(0u, packet.header)
    assertTrue(packet.data.isEmpty())
  }

  @Test
  fun `test data length change preserves header info but clears data`() {
    val packet = CcTalkPacket.build {
      data(ubyteArrayOf(1u, 2u, 3u))
      source(1u)
      header(242u)
      checksum(7u)
    }

    // Save original header values
    val originalDestination = packet.destination
    val originalSource = packet.source
    val originalHeader = packet.header
    val originalChecksum = packet.checksum

    // Change data length
    packet.dataLength = 5u

    // Verify header information is preserved
    assertEquals(originalDestination, packet.destination)
    assertEquals(originalSource, packet.source)
    assertEquals(originalHeader, packet.header)
    assertEquals(originalChecksum, packet.checksum)

    // Verify data is cleared (all zeros)
    assertTrue(packet.data.all { it.toUInt() == 0u })
  }

  @Test
  fun `test packet equality`() {
    val packet1 = CcTalkPacket.build {
      destination(2u)
      source(1u)
      header(242u)
      data(ubyteArrayOf(1u, 2u, 3u))
      checksum(7u)
    }

    val packet2 = CcTalkPacket.build {
      destination(2u)
      source(1u)
      header(242u)
      data(ubyteArrayOf(1u, 2u, 3u))
      checksum(7u)
    }

    assertEquals(packet1, packet2)
    assertEquals(packet1.hashCode(), packet2.hashCode())
  }
}