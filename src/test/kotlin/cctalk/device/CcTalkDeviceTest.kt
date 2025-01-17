package cctalk.device

import be.inotek.communication.CcTalkChecksumTypes
import cctalk.CcTalkStatus
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CcTalkDeviceTest {
  @Test
  fun `deviceCategory should return correct category`() = runBlocking {
    // Arrange
    val responses = mapOf(
      248.toUByte() to TestCcTalkPort.createStringResponse("Coin Acceptor")
    )
    val testPort = TestCcTalkPort(
      deviceAddress = 2u,
      responses = responses
    )
    val device = CcTalkDevice(
      port = testPort,
      address = 2,
      checksumType = CcTalkChecksumTypes.Simple8
    )

    // Act
    val result = device.deviceCategory()

    // Assert
    assertTrue(result.isRight())
    assertEquals("Coin Acceptor", result.getOrNull())

    // Verify correct packet was sent
    val sentPacket = testPort.lastPacketSent!!
    assertEquals(2u, sentPacket.destination)
    assertEquals(1u, sentPacket.source)
    assertEquals(248u, sentPacket.header)
  }

  @Test
  fun `simplePoll should return Ok`() = runBlocking {
    // Arrange
    val responses = mapOf(
      254.toUByte() to TestCcTalkPort.createLongResponse(0)
    )
    val testPort = TestCcTalkPort(
      deviceAddress = 2u,
      responses = responses
    )
    val device = CcTalkDevice(
      port = testPort,
      address = 2,
      checksumType = CcTalkChecksumTypes.Simple8
    )

    // Act
    val result = device.simplePoll()

    // Assert
    assertEquals(CcTalkStatus.Ok, result)

    // Verify correct packet was sent
    val sentPacket = testPort.lastPacketSent!!
    assertEquals(2u, sentPacket.destination)
    assertEquals(1u, sentPacket.source)
    assertEquals(254u, sentPacket.header)
  }

  @Test
  fun `simplePoll should handle communication error`() = runBlocking {
    // Arrange
    val testPort = TestCcTalkPort(
      deviceAddress = 2u,
      forcedError = CcTalkStatus.SendErr
    )
    val device = CcTalkDevice(
      port = testPort,
      address = 2,
      checksumType = CcTalkChecksumTypes.Simple8
    )

    // Act
    val result = device.simplePoll()

    // Assert
    assertEquals(CcTalkStatus.SendErr, result)
  }

  @Test
  fun `multiple commands should track call counts`() = runBlocking {
    // Arrange
    val testPort = TestCcTalkPort(deviceAddress = 2u)
    val device = CcTalkDevice(
      port = testPort,
      address = 2,
      checksumType = CcTalkChecksumTypes.Simple8
    )

    // Act
    device.simplePoll()
    device.simplePoll()
    device.deviceCategory()

    // Assert
    assertEquals(2, testPort.getHeaderCallCount(254u)) // simplePoll called twice
    assertEquals(1, testPort.getHeaderCallCount(248u)) // deviceCategory called once
  }
}