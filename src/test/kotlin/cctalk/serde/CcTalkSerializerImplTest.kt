package cctalk.serde

import be.inotek.communication.CcTalkChecksumTypes
import cctalk.CcTalkStatus
import cctalk.packet.CcTalkPacket
import cctalk.packet.MAX_BLOCK_LENGTH
import org.junit.jupiter.api.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

@OptIn(ExperimentalUnsignedTypes::class)
class CcTalkSerializerImplTest {
  private val serializer: CcTalkSerializer = CcTalkSerializerImpl()

  @Test
  fun `serialize with computeChecksum true should set checksum`() {
    // Given
    val packet = CcTalkPacket(
      destination = 2,
      dataLength = 2,
      source = 1,
      header = 254,
      data = intArrayOf(5, 6),
      checksum = null,
      checksumType = CcTalkChecksumTypes.Simple8
    )

    // When
    val result = serializer.serialize(packet)

    // Then
    assertEquals(7, result.size)
    assertTrue(
      CcTalkPacket(
        destination = result.data[0].toUByte().toInt(),
        dataLength = result.data[1].toUByte().toInt(),
        source = result.data[2].toUByte().toInt(),
        header = result.data[3].toUByte().toInt(),
        data = IntArray(2) { result.data[it + 4].toInt() },
        checksum = result.data[6].toUByte().toInt(),
        checksumType = CcTalkChecksumTypes.Simple8
      ).isChecksumValid()
    )
  }

  @Test
  fun `deserialize with valid data and checksum should succeed`() {
    // Given
    val originalPacket = CcTalkPacket(
      destination = 2,
      dataLength = 2,
      source = 1,
      header = 254,
      data = intArrayOf(5, 6),
      checksum = null,
      checksumType = CcTalkChecksumTypes.Simple8
    )
    val serialized = serializer.serialize(originalPacket)

    // When
    val deserialized = serializer.deserialize(serialized.data, CcTalkChecksumTypes.Simple8)

    // Then
    val packet = deserialized.getOrNull() ?: fail("Deserialization failed")
    assertEquals(originalPacket.destination, packet.destination)
    assertEquals(originalPacket.dataLength, packet.dataLength)
    assertEquals(originalPacket.source, packet.source)
    assertEquals(originalPacket.header, packet.header)
    assertContentEquals(originalPacket.data, packet.data)
  }

  @Test
  fun `deserialize with data length less than 5 should throw IllegalArgumentException`() {
    // Given
    val data = ByteArray(4) { 0 }

    // When/Then
    var deserialized = serializer.deserialize(data, CcTalkChecksumTypes.Simple8)
    assertTrue { deserialized.isLeft() }
    assertTrue { deserialized.leftOrNull()?.status!! == CcTalkStatus.DataFormat }
  }

  @Test
  fun `deserialize with data length greater than MAX_BLOCK_LENGTH should throw IllegalArgumentException`() {
    // Given
    val data = ByteArray(MAX_BLOCK_LENGTH + 1) { 0 }

    // When/Then
    var deserialized = serializer.deserialize(data, CcTalkChecksumTypes.Simple8)
    assertTrue { deserialized.isLeft() }
    assertTrue { deserialized.leftOrNull()?.status!! == CcTalkStatus.DataFormat }
  }

  @Test
  fun `deserialize with verifyChecksum false should skip checksum validation`() {
    // Given
    val data = ByteArray(6) { 1 } // All 1's will produce invalid checksum

    // When
    val packet = serializer.deserialize(data, CcTalkChecksumTypes.Simple8, verifyChecksum = false)

    // Then
    assertTrue(packet.getOrNull()?.isChecksumValid() == false)
  }

  @Test
  fun `deserialize with invalid checksum should throw IllegalStateException`() {
    // Given
    val data = ByteArray(6) { 1 } // All 1's will produce invalid checksum

    // When/Then
    var deserialized = serializer.deserialize(data, CcTalkChecksumTypes.Simple8)
    assertTrue { deserialized.isLeft() }
    assertTrue { deserialized.leftOrNull()?.status!! == CcTalkStatus.ChSumErr }
  }

  @Test
  fun `test CRC16 checksum serialization and deserialization`() {
    // Given
    val originalPacket = CcTalkPacket(
      destination = 2,
      dataLength = 2,
      source = 0,
      header = 254,
      data = intArrayOf(5, 6),
      checksum = null,
      checksumType = CcTalkChecksumTypes.CRC16
    )

    // When
    val serialized = serializer.serialize(originalPacket)
    val deserialized = serializer.deserialize(serialized.data, CcTalkChecksumTypes.CRC16)

    // Then
    val packet = deserialized.getOrNull() ?: fail("Deserialization failed")
    assertEquals(originalPacket.destination, packet.destination)
    assertEquals(originalPacket.dataLength, packet.dataLength)
    assertEquals(originalPacket.header, packet.header)
    assertContentEquals(
      originalPacket.data.sliceArray(0 until originalPacket.dataLength.toInt()),
      packet.data.sliceArray(0 until packet.dataLength.toInt())
    )
    assertTrue(packet.isChecksumValid())
  }
}