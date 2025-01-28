package cctalk.packet

import be.inotek.communication.CcTalkChecksumTypes
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalUnsignedTypes::class)
class CcTalkPacketTest {

  @Test
  fun `test basic packet creation`() {
    val packet = CcTalkPacket.build {
      data(byteArrayOf(1, 2, 3))
      destination(2u)
      source(1u)
      header(242u)
      checksum(7u)
      checksumType(CcTalkChecksumTypes.CRC16)
    }

    assertEquals(2, packet.destination)
    assertEquals(1, packet.source)
    assertEquals(242, packet.header)
    assertEquals(3, packet.dataLength)
    assertTrue(packet.data.contentEquals(intArrayOf(1, 2, 3)))
    assertEquals(7, packet.checksum)
  }

  @Test
  fun `test packet with empty data`() {
    val packet = CcTalkPacket.build {
      destination(2u)
      source(1u)
      header(242u)
      checksum(7u)
    }

    assertEquals(0, packet.dataLength)
    assertTrue(packet.data.isEmpty())
  }

  @Test
  fun `test packet with max data length`() {
    val maxData = IntArray(250) { it }
    val packet = CcTalkPacket.build {
      data(maxData)
      destination(2u)
      source(1u)
      header(242u)
      checksum(7u)
    }

    assertEquals(250, packet.dataLength)
    assertTrue(packet.data.contentEquals(maxData))
  }

  @Test
  fun `test packet data length exceeds max throws exception`() {
    assertThrows<IllegalArgumentException> {
      val tooLongData = ByteArray(251) { it.toByte() }
      CcTalkPacket.build {
        destination(2u)
        source(1u)
        header(242u)
        data(tooLongData)
      }
    }
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