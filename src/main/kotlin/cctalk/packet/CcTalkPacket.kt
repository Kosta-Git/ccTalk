package be.inotek.communication.packet

import be.inotek.communication.CcTalkChecksumTypes
import cctalk.device.CcTalkDevice

const val MAX_BLOCK_LENGTH = 255
const val DESTINATION_OFFSET = 0
const val DATA_LENGTH_OFFSET = 1
const val SOURCE_OFFSET = 2
const val HEADER_OFFSET = 3
const val DATA_OFFSET = 4

@OptIn(ExperimentalUnsignedTypes::class)
data class CcTalkPacket(
  var rawData: UByteArray = UByteArray(5),
  val checksumType: CcTalkChecksumTypes = CcTalkChecksumTypes.Simple8
) {
  init {
    require(rawData.size >= 5) { "Raw data must be at least 5 bytes" }
  }

  var destination: UByte
    get() = rawData[DESTINATION_OFFSET]
    set(value) {
      rawData[DESTINATION_OFFSET] = value
    }

  var dataLength: UByte
    get() = rawData[DATA_LENGTH_OFFSET]
    set(value) {
      require(value.toInt() <= MAX_BLOCK_LENGTH - 5) { "Data length must be less than ${MAX_BLOCK_LENGTH - 5}" }
      if (value != dataLength) {
        // Save header information
        val oldDestination = destination
        val oldSource = source
        val oldHeader = header
        val oldChecksum = checksum

        // Create new array with proper size
        rawData = UByteArray(DATA_OFFSET + value.toInt() + 1)

        // Restore header information
        destination = oldDestination
        source = oldSource
        header = oldHeader
        checksum = oldChecksum
      }
      rawData[DATA_LENGTH_OFFSET] = value
    }

  var source: UByte
    get() = rawData[SOURCE_OFFSET]
    set(value) {
      rawData[SOURCE_OFFSET] = value
    }

  var header: UByte
    get() = rawData[HEADER_OFFSET]
    set(value) {
      rawData[HEADER_OFFSET] = value
    }

  var data: UByteArray
    get() = rawData.copyOfRange(DATA_OFFSET, DATA_OFFSET + dataLength.toInt())
    set(value) {
      dataLength = value.size.toUByte()
      value.copyInto(rawData, DATA_OFFSET)
    }

  var checksum: UByte
    get() = rawData[rawData.size - 1]
    set(value) {
      rawData[rawData.size - 1] = value
    }

  fun clear() {
    rawData = UByteArray(5)
    dataLength = 0u
  }

  companion object {
    fun build(block: CcTalkPacketBuilder.() -> Unit): CcTalkPacket =
      CcTalkPacketBuilder().apply(block).build()
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
    other as CcTalkPacket
    return rawData.contentEquals(other.rawData)
  }

  override fun hashCode(): Int {
    return rawData.contentHashCode()
  }

  override fun toString(): String =
    "CcTalkPacket(destination=${destination}, dataLength=${dataLength}, " +
        "source=${source}, header=${header}, data=${data.joinToString()})"
}

@OptIn(ExperimentalUnsignedTypes::class)
class CcTalkPacketBuilder {
  private var destination: UByte = 0u
  private var source: UByte = CcTalkDevice.CcTalkCommand.SOURCE_ADDRESS
  private var header: UByte = 0u
  private var data: UByteArray = UByteArray(0)
  private var checksum: UByte = 0u
  private var checksumType: CcTalkChecksumTypes = CcTalkChecksumTypes.Simple8

  fun destination(value: UByte) = apply { destination = value }
  fun destination(value: Byte) = apply { destination = value.toUByte() }
  fun source(value: UByte) = apply { source = value }
  fun header(value: UByte) = apply { header = value }
  fun data(value: UByteArray) = apply { data = value }
  fun checksum(value: UByte) = apply { checksum = value }
  fun checksumType(value: CcTalkChecksumTypes) = apply { checksumType = value }
  fun withDefaults(device: CcTalkDevice) = apply {
    destination = device.address.toUByte()
    source = CcTalkDevice.CcTalkCommand.SOURCE_ADDRESS
    checksumType = device.checksumType
  }

  fun build(): CcTalkPacket {
    val packet = CcTalkPacket(
      UByteArray(DATA_OFFSET + data.size + 1),
      checksumType
    )
    packet.destination = destination
    packet.data = data
    packet.source = source
    packet.header = header
    packet.checksum = checksum
    return packet
  }
}
