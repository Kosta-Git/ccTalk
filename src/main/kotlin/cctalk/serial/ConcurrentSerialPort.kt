package cctalk.serial

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.right
import cctalk.CcTalkError
import cctalk.packet.PacketRequest
import com.fazecast.jSerialComm.SerialPort
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import org.slf4j.LoggerFactory
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
    suspend fun sendPacket(packet: ByteArray): Either<CcTalkError, ByteArray>
}

class ConcurrentSerialPort(
    val port: SerialPort,
    val localEcho: Boolean = false,
    val name: String = port.systemPortName,
    val index: Int = Random.nextInt(),
    val communicationDelay: Int = 50,
    val timeOut: Int = 100,
    private val logger: org.slf4j.Logger = LoggerFactory.getLogger(ConcurrentSerialPort::class.java)
) : ConcurrentPort {
    private val isRunning = AtomicBoolean(false)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val sendQueue = Channel<PacketRequest>(Channel.UNLIMITED)

    init {
        startQueueProcessor()
    }

    private fun startQueueProcessor() {
        if (isRunning.get()) return
        isRunning.set(true)
        scope.launch(Dispatchers.IO) {
            logger.info("Starting queue processor for port $name")
            while (isRunning.get()) {
                try {
                    val request = sendQueue.receive()

                    try {
                        val response = sendPacketInternal(request.data)
                        request.response.complete(response)
                    } catch (e: Exception) {
                        request.response.completeExceptionally(e)
                    }
                } catch (_: CancellationException) {
                    isRunning.set(false)
                    logger.error("Queue processor for port $name was cancelled")
                    break
                } catch (e: Exception) {
                    logger.error("Error in queue processor for port $name", e)
                }
            }
        }
    }

    private suspend fun sendPacketInternal(payload: ByteArray): Either<CcTalkError, ByteArray> = either {
        return with(port) {
            if (!isOpen) raise(CcTalkError.PortError(this.systemPortName ?: "Unknown port"))

            var written = writeBytes(payload, payload.size)
            if (written != payload.size) raise(CcTalkError.WriteError("Failed to write all bytes"))
            port.flushIOBuffers()

            if (localEcho) {
                // Shitty windows only issue
                if (System.getProperty("os.name").lowercase().contains("windows")) {
                    delay(25)
                }

                val echo = readBytes(ByteArray(payload.size), payload.size)
                if (echo != payload.size) raise(CcTalkError.CommunicationError("Local echo does not match"))
            }

            val buffer = mutableListOf<Byte>()
            val startTime = System.currentTimeMillis()
            var hasRead = false

            delay(communicationDelay.toLong())

            while (true) {
                if (System.currentTimeMillis() - startTime > timeOut) {
                    raise(CcTalkError.TimeoutError())
                }

                val available = bytesAvailable()
                if (available > 0) {
                    val tempBuffer = ByteArray(available)
                    val bytesRead = readBytes(tempBuffer, available)
                    if (bytesRead > 0) buffer.addAll(tempBuffer.take(bytesRead))
                }

                if (hasRead && available == 0) {
                    return buffer.toByteArray().right()
                }
                if (available > 0) hasRead = true
                if (available == 0) delay(10)
            }

            return buffer.toByteArray().right()
        }
    }

    override suspend fun sendPacket(packet: ByteArray): Either<CcTalkError, ByteArray> {
        val response = CompletableDeferred<Either<CcTalkError, ByteArray>>()
        val request = PacketRequest(packet, response)
        sendQueue.send(request)
        return response.await()
    }

    override fun open(): Boolean = with(port) {
        baudRate = BAUD_RATE
        numDataBits = DATA_BITS
        numStopBits = STOP_BITS
        parity = PARITY
        setRs485ModeParameters(false, false, 0, 0)
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
}