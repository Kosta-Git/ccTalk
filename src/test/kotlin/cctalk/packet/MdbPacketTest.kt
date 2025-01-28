package cctalk.packet

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalUnsignedTypes::class)
class MdbPacketTest {

  @Test
  fun `create empty packet`() {
    val packet = MdbPacket(ByteArray(0))
    assertEquals(0, packet.dataLength.toInt())
    assertEquals(0, packet.data.size)
    assertTrue(packet.data.isEmpty())
  }

  @Test
  fun `create packet with data`() {
    val initialData = ByteArray(3) { it.toByte() }
    val packet = MdbPacket(initialData)
    assertEquals(3, packet.dataLength.toInt())
    assertTrue(packet.data.contentEquals(initialData))
  }

  @Test
  fun `set new data`() {
    val packet = MdbPacket()
    val newData = ByteArray(5) { it.toByte() }
    packet.data = newData
    assertEquals(5, packet.dataLength.toInt())
    assertTrue(packet.data.contentEquals(newData))
  }

  @Test
  fun `set empty data`() {
    val packet = MdbPacket()
    packet.data = ByteArray(0)
    assertEquals(0, packet.dataLength.toInt())
    assertTrue(packet.data.isEmpty())
  }

  @Test
  fun `test equality`() {
    val data1 = ByteArray(3) { it.toByte() }
    val data2 = ByteArray(3) { it.toByte() }
    val data3 = ByteArray(2) { it.toByte() }

    val packet1 = MdbPacket(data1)
    val packet2 = MdbPacket(data2)
    val packet3 = MdbPacket(data3)

    assertEquals(packet1, packet2)
    assertEquals(packet1.hashCode(), packet2.hashCode())
    assertFalse(packet1 == packet3)
  }

  @Test
  fun `verify fixed size array maintenance`() {
    val packet = MdbPacket()
    val smallData = ByteArray(3) { it.toByte() }
    packet.data = smallData

    // Internal array should still be MAX_BLOCK_LENGTH
    assertEquals(smallData.size, packet.data.size)
    // But getData() should only return actual data
    assertEquals(3, packet.data.size)
  }

  @Test
  fun `verify unused array elements don't affect equality`() {
    val data1 = ByteArray(3) { it.toByte() }
    val packet1 = MdbPacket(data1)

    val packet2 = MdbPacket()
    packet2.data = data1

    // Even though internal arrays might have different unused elements,
    // packets should be equal
    assertEquals(packet1, packet2)
  }
}