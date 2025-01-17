package cctalk.device.hopper

import arrow.core.Either
import be.inotek.communication.CcTalkChecksumTypes
import be.inotek.communication.packet.CcTalkPacket
import cctalk.CcTalkStatus
import cctalk.device.PayoutDevice
import cctalk.device.PayoutDevice.PayoutMode
import cctalk.device.TestCcTalkPort
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalUnsignedTypes::class)
class PayoutDeviceGetCoinIdTest {

  data class CoinIdTestCase(
    val coinNumber: Int,
    val responseData: UByteArray?,
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
        responseData = ubyteArrayOf(1u, 2u, 3u), // Too short
        expectedResult = Either.Left(CcTalkStatus.DataFormat),
        description = "Response data too short"
      ),

      // Valid cases
      CoinIdTestCase(
        coinNumber = 0,
        responseData = "EUR 1.00".toByteArray().map { it.toUByte() }.toUByteArray() + ubyteArrayOf(1u, 0u),
        expectedResult = Either.Right(PayoutDevice.CoinId("1", "EUR 1.00")),
        description = "Valid coin ID for EUR 1.00"
      ),
      CoinIdTestCase(
        coinNumber = 1,
        responseData = "A TOKEN".toByteArray().map { it.toUByte() }.toUByteArray() + ubyteArrayOf(2u, 0u),
        expectedResult = Either.Right(PayoutDevice.CoinId("2", "A TOKEN")),
        description = "Valid coin ID for TOKEN"
      ),
      CoinIdTestCase(
        coinNumber = 15,
        responseData = "USD 0.25".toByteArray().map { it.toUByte() }.toUByteArray() + ubyteArrayOf(255u, 255u),
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
      mapOf((131).toUByte() to { packet: CcTalkPacket ->
        Either.Right(CcTalkPacket.build {
          destination(1u)
          source(3u)
          header(0u)
          data(testCase.responseData)
          checksumType(packet.checksumType)
        })
      })
    } else {
      emptyMap()
    }

    val testPort = TestCcTalkPort(
      deviceAddress = 3u,
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
          (testCase.expectedResult as Either.Left).value,
          result.value,
          "Expected error ${testCase.expectedResult.value} but got ${result.value}"
        )
      }

      is Either.Right -> {
        assertTrue(testCase.expectedResult is Either.Right)
        assertEquals(
          (testCase.expectedResult as Either.Right).value,
          result.value,
          "Expected coin ID ${testCase.expectedResult.value} but got ${result.value}"
        )
      }
    }

    // Verify packet was only sent for valid coin numbers
    if (testCase.coinNumber in 0..15) {
      val sentPacket = testPort.lastPacketSent!!
      assertEquals(3u, sentPacket.destination)
      assertEquals(1u, sentPacket.source)
      assertEquals(131u, sentPacket.header)
      assertEquals(1, sentPacket.data.size)
      assertEquals((testCase.coinNumber + 1).toUByte(), sentPacket.data[0])
    }
  }

  @Test
  fun `test device initialization with default values`() = runBlocking {
    // Arrange
    val testPort = TestCcTalkPort(deviceAddress = 3u)

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
    val testPort = TestCcTalkPort(deviceAddress = 3u)

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