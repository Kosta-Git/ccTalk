package cctalk.serde

import arrow.core.Either
import be.inotek.communication.CcTalkChecksumTypes
import be.inotek.communication.packet.CcTalkPacket
import be.inotek.communication.packet.MAX_BLOCK_LENGTH
import cctalk.CcTalkStatus

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
  fun serialize(packet: CcTalkPacket, computeChecksum: Boolean = true): CcTalkSerializationResult
  fun deserialize(
    data: ByteArray,
    checksumType: CcTalkChecksumTypes = CcTalkChecksumTypes.Simple8,
    verifyChecksum: Boolean = true
  ): Either<CcTalkStatus, CcTalkPacket>
}

@OptIn(ExperimentalUnsignedTypes::class)
class CcTalkSerializerImpl : CcTalkSerializer {
  override fun serialize(packet: CcTalkPacket, computeChecksum: Boolean): CcTalkSerializationResult = with(packet) {
    if (computeChecksum) setCheckSum()
    return CcTalkSerializationResult(ByteArray(rawData.size) { i -> rawData[i].toByte() }, rawData.size)
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
  ): Either<CcTalkStatus, CcTalkPacket> {
    if(data.size < 5 || data.size > MAX_BLOCK_LENGTH) return Either.Left(CcTalkStatus.DataLen)
    val packet = CcTalkPacket(UByteArray(data.size) { i -> data[i].toUByte() }, checksumType)
    if (verifyChecksum && !packet.isChecksumValid()) return Either.Left(CcTalkStatus.ChSumErr)
    return Either.Right(packet)
  }
}