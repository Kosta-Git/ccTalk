package cctalk.packet

import be.inotek.communication.CcTalkChecksumTypes
import cctalk.device.CcTalkDevice
import cctalk.serde.getCheckSum

const val MAX_BLOCK_LENGTH = 255
const val DESTINATION_OFFSET = 0
const val DATA_LENGTH_OFFSET = 1
const val SOURCE_OFFSET = 2
const val HEADER_OFFSET = 3
const val DATA_OFFSET = 4

class CcTalkPacket {
  val destination: Int
  val dataLength: Int
  val source: Int
  val header: Int
  val data: IntArray
  val checksum: Int
  val checksumType: CcTalkChecksumTypes

  constructor(
    destination: Int,
    dataLength: Int,
    source: Int,
    header: Int,
    data: IntArray,
    checksum: Int?,
    checksumType: CcTalkChecksumTypes = CcTalkChecksumTypes.Simple8
  ) {
    require(destination in 0..255) { "Destination must be between 0 and 255" }
    require(dataLength in 0..MAX_BLOCK_LENGTH - 5) { "Data length must be less than ${MAX_BLOCK_LENGTH - 5}" }
    require(source in 0..255) { "Source must be between 0 and 255" }
    require(header in 0..255) { "Header must be between 0 and 255" }
    require(data.size == dataLength) { "Data length must match data length" }

    this.checksumType = checksumType
    this.destination = destination
    this.dataLength = dataLength
    this.header = header
    this.data = data

    if (checksum == null) {
      when (checksumType) {
        CcTalkChecksumTypes.Simple8 -> {
          this.source = source
          this.checksum = getCheckSum()
        }

        CcTalkChecksumTypes.CRC16 -> {
          val computedChecksum = getCheckSum()
          this.source = (computedChecksum and 0xFF).toUByte().toInt()
          this.checksum = ((computedChecksum shr 8) and 0xFF).toUByte().toInt()
        }
      }
    } else {
      this.source = source
      this.checksum = checksum
    }
  }

  companion object {
    fun build(block: CcTalkPacketBuilder.() -> Unit): CcTalkPacket =
      CcTalkPacketBuilder().apply(block).build()
  }

  override fun toString(): String =
    "CcTalkPacket(destination=${destination}, dataLength=${dataLength}, " +
        "source=${source}, header=${header}, data=${data.joinToString()})"

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as CcTalkPacket

    if (destination != other.destination) return false
    if (dataLength != other.dataLength) return false
    if (source != other.source) return false
    if (header != other.header) return false
    if (checksum != other.checksum) return false
    if (!data.contentEquals(other.data)) return false
    if (checksumType != other.checksumType) return false

    return true
  }

  override fun hashCode(): Int {
    var result = destination
    result = 31 * result + dataLength
    result = 31 * result + source
    result = 31 * result + header
    result = 31 * result + checksum
    result = 31 * result + data.contentHashCode()
    result = 31 * result + checksumType.hashCode()
    return result
  }
}

@OptIn(ExperimentalUnsignedTypes::class)
class CcTalkPacketBuilder {
  private var destination: Int = 0
  private var source: Int = CcTalkDevice.CcTalkCommand.SOURCE_ADDRESS.toInt()
  private var header: Int = 0
  private var data: IntArray = IntArray(0)
  private var checksum: Int? = null
  private var checksumType: CcTalkChecksumTypes = CcTalkChecksumTypes.Simple8

  fun destination(value: Int) = apply { destination = value }
  fun source(value: Int) = apply { source = value }
  fun header(value: Int) = apply { header = value }
  fun checksum(value: Int) = apply { checksum = value }
  fun data(value: IntArray) = apply { data = value }

  fun destination(value: UByte) = apply { destination = value.toInt() }
  fun source(value: UByte) = apply { source = value.toInt() }
  fun header(value: UByte) = apply { header = value.toInt() }
  fun checksum(value: UByte) = apply { checksum = value.toInt() }
  fun data(value: UByteArray) = apply { data = value.map { it.toInt() }.toIntArray() }

  fun destination(value: Byte) = apply { destination = value.toUByte().toInt() }
  fun source(value: Byte) = apply { source = value.toUByte().toInt() }
  fun header(value: Byte) = apply { header = value.toUByte().toInt() }
  fun data(value: ByteArray) = apply { data = value.map { it.toUByte().toInt() }.toIntArray() }
  fun checksum(value: Byte) = apply { checksum = value.toUByte().toInt() }

  fun checksumType(value: CcTalkChecksumTypes) = apply { checksumType = value }
  fun withDefaults(device: CcTalkDevice) = apply {
    destination = device.address
    source = CcTalkDevice.CcTalkCommand.SOURCE_ADDRESS
    checksumType = device.checksumType
  }

  fun build(): CcTalkPacket {
    require(data.size <= MAX_BLOCK_LENGTH - 5) { "Data length must be less than ${MAX_BLOCK_LENGTH - 5}" }
    return CcTalkPacket(
      destination = destination,
      dataLength = data.size,
      source = source,
      header = header,
      data = data,
      checksum = checksum,
      checksumType = checksumType
    )
  }
}
