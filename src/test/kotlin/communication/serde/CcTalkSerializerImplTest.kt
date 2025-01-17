package communication.serde

import be.inotek.communication.CcTalkChecksumTypes
import be.inotek.communication.packet.CcTalkPacket
import be.inotek.communication.packet.MAX_BLOCK_LENGTH
import communication.CcTalkStatus
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.fail

@OptIn(ExperimentalUnsignedTypes::class)
class CcTalkSerializerImplTest {
  private val serializer: CcTalkSerializer = CcTalkSerializerImpl()

  @Test
  fun `serialize with computeChecksum true should set checksum`() {
    // Given
    val packet = CcTalkPacket(
      UByteArray(7) {
        when (it) {
          0 -> 2u    // destination
          1 -> 2u    // data length
          2 -> 1u    // source
          3 -> 254u  // header
          4 -> 5u    // data[0]
          5 -> 6u    // data[1]
          6 -> 0u    // checksum (will be calculated)
          else -> 0u
        }
      },
      CcTalkChecksumTypes.Simple8
    )

    // When
    val result = serializer.serialize(packet, computeChecksum = true)

    // Then
    assertEquals(7, result.size)
    assertTrue(
      CcTalkPacket(
        UByteArray(result.size) { result.data[it].toUByte() },
        CcTalkChecksumTypes.Simple8
      ).isChecksumValid()
    )
  }

  @Test
  fun `serialize with computeChecksum false should not set checksum`() {
    // Given
    val packet = CcTalkPacket(
      UByteArray(7) { 1u }, // All 1's will produce invalid checksum
      CcTalkChecksumTypes.Simple8
    )

    // When
    val result = serializer.serialize(packet, computeChecksum = false)

    // Then
    assertEquals(7, result.size)
    assertContentEquals(ByteArray(7) { 1 }, result.data)
  }

  @Test
  fun `deserialize with valid data and checksum should succeed`() {
    // Given
    val originalPacket = CcTalkPacket(
      UByteArray(7) {
        when (it) {
          0 -> 2u    // destination
          1 -> 2u    // data length
          2 -> 1u    // source
          3 -> 254u  // header
          4 -> 5u    // data[0]
          5 -> 6u    // data[1]
          6 -> 0u    // checksum (will be calculated)
          else -> 0u
        }
      },
      CcTalkChecksumTypes.Simple8
    )
    val serialized = serializer.serialize(originalPacket, computeChecksum = true)

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
    assertTrue { deserialized.leftOrNull() == CcTalkStatus.DataLen }
  }

  @Test
  fun `deserialize with data length greater than MAX_BLOCK_LENGTH should throw IllegalArgumentException`() {
    // Given
    val data = ByteArray(MAX_BLOCK_LENGTH + 1) { 0 }

    // When/Then
    var deserialized = serializer.deserialize(data, CcTalkChecksumTypes.Simple8)
    assertTrue { deserialized.isLeft() }
    assertTrue { deserialized.leftOrNull() == CcTalkStatus.DataLen }
  }

  @Test
  fun `deserialize with verifyChecksum false should skip checksum validation`() {
    // Given
    val data = ByteArray(5) { 1 } // All 1's will produce invalid checksum

    // When
    val packet = serializer.deserialize(data, CcTalkChecksumTypes.Simple8, verifyChecksum = false)

    // Then
    assertTrue(packet.getOrNull()?.isChecksumValid() == false)
  }

  @Test
  fun `deserialize with invalid checksum should throw IllegalStateException`() {
    // Given
    val data = ByteArray(5) { 1 } // All 1's will produce invalid checksum

    // When/Then
    var deserialized = serializer.deserialize(data, CcTalkChecksumTypes.Simple8)
    assertTrue { deserialized.isLeft() }
    assertTrue { deserialized.leftOrNull() == CcTalkStatus.ChSumErr }
  }

  @Test
  fun `test CRC16 checksum serialization and deserialization`() {
    // Given
    val originalPacket = CcTalkPacket(
      UByteArray(7) {
        when (it) {
          0 -> 2u    // destination
          1 -> 2u    // data length
          2 -> 0u    // source (will be part of CRC)
          3 -> 254u  // header
          4 -> 5u    // data[0]
          5 -> 6u    // data[1]
          6 -> 0u    // checksum (will be calculated)
          else -> 0u
        }
      },
      CcTalkChecksumTypes.CRC16
    )

    // When
    val serialized = serializer.serialize(originalPacket, computeChecksum = true)
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