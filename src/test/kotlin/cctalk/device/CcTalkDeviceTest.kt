package cctalk.device

import be.inotek.communication.CcTalkChecksumTypes
import cctalk.CcTalkError
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
      245 to TestCcTalkPort.createStringResponse("Coin Acceptor")
    )
    val testPort = TestCcTalkPort(
      deviceAddress = 2,
      responses = responses
    )
    val device = CcTalkDevice(
      port = testPort,
      address = 2,
      checksumType = CcTalkChecksumTypes.Simple8
    )

    // Act
    val result = device.equipmentCategoryId()

    // Assert
    assertTrue(result.isRight())
    assertEquals("Coin Acceptor", result.getOrNull())

    // Verify correct packet was sent
    val sentPacket = testPort.lastPacketSent!!
    assertEquals(2, sentPacket.destination)
    assertEquals(1, sentPacket.source)
    assertEquals(245, sentPacket.header)
  }

  @Test
  fun `simplePoll should return Ok`() = runBlocking {
    // Arrange
    val responses = mapOf(
      254 to TestCcTalkPort.createLongResponse(0)
    )
    val testPort = TestCcTalkPort(
      deviceAddress = 2,
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
    assertEquals(CcTalkStatus.Ok, result.getOrNull()!!)

    // Verify correct packet was sent
    val sentPacket = testPort.lastPacketSent!!
    assertEquals(2, sentPacket.destination)
    assertEquals(1, sentPacket.source)
    assertEquals(254, sentPacket.header)
  }

  @Test
  fun `simplePoll should handle communication error`() = runBlocking {
    // Arrange
    val testPort = TestCcTalkPort(
      deviceAddress = 2,
      forcedError = CcTalkError.SendError()
    )
    val device = CcTalkDevice(
      port = testPort,
      address = 2,
      checksumType = CcTalkChecksumTypes.Simple8
    )

    // Act
    val result = device.simplePoll()

    // Assert
    assertEquals(CcTalkStatus.SendErr, result.leftOrNull()?.status!!)
  }

  @Test
  fun `multiple commands should track call counts`() = runBlocking {
    // Arrange
    val testPort = TestCcTalkPort(deviceAddress = 2)
    val device = CcTalkDevice(
      port = testPort,
      address = 2,
      checksumType = CcTalkChecksumTypes.Simple8
    )

    // Act
    device.simplePoll()
    device.simplePoll()
    device.equipmentCategoryId()

    // Assert
    assertEquals(2, testPort.getHeaderCallCount(254)) // simplePoll called twice
    assertEquals(1, testPort.getHeaderCallCount(245)) // deviceCategory called once
  }
}