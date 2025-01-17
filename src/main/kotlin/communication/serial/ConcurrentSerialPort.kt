package communication.serial

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.right
import com.fazecast.jSerialComm.SerialPort
import communication.CcTalkStatus
import communication.packet.PacketRequest
import communication.serial.ConcurrentSerialPort.SerialError
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.random.Random

const val MAX_RETRIES = 3
const val BAUD_RATE = 9600
const val DATA_BITS = 8
const val STOP_BITS = 1
const val PARITY = SerialPort.NO_PARITY
const val MAX_BLOCK_LENGTH = 64

interface ConcurrentPort {
  fun open(): Boolean
  fun close(): Unit
  suspend fun sendPacket(packet: ByteArray): Either<SerialError, ByteArray>
}

class ConcurrentSerialPort(
  val port: SerialPort,
  val localEcho: Boolean = false,
  val name: String = port.systemPortName,
  val index: Int = Random.nextInt(),
  val communicationDelay: Int = 50,
  val timeOut: Int = 100
) : ConcurrentPort {
  private val isRunning = AtomicBoolean(false)
  private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
  private val sendQueue = Channel<PacketRequest>(Channel.UNLIMITED)

  private var lastCommunicationTime = System.currentTimeMillis()

  init {
    startQueueProcessor()
  }

  private fun startQueueProcessor() {
    if (isRunning.get()) return
    isRunning.set(true)
    scope.launch {
      while (isRunning.get()) {
        try {
          val request = sendQueue.receive()
          ensureCommunicationDelay()

          try {
            val response = sendPacketInternal(request.data)
            request.response.complete(response)
          } catch (e: Exception) {
            request.response.completeExceptionally(e)
          }
        } catch (e: CancellationException) {
          isRunning.set(false)
          break
        } catch (e: Exception) {
          TODO("Use logger")
        }
      }
    }
  }

  private suspend fun sendPacketInternal(payload: ByteArray): Either<SerialError, ByteArray> = either {
    with(port) {
      if (!isOpen) raise(SerialError.PortError("Port is not open"))

      clearDTR()
      clearRTS()
      while (bytesAvailable() > 0) {
        readBytes(ByteArray(bytesAvailable()), bytesAvailable())
      }

      lastCommunicationTime = System.currentTimeMillis()
      var written = writeBytes(payload, payload.size)
      if (written != payload.size) raise(SerialError.WriteError("Failed to write all bytes"))

      if (localEcho) {
        val echo = readBytes(ByteArray(payload.size), payload.size)
        if (echo != payload.size) raise(SerialError.CommunicationError("Local echo does not match"))
      }

      val buffer = mutableListOf<Byte>()
      val startTime = System.currentTimeMillis()
      var hasRead = false

      while (true) {
        if (System.currentTimeMillis() - startTime > timeOut) {
          lastCommunicationTime = System.currentTimeMillis()
          raise(SerialError.TimeoutError("Timeout while waiting for response"))
        }

        val available = bytesAvailable()
        if (available > 0) {
          val tempBuffer = ByteArray(available)
          val bytesRead = readBytes(tempBuffer, available)

          if (bytesRead > 0) buffer.addAll(tempBuffer.take(bytesRead))
        }

        if (hasRead && available == 0) {
          lastCommunicationTime = System.currentTimeMillis()
          return buffer.toByteArray().right()
        }
        if (available > 0) hasRead = true
        if (available == 0) delay(20) else delay(1)
      }

      return buffer.toByteArray().right()
    }
  }

  private suspend fun ensureCommunicationDelay() {
    if (communicationDelay == 0) return;
    val time = lastCommunicationTime + communicationDelay - System.currentTimeMillis()
    if (time > 0) delay(time)
  }

  override suspend fun sendPacket(packet: ByteArray): Either<SerialError, ByteArray> {
    val response = CompletableDeferred<Either<SerialError, ByteArray>>()
    val request = PacketRequest(packet, response)
    sendQueue.send(request)
    return response.await()
  }

  override fun open(): Boolean = with(port) {
    baudRate = BAUD_RATE
    numDataBits = DATA_BITS
    numStopBits = STOP_BITS
    parity = PARITY
    setComPortTimeouts(
      SerialPort.TIMEOUT_READ_SEMI_BLOCKING or SerialPort.TIMEOUT_WRITE_BLOCKING,
      timeOut.toInt(),
      timeOut.toInt()
    )

    return if (isOpen) true else openPort()
  }

  override fun close() {
    scope.cancel()
    port.closePort()
  }

  sealed class SerialError(val status: CcTalkStatus = CcTalkStatus.Unknown) {
    data class PortError(val message: String) : SerialError(CcTalkStatus.BadLine)
    data class TimeoutError(val message: String) : SerialError(CcTalkStatus.RcvTimeout)
    data class CommunicationError(val message: String) : SerialError(CcTalkStatus.CommError)
    data class WriteError(val message: String) : SerialError(CcTalkStatus.Unknown)
    data class ReadError(val message: String) : SerialError(CcTalkStatus.Unknown)
  }
}