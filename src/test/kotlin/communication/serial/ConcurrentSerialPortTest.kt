package communication.serial

import arrow.core.left
import arrow.core.right
import com.fazecast.jSerialComm.SerialPort
import communication.serial.ConcurrentSerialPort.SerialError
import io.mockk.MockKAnnotations
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ConcurrentSerialPortTest {
  @MockK
  private lateinit var mockPort: SerialPort
  private lateinit var serialPort: ConcurrentSerialPort
  private val counter = AtomicInteger(0)

  @BeforeEach
  fun setup() {
    MockKAnnotations.init(this)

    // Setup default mock behavior
    every { mockPort.systemPortName } returns "TEST_PORT"
    every { mockPort.setBaudRate(any()) } returns true
    every { mockPort.setNumDataBits(any()) } returns true
    every { mockPort.setNumStopBits(any()) } returns true
    every { mockPort.setParity(any()) } returns true
    every { mockPort.setComPortTimeouts(any(), any(), any()) } returns true
    every { mockPort.openPort() } returns true
    every { mockPort.closePort() } returns true
    every { mockPort.isOpen } returns true
    every { mockPort.clearDTR() } returns true
    every { mockPort.clearRTS() } returns true

    serialPort = ConcurrentSerialPort(
      port = mockPort,
      localEcho = false,
      name = "TEST_PORT",
      index = counter.incrementAndGet(),
      communicationDelay = 0,
      timeOut = 100
    )
  }

  @AfterEach
  fun teardown() {
    serialPort.close()
    clearAllMocks()
  }

  @Test
  fun `test port opening with correct parameters`() {
    // Act
    serialPort.open()

    // Assert
    verify {
      mockPort.baudRate = BAUD_RATE
      mockPort.numDataBits = DATA_BITS
      mockPort.numStopBits = STOP_BITS
      mockPort.parity = PARITY
      mockPort.setComPortTimeouts(
        SerialPort.TIMEOUT_READ_SEMI_BLOCKING or SerialPort.TIMEOUT_WRITE_BLOCKING,
        100,
        100
      )
    }
  }

  @Test
  fun `test basic packet sending`() = runBlocking {
    // Arrange
    val testPacket = byteArrayOf(1, 2, 3)
    val expectedResponse = byteArrayOf(4, 5, 6)
    var readCallCount = 0

    every { mockPort.bytesAvailable() } answers {
      when (readCallCount++) {
        0 -> 0  // Initial check
        1 -> expectedResponse.size  // Response available
        else -> 0  // No more data
      }
    }

    every { mockPort.writeBytes(any(), any()) } returns testPacket.size
    every { mockPort.readBytes(any(), any()) } answers {
      val buffer = firstArg<ByteArray>()
      expectedResponse.copyInto(buffer)
      expectedResponse.size
    }

    // Act
    val response = serialPort.sendPacket(testPacket)

    // Assert
    assertArrayEquals(expectedResponse, response.getOrNull())
    verify {
      mockPort.writeBytes(testPacket, testPacket.size)
    }
  }

  @Test
  fun `test communication delay`() = runBlocking {
    // Arrange
    val delayPort = ConcurrentSerialPort(
      port = mockPort,
      communicationDelay = 100,
      timeOut = 200
    )
    val testPacket = byteArrayOf(1, 2, 3)
    val expectedResponse = byteArrayOf(4, 5, 6)
    var readCallCount = 0

    every { mockPort.bytesAvailable() } answers {
      when (readCallCount++) {
        0 -> 0
        1 -> expectedResponse.size
        else -> 0
      }
    }

    every { mockPort.writeBytes(any(), any()) } returns testPacket.size
    every { mockPort.readBytes(any(), any()) } answers {
      val buffer = firstArg<ByteArray>()
      expectedResponse.copyInto(buffer)
      expectedResponse.size
    }

    // Act
    val startTime = System.currentTimeMillis()
    val response = delayPort.sendPacket(testPacket)
    val endTime = System.currentTimeMillis()

    // Assert
    assertTrue(endTime - startTime >= 100)
    assertArrayEquals(expectedResponse, response.getOrNull())
  }

  @Test
  fun `test local echo handling`() = runBlocking {
    // Arrange
    val echoPort = ConcurrentSerialPort(
      port = mockPort,
      localEcho = true,
      timeOut = 200
    )
    val testPacket = byteArrayOf(1, 2, 3)
    val expectedResponse = byteArrayOf(4, 5, 6)
    var readCallCount = 0
    var readEchoCallCount = 0

    every { mockPort.bytesAvailable() } answers {
      when (readCallCount++) {
        0 -> 0  // Initial check
        1 -> testPacket.size  // Echo available
        2 -> expectedResponse.size  // Response available
        else -> 0  // No more data
      }
    }

    every { mockPort.writeBytes(any(), any()) } returns testPacket.size
    every { mockPort.readBytes(any(), any()) } answers {
      val buffer = firstArg<ByteArray>()
      when (readEchoCallCount++) {
        0 -> {
          testPacket.copyInto(buffer)
          testPacket.size
        }

        1 -> {
          expectedResponse.copyInto(buffer)
          expectedResponse.size
        }

        else -> 0
      }
    }

    // Act
    val response = echoPort.sendPacket(testPacket)

    // Assert
    response.fold(
      { assertTrue(false) },
      { assertArrayEquals(expectedResponse, it) }
    )
    verify(exactly = 3) {
      mockPort.readBytes(any(), any())
    }
  }

  @Test
  fun `test timeout exception`() = runBlocking {
    // Arrange
    val testPacket = byteArrayOf(1, 2, 3)
    every { mockPort.bytesAvailable() } returns 0
    every { mockPort.writeBytes(any(), any()) } returns testPacket.size

    // Act & Assert
    serialPort.sendPacket(testPacket).fold(
      { assertEquals(SerialError.TimeoutError("Timeout while waiting for response"), it) },
      { assertTrue(false) }
    )
  }
}