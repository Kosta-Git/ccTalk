package cctalk.packet

@OptIn(ExperimentalUnsignedTypes::class)
data class MdbPacket(var data: ByteArray = ByteArray(MAX_BLOCK_LENGTH)) {
  companion object {
    const val MAX_BLOCK_LENGTH = 64
  }

  init {
    require(data.size <= MAX_BLOCK_LENGTH) {
      "Data length cannot exceed $MAX_BLOCK_LENGTH bytes"
    }
  }

  val dataLength: Byte
    get() = data.size.toByte()

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as MdbPacket

    if (!data.contentEquals(other.data)) return false
    if (dataLength != other.dataLength) return false

    return true
  }

  override fun hashCode(): Int {
    var result = data.contentHashCode()
    result = 31 * result + dataLength.hashCode()
    return result
  }

  override fun toString(): String {
    return "MdbPacket(data=${data.joinToString()}, dataLength=$dataLength)"
  }
}