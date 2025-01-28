package cctalk.device.hopper

import arrow.core.Either
import be.inotek.communication.CcTalkChecksumTypes
import cctalk.CcTalkStatus
import cctalk.device.PayoutDevice
import cctalk.device.PayoutDevice.PayoutMode
import cctalk.device.TestCcTalkPort
import cctalk.packet.CcTalkPacket
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PayoutDeviceGetCoinIdTest {

  data class CoinIdTestCase(
    val coinNumber: Int,
    val responseData: ByteArray?,
    val expectedResult: Either<CcTalkStatus, PayoutDevice.CoinId>,
    val description: String
  ) {
    override fun toString(): String {
      return description
    }
  }

  companion object {
    @JvmStatic
    fun coinIdTestCases(): Stream<CoinIdTestCase> = Stream.of(
      // Parameter validation
      CoinIdTestCase(
        coinNumber = -1,
        responseData = null,
        expectedResult = Either.Left(CcTalkStatus.WrongParameter),
        description = "Coin number below minimum"
      ),
      CoinIdTestCase(
        coinNumber = 16,
        responseData = null,
        expectedResult = Either.Left(CcTalkStatus.WrongParameter),
        description = "Coin number above maximum"
      ),

      // Response format validation
      CoinIdTestCase(
        coinNumber = 0,
        responseData = byteArrayOf(1, 2, 3), // Too short
        expectedResult = Either.Left(CcTalkStatus.DataFormat),
        description = "Response data too short"
      ),

      // Valid cases
      CoinIdTestCase(
        coinNumber = 0,
        responseData = "EUR 1.00".toByteArray() + byteArrayOf(1, 0),
        expectedResult = Either.Right(PayoutDevice.CoinId("1", "EUR 1.00")),
        description = "Valid coin ID for EUR 1.00"
      ),
      CoinIdTestCase(
        coinNumber = 1,
        responseData = "A TOKEN".toByteArray() + byteArrayOf(2, 0),
        expectedResult = Either.Right(PayoutDevice.CoinId("2", "A TOKEN")),
        description = "Valid coin ID for TOKEN"
      ),
      CoinIdTestCase(
        coinNumber = 15,
        responseData = "USD 0.25".toByteArray() + byteArrayOf(255.toByte(), 255.toByte()),
        expectedResult = Either.Right(PayoutDevice.CoinId("65535", "USD 0.25")),
        description = "Valid coin ID with maximum value"
      )
    )
  }

  @ParameterizedTest
  @MethodSource("coinIdTestCases")
  fun `test getHopperCoinId`(testCase: CoinIdTestCase) = runBlocking {
    // Arrange
    val responses = if (testCase.responseData != null) {
      mapOf(131 to { packet: CcTalkPacket ->
        Either.Right(CcTalkPacket.build {
          destination(1)
          source(3)
          header(0)
          data(testCase.responseData)
          checksumType(packet.checksumType)
        })
      })
    } else {
      emptyMap()
    }

    val testPort = TestCcTalkPort(
      deviceAddress = 3,
      responses = responses
    )

    val device = PayoutDevice(
      port = testPort,
      address = 3,
      checksumType = CcTalkChecksumTypes.Simple8,
      payoutMode = PayoutMode.SerialNumber
    )

    // Act
    val result = device.getHopperCoinId(testCase.coinNumber)

    // Assert
    when (result) {
      is Either.Left -> {
        assertTrue(testCase.expectedResult is Either.Left)
        assertEquals(
          testCase.expectedResult.value,
          result.value.status,
          "Expected error ${testCase.expectedResult.value} but got ${result.value}"
        )
      }

      is Either.Right -> {
        assertTrue(testCase.expectedResult is Either.Right)
        assertEquals(
          testCase.expectedResult.value,
          result.value,
          "Expected coin ID ${testCase.expectedResult.value} but got ${result.value}"
        )
      }
    }

    // Verify packet was only sent for valid coin numbers
    if (testCase.coinNumber in 0..15) {
      val sentPacket = testPort.lastPacketSent!!
      assertEquals(3, sentPacket.destination)
      assertEquals(1, sentPacket.source)
      assertEquals(131, sentPacket.header)
      assertEquals(1, sentPacket.data.size)
      assertEquals(testCase.coinNumber + 1, sentPacket.data[0])
    }
  }

  @Test
  fun `test device initialization with default values`() = runBlocking {
    // Arrange
    val testPort = TestCcTalkPort(deviceAddress = 3)

    // Act
    val device = PayoutDevice(
      port = testPort,
      checksumType = CcTalkChecksumTypes.Simple8
    )

    // Assert
    assertEquals(
      PayoutMode.SerialNumber,
      device::class.java.getDeclaredField("payoutMode").apply { isAccessible = true }.get(device)
    )
    assertEquals(false, device.multiCoin)
    assertEquals(0, device.coinTypes)
  }

  @Test
  fun `test device initialization with custom values`() = runBlocking {
    // Arrange
    val testPort = TestCcTalkPort(deviceAddress = 3)

    // Act
    val device = PayoutDevice(
      port = testPort,
      address = 4,
      checksumType = CcTalkChecksumTypes.CRC16,
      payoutMode = PayoutMode.NoEncryption
    )

    // Assert
    assertEquals(
      PayoutMode.NoEncryption,
      device::class.java.getDeclaredField("payoutMode").apply { isAccessible = true }.get(device)
    )
    assertEquals(false, device.multiCoin)
    assertEquals(0, device.coinTypes)
  }
}