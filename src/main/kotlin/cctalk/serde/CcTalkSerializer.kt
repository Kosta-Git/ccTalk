package cctalk.serde

import arrow.core.Either
import arrow.core.raise.either
import be.inotek.communication.CcTalkChecksumTypes
import cctalk.CcTalkError
import cctalk.packet.CcTalkPacket
import cctalk.packet.CcTalkPacketBuilder
import cctalk.packet.DATA_LENGTH_OFFSET
import cctalk.packet.DATA_OFFSET
import cctalk.packet.DESTINATION_OFFSET
import cctalk.packet.HEADER_OFFSET
import cctalk.packet.MAX_BLOCK_LENGTH
import cctalk.packet.SOURCE_OFFSET

data class CcTalkSerializationResult(val data: ByteArray, val size: Int) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as CcTalkSerializationResult

    if (size != other.size) return false
    if (!data.contentEquals(other.data)) return false

    return true
  }

  override fun hashCode(): Int {
    var result = size
    result = 31 * result + data.contentHashCode()
    return result
  }
}

interface CcTalkSerializer {
  fun serialize(packet: CcTalkPacket): CcTalkSerializationResult
  fun deserialize(
    data: ByteArray,
    checksumType: CcTalkChecksumTypes = CcTalkChecksumTypes.Simple8,
    verifyChecksum: Boolean = true
  ): Either<CcTalkError, CcTalkPacket>
}

class CcTalkSerializerImpl : CcTalkSerializer {
  override fun serialize(packet: CcTalkPacket): CcTalkSerializationResult = with(packet) {
    ByteArray(dataLength + 5) {
      when (it) {
        0 -> destination.toByte()
        1 -> dataLength.toByte()
        2 -> source.toByte()
        3 -> header.toByte()
        in 4 until dataLength + 4 -> data[it - 4].toByte()
        dataLength + 4 -> checksum.toByte()
        else -> throw IllegalStateException("Invalid index $it")
      }
    }.let { rawData ->
      CcTalkSerializationResult(rawData, rawData.size)
    }
  }

  /**
   * Deserializes a byte array into a CcTalkPacket
   * @param data The byte array to deserialize
   * @param checksumType The checksum type to use
   * @param verifyChecksum Whether to verify the checksum
   * @return The deserialized CcTalkPacket
   * @throws [IllegalStateException] If the data length is invalid or the checksum is invalid
   */
  override fun deserialize(
    data: ByteArray,
    checksumType: CcTalkChecksumTypes,
    verifyChecksum: Boolean
  ): Either<CcTalkError, CcTalkPacket> = either {
    if (data.size < 5 || data.size > MAX_BLOCK_LENGTH) raise(
      CcTalkError.DataLengthError(
        5,
        MAX_BLOCK_LENGTH,
        data.size
      )
    )
    // Convert to uByte then int to have real byte representation (0-255)
    CcTalkPacketBuilder().apply {
      destination(data[DESTINATION_OFFSET].toUByte().toInt())
      val advertisedDataLength = data[DATA_LENGTH_OFFSET].toUByte().toInt()
      source(data[SOURCE_OFFSET].toUByte().toInt())
      header(data[HEADER_OFFSET].toUByte().toInt())
      data(IntArray(data.size - 5) { data[DATA_OFFSET + it].toUByte().toInt() })
      checksum(data[data.size - 1].toUByte().toInt())
      checksumType(checksumType)
      if(advertisedDataLength != data.size - 5 && checksumType != CcTalkChecksumTypes.CRC16) raise(CcTalkError.DataFormatError(advertisedDataLength, data.size - 5))
    }.build().apply {
      if (verifyChecksum && !isChecksumValid()) raise(CcTalkError.ChecksumError())
    }
  }
}