package communication.packet

import arrow.core.Either
import communication.serial.ConcurrentSerialPort.SerialError
import kotlinx.coroutines.CompletableDeferred

data class PacketRequest(
  val data: ByteArray,
  val response: CompletableDeferred<Either<SerialError, ByteArray>>
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as PacketRequest

    if (!data.contentEquals(other.data)) return false
    if (response != other.response) return false

    return true
  }

  override fun hashCode(): Int {
    var result = data.contentHashCode()
    result = 31 * result + response.hashCode()
    return result
  }
}
