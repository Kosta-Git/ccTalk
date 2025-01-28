package cctalk.device

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.right
import be.inotek.communication.CcTalkChecksumTypes
import cctalk.CcTalkError
import cctalk.CcTalkStatus
import cctalk.currency.CoinValue
import cctalk.currency.Currency
import cctalk.currency.ValueFactor
import cctalk.device.CcTalkDevice.CcTalkCommand.HOPPER_ENABLE
import cctalk.device.CcTalkDevice.CcTalkCommand.REQUEST_HOPPER_CIPHER_KEY
import cctalk.device.CcTalkDevice.CcTalkCommand.REQUEST_HOPPER_COIN_ID
import cctalk.device.CcTalkDevice.CcTalkCommand.REQUEST_HOPPER_DISPENSE
import cctalk.device.CcTalkDevice.CcTalkCommand.REQUEST_HOPPER_EMERGENCY_STOP
import cctalk.device.CcTalkDevice.CcTalkCommand.REQUEST_HOPPER_LEVEL_STATUS
import cctalk.device.CcTalkDevice.CcTalkCommand.REQUEST_HOPPER_PUMP_RNG
import cctalk.device.CcTalkDevice.CcTalkCommand.REQUEST_HOPPER_PURGE
import cctalk.device.CcTalkDevice.CcTalkCommand.REQUEST_HOPPER_STATUS
import cctalk.device.CcTalkDevice.CcTalkCommand.REQUEST_HOPPER_TEST
import cctalk.device.CcTalkDevice.CcTalkCommand.REQUEST_SERIAL_NUMBER
import cctalk.payout.*
import cctalk.serial.CcTalkPort
import java.util.*
import kotlin.math.pow

class PayoutDevice(
  port: CcTalkPort,
  address: Int = 3,
  checksumType: CcTalkChecksumTypes = CcTalkChecksumTypes.Simple8,
  private var payoutMode: PayoutMode = PayoutMode.SerialNumber,
) : CcTalkDevice(port, address, checksumType) {
  data class CoinId(val id: String, val name: String)

  data class HopperSensorLevels(
    val lowLevel: PayoutSensorStatus = PayoutSensorStatus.Unknown,
    val highLevel: PayoutSensorStatus = PayoutSensorStatus.Unknown
  )

  enum class PayoutMode {
    // Use serial number to verify payout command.
    SerialNumber,

    // Use dummy key verify payout command.
    NoEncryption,

    // Use encrypted key to verify payout command.
    Encrypted, // Currently not supported
  }

  var multiCoin: Boolean = false
    private set

  var coinTypes: Int = 0
    private set

  private var isPurging: Boolean = false
  private var coinValues: MutableList<CoinValue> = mutableListOf()

  fun isCurrentlyPurging(): Boolean = isPurging && multiCoin

  // Same thing but without event count
  suspend fun payoutStatus(): Either<CcTalkError, PayoutStatus> = extendedPayoutStatus().map { it.toPayoutStatus() }

  suspend fun extendedPayoutStatus(): Either<CcTalkError, PayoutStatusExtended> = either {
    // Get hopper status
    var statuses = EnumSet.noneOf<PayoutStatusFlag>(PayoutStatusFlag::class.java)
    getHopperStatusTest().bind().forEach { statuses::add }

    // Retrieve event data
    val eventsData = talkCc {
      header(REQUEST_HOPPER_STATUS)
      withDefaults(this@PayoutDevice)
    }.bind()
    ensure(eventsData.dataLength.toInt() >= 4) { CcTalkError.DataFormatError(4, eventsData.dataLength.toInt()) }
    if (eventsData.data[0].toInt() == 0) statuses.add(PayoutStatusFlag.Reset)
    val (events, remaining, lastPayout, lastUnpaid) = eventsData.data.map { it.toInt() }

    // Perform sensor level check
    var (lowLevel, highLevel) = getHopperSensors().bind()

    PayoutStatusExtended(
      event = events,
      status = statuses,
      remaining = remaining,
      lastPayout = lastPayout,
      lastUnpaid = lastUnpaid,
      lowLevelSensorStatus = lowLevel,
      highLevelSensorStatus = highLevel,
    )
  }

  suspend fun whPayoutStatus(): Either<CcTalkError, WhPayoutStatus> = either {
    // It's getting the event data but differently???
    val status = talkCc {
      header(133u)
      withDefaults(this@PayoutDevice)
    }.bind()
    val dataLength = status.dataLength.toInt()
    ensure(dataLength >= 3) { CcTalkError.DataFormatError(3, dataLength) }

    var statuses = EnumSet.noneOf<PayoutStatusFlag>(PayoutStatusFlag::class.java)
    if (status.data[0].toInt() == 0) statuses and PayoutStatusFlag.Reset
    var remaining = 0.0
    isPurging = status.data[1].toInt() == 255 && status.data[2].toInt() == 255
    if (!isPurging) remaining = (status.data[1].toDouble() + 256 * status.data[2].toDouble()) / 100.0
    val lastPayout = (status.data[3].toDouble() + 256 * status.data[4].toDouble()) / 100.0
    val lastUnpaid = (status.data[5].toDouble() + 256 * status.data[6].toDouble()) / 100.0

    if (isPurging) return WhPayoutStatus(
      status = EnumSet.of<PayoutStatusFlag>(PayoutStatusFlag.Purging),
      remaining = remaining,
      lastPayout = lastPayout,
      lastUnpaid = lastUnpaid,
    ).right()

    // Perform status check
    getHopperStatusTest().bind().forEach { statuses::add }

    // Perform sensor level check
    var (lowLevel, highLevel) = getHopperSensors().bind()

    WhPayoutStatus(
      status = statuses,
      remaining = remaining,
      lastPayout = lastPayout,
      lastUnpaid = lastUnpaid,
      lowLevelSensorStatus = lowLevel,
      highLevelSensorStatus = highLevel
    )
  }

  private suspend fun getHopperSensors(): Either<CcTalkError, HopperSensorLevels> = either {
    val levelSensor = talkCc {
      header(REQUEST_HOPPER_LEVEL_STATUS)
      withDefaults(this@PayoutDevice)
    }.bind()
    ensure(levelSensor.dataLength.toInt() >= 1) { CcTalkError.DataLengthError(1, 255, levelSensor.dataLength.toInt()) }

    HopperSensorLevels(
      lowLevel = computeSensorStatus(levelSensor.data[0], isHighLevel = false),
      highLevel = computeSensorStatus(levelSensor.data[0], isHighLevel = true)
    )
  }

  // TODO: The flags do not look correct
  private fun computeSensorStatus(value: Int, isHighLevel: Boolean): PayoutSensorStatus {
    val supportedFlag = if (isHighLevel) 0x20 else 0x10
    val isTriggeredFlag = if (isHighLevel) 0x02 else 0x01

    if ((value.toInt() and supportedFlag) == 0) return PayoutSensorStatus.NotSupported
    return if ((value.toInt() and isTriggeredFlag) == 0) PayoutSensorStatus.Triggered else PayoutSensorStatus.Untriggered
  }

  private suspend fun getHopperStatusTest(): Either<CcTalkError, EnumSet<PayoutStatusFlag>> = either {
    val packet = talkCc {
      header(REQUEST_HOPPER_TEST)
      withDefaults(this@PayoutDevice)
    }.bind()
    ensure(packet.dataLength.toInt() >= 1) { CcTalkError.DataLengthError(1, 255, packet.dataLength.toInt()) }

    var statuses = EnumSet.noneOf<PayoutStatusFlag>(PayoutStatusFlag::class.java)
    packet.data
      .mapIndexed { index: Int, value: Int -> value.toLong() shl (index * 8) }
      .map { PayoutStatusFlag::fromCode }
      .forEach { statuses::add }

    statuses
  }

  suspend fun setPayoutEnabled(enabled: Boolean):  Either<CcTalkError, CcTalkStatus> = either {
    talkCc {
      header(HOPPER_ENABLE)
      data(byteArrayOf(if (enabled) 0xA5.toByte() else 0.toByte()))
      withDefaults(this@PayoutDevice)
    }.bind()
    CcTalkStatus.Ok
  }

  suspend fun payout(coins: Int): Either<CcTalkError, CcTalkStatus> = either {
    if (coins !in 1..255) raise(CcTalkError.WrongParameterError("coins must be between 1 and 255, got: $coins"))
    if (multiCoin) raise(CcTalkError.UnsupportedError("payout is not supported in multi coin mode"))

    when (payoutMode) {
      PayoutMode.SerialNumber -> handleSerialNumberPayout(coins).bind()
      PayoutMode.NoEncryption -> handleNoEncryptionPayout(coins).bind()
      PayoutMode.Encrypted -> raise(CcTalkError.UnsupportedError("encrypted payout is not supported"))
    }
    CcTalkStatus.Ok
  }

  private suspend fun handleSerialNumberPayout(coins: Int): Either<CcTalkError, CcTalkStatus> = either {
    // Request Serial number
    talkCc {
      header(REQUEST_SERIAL_NUMBER)
      withDefaults(this@PayoutDevice)
    }
      .bind()
      .let {
        ensure(it.dataLength.toInt() >= 3) { CcTalkError.DataLengthError(3, 255, it.dataLength.toInt()) }
        talkCc {
          // Payout with serial number
          header(REQUEST_HOPPER_DISPENSE)
          data(intArrayOf(it.data[0], it.data[1], it.data[2], coins))
          withDefaults(this@PayoutDevice)
        }.bind()
      }
    CcTalkStatus.Ok
  }

  private suspend fun handleNoEncryptionPayout(coins: Int): Either<CcTalkError, CcTalkStatus> = either {
    // Pump RNG
    talkCc {
      header(REQUEST_HOPPER_PUMP_RNG)
      data(ByteArray(8) { 0 })
      withDefaults(this@PayoutDevice)
    }.bind()
    // Request Cipher Key
    talkCc {
      header(REQUEST_HOPPER_CIPHER_KEY)
      withDefaults(this@PayoutDevice)
    }.bind()
    // Payout
    talkCc {
      header(REQUEST_HOPPER_DISPENSE)
      data(ByteArray(9) { 0 }.also { it[8] = coins.toByte() })
      withDefaults(this@PayoutDevice)
    }.bind()
    CcTalkStatus.Ok
  }

  suspend fun emergencyStop(): Either<CcTalkError, Int> = either {
    val stop = talkCc {
      header(REQUEST_HOPPER_EMERGENCY_STOP)
      withDefaults(this@PayoutDevice)
    }.bind()
    if (stop.dataLength.toInt() > 0) stop.data[0].toInt() else 0
  }

  suspend fun purge(): Either<CcTalkError, CcTalkStatus> = either {
    if (multiCoin) raise(CcTalkError.UnsupportedError("purge is not supported in multi coin mode"))

    talkCc {
      header(REQUEST_HOPPER_PURGE)
      data(byteArrayOf(0))
      withDefaults(this@PayoutDevice)
    }.bind().let { CcTalkStatus.Ok }
  }

  suspend fun getAllHopperCoinValue(ignoreCache: Boolean = false): Either<CcTalkError, List<CoinValue>> = either {
    if (coinValues.isNotEmpty() && !ignoreCache) {
      return coinValues.right()
    }

    coinTypes = 0
    coinValues.clear()
    for (i in 0 until 16) {
      coinValues.add(getHopperCoinValue(i).bind())
      coinTypes++
    }
    multiCoin = coinTypes > 1
    coinValues
  }

  suspend fun getHopperCoinValue(coinNumber: Int): Either<CcTalkError, CoinValue> = either {
    var coinId = getHopperCoinId(coinNumber).bind()
    ensure(coinId.name.length >= 6) {
      CcTalkError.PayloadError("coin name should be at least 6 char, got: ${coinId.name.length}")
    }

    val id = coinId.name.substring(0, 2)
    var factorChar = ' '
    val valueString = buildString {
      for (i in 2 until 5) {
        val currentChar = coinId.name[i]
        if (currentChar.isDigit()) append(currentChar)
        else {
          append('.')
          factorChar = currentChar
        }
      }
    }

    val currency = Currency.Search.byCcTalkID(id) ?: raise(CcTalkError.PayloadError("unknown currency: $id"))
    val coinValue = try {
      valueString.toDouble() / 10.0.pow(currency.decimals.toDouble())
    } catch (_: NumberFormatException) {
      raise(CcTalkError.PayloadError("invalid coin value: $valueString"))
    }

    val factor = ValueFactor.fromChar(factorChar) ?: ValueFactor(' ', 1.0)
    CoinValue(coinId.id, coinValue * factor.face, currency.decimals)
  }

  suspend fun getHopperCoinId(coinNumber: Int): Either<CcTalkError, CoinId> = either {
    if (coinNumber !in 0..15)
      raise(CcTalkError.WrongParameterError("coinNumber must be between 0 and 15, got: $coinNumber"))

    talkCc {
      header(REQUEST_HOPPER_COIN_ID)
      data(byteArrayOf(((coinNumber + 1).toByte())))
      withDefaults(this@PayoutDevice)
    }.bind()
      .let {
        val dataLength = it.dataLength.toInt()
        if (dataLength < 8) raise(CcTalkError.DataFormatError(8, dataLength))

        val coinName = buildString {
          it.data
            .dropLast(2)
            .map { it.toInt().toChar() }
            .forEach { append(it) }
        }
        val coinId = it.data[dataLength - 2] + 256 * it.data[dataLength - 1]
        CoinId(coinId.toString(), coinName.toString())
      }
  }
}