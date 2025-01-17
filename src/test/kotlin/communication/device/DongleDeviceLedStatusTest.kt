package communication.device

import be.inotek.communication.CcTalkChecksumTypes
import communication.CcTalkStatus
import communication.LedStatus
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream
import kotlin.test.assertEquals

class DongleDeviceLedStatusTest {
  data class LedTestCase(
    val ledNumber: Int,
    val status: LedStatus,
    val time: Int,
    val expectedStatus: CcTalkStatus,
    val description: String
  ) {
    override fun toString(): String {
      return description
    }
  }

  companion object {
    @JvmStatic
    fun validationTestCases(): Stream<LedTestCase> = Stream.of(
      // LED number validation
      LedTestCase(
        ledNumber = -1,
        status = LedStatus.ON,
        time = 0,
        expectedStatus = CcTalkStatus.WrongParameter,
        description = "LED number below minimum"
      ),
      LedTestCase(
        ledNumber = 8,
        status = LedStatus.ON,
        time = 0,
        expectedStatus = CcTalkStatus.WrongParameter,
        description = "LED number above maximum"
      ),

      // Blink time validation - only applies when status is BLINK
      LedTestCase(
        ledNumber = 0,
        status = LedStatus.BLINK,
        time = 49,
        expectedStatus = CcTalkStatus.WrongParameter,
        description = "Blink time below minimum"
      ),
      LedTestCase(
        ledNumber = 0,
        status = LedStatus.BLINK,
        time = 12501,
        expectedStatus = CcTalkStatus.WrongParameter,
        description = "Blink time above maximum"
      ),

      // Valid cases for each status
      LedTestCase(
        ledNumber = 0,
        status = LedStatus.ON,
        time = 0,
        expectedStatus = CcTalkStatus.Ok,
        description = "Valid ON state"
      ),
      LedTestCase(
        ledNumber = 7,
        status = LedStatus.OFF,
        time = 0,
        expectedStatus = CcTalkStatus.Ok,
        description = "Valid OFF state"
      ),
      LedTestCase(
        ledNumber = 3,
        status = LedStatus.BLINK,
        time = 50,
        expectedStatus = CcTalkStatus.Ok,
        description = "Valid BLINK state minimum time"
      ),
      LedTestCase(
        ledNumber = 3,
        status = LedStatus.BLINK,
        time = 12500,
        expectedStatus = CcTalkStatus.Ok,
        description = "Valid BLINK state maximum time"
      ),

      // Edge cases
      LedTestCase(
        ledNumber = 0,
        status = LedStatus.OFF,
        time = -1,
        expectedStatus = CcTalkStatus.Ok,
        description = "Invalid time ignored for OFF state"
      ),
      LedTestCase(
        ledNumber = 0,
        status = LedStatus.ON,
        time = 999999,
        expectedStatus = CcTalkStatus.Ok,
        description = "Invalid time ignored for ON state"
      )
    )
  }


  @OptIn(ExperimentalUnsignedTypes::class)
  @ParameterizedTest
  @MethodSource("validationTestCases")
  fun `test setLedState validation`(testCase: LedTestCase) = runBlocking {
    // Arrange
    val testPort = TestCcTalkPort(deviceAddress = 2u)
    val device = DongleDevice(
      port = testPort,
      address = 2,
      checksumType = CcTalkChecksumTypes.Simple8
    )

    // Act
    val result = device.setLedState(
      ledNumber = testCase.ledNumber,
      status = testCase.status,
      time = testCase.time
    )

    // Assert
    assertEquals(
      testCase.expectedStatus,
      result,
      "Failed for case: ${testCase.description}"
    )

    // Verify packet was only sent for valid cases
    if (testCase.expectedStatus == CcTalkStatus.Ok) {
      val sentPacket = testPort.lastPacketSent!!
      assertEquals(2u, sentPacket.destination)
      assertEquals(1u, sentPacket.source)

      // Verify correct header based on status
      val expectedHeader = when (testCase.status) {
        LedStatus.ON, LedStatus.OFF -> 107u
        LedStatus.BLINK -> 108u
      }
      assertEquals(expectedHeader.toUByte(), sentPacket.header)

      // Verify payload
      val data = sentPacket.data
      assertEquals(2, data.size)
      assertEquals(testCase.ledNumber.toUByte(), data[0])

      val expectedPayload = when (testCase.status) {
        LedStatus.OFF -> 1u
        LedStatus.ON -> 0u
        LedStatus.BLINK -> (testCase.time / 50).toUByte()
      }
      assertEquals(expectedPayload, data[1])
    }
  }
}