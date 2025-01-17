package cctalk.device

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import be.inotek.communication.CcTalkChecksumTypes
import cctalk.CcTalkStatus
import cctalk.currency.CoinValue
import cctalk.currency.Currency
import cctalk.currency.ValueFactor
import cctalk.payout.*
import cctalk.serial.CcTalkPort
import java.util.*
import kotlin.math.pow

@OptIn(ExperimentalUnsignedTypes::class)
class PayoutDevice(
  port: CcTalkPort,
  address: Byte = 3,
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

  suspend fun payoutStatus(): Either<CcTalkStatus, PayoutStatus> {
    // Same thing but without event count
    return extendedPayoutStatus()
      .map { it.toPayoutStatus() }
  }

  suspend fun extendedPayoutStatus(): Either<CcTalkStatus, PayoutStatusExtended> {
    // Get hopper status
    var statuses = EnumSet.noneOf<PayoutStatusFlag>(PayoutStatusFlag::class.java)
    val hopperStatus = getHopperStatusTest()
    if (hopperStatus.isLeft()) return (hopperStatus.leftOrNull() ?: CcTalkStatus.Unknown).left()
    hopperStatus.getOrNull()?.forEach { statuses::add }

    // Retrieve event data
    val eventsDataEither = talkCc { header(166u) }
    if (eventsDataEither.hasError()) return eventsDataEither.error().left()
    val eventsData = eventsDataEither.packet()
    val eventsDataLength = eventsData.dataLength.toInt()
    if (eventsDataLength < 4) return CcTalkStatus.DataFormat.left()
    if (eventsData.data[0].toInt() == 0) statuses.add(PayoutStatusFlag.Reset)
    var events = eventsData.data[0].toInt()
    var remaining = eventsData.data[1].toInt()
    var lastPayout = eventsData.data[2].toInt()
    var lastUnpaid = eventsData.data[3].toInt()

    // Perform sensor level check
    val sensorLevelsEither = getHopperSensors()
    if (sensorLevelsEither.isLeft()) return (sensorLevelsEither.leftOrNull() ?: CcTalkStatus.Unknown).left()
    var (lowLevel, highLevel) = sensorLevelsEither.getOrNull() ?: HopperSensorLevels()

    return PayoutStatusExtended(
      event = events,
      status = statuses,
      remaining = remaining,
      lastPayout = lastPayout,
      lastUnpaid = lastUnpaid,
      lowLevelSensorStatus = lowLevel,
      highLevelSensorStatus = highLevel,
    ).right()
  }

  suspend fun whPayoutStatus(): Either<CcTalkStatus, WhPayoutStatus> {
    // It's getting the event data but differently???
    val statusEither = talkCc { header(133u) }
    if (statusEither.isLeft()) return (statusEither.leftOrNull() ?: CcTalkStatus.Unknown).left()

    val status = statusEither.getOrNull()!!
    val dataLength = status.dataLength.toInt()

    if (dataLength < 3) return CcTalkStatus.DataFormat.left()
    var statuses = EnumSet.noneOf<PayoutStatusFlag>(PayoutStatusFlag::class.java)
    if (status.data[0].toInt() == 0) statuses and PayoutStatusFlag.Reset
    var remaining = 0.0
    isPurging = status.data[1].toInt() == 255 && status.data[2].toInt() == 255
    if (!isPurging) remaining = (status.data[1].toDouble() + 256 * status.data[2].toDouble()) / 100.0
    var lastPayout = (status.data[3].toDouble() + 256 * status.data[4].toDouble()) / 100.0
    var lastUnpaid = (status.data[5].toDouble() + 256 * status.data[6].toDouble()) / 100.0

    if (isPurging) return WhPayoutStatus(
      status = EnumSet.of<PayoutStatusFlag>(PayoutStatusFlag.Purging),
      remaining = remaining,
      lastPayout = lastPayout,
      lastUnpaid = lastUnpaid,
    ).right()

    // Perform status check
    val hopperStatus = getHopperStatusTest()
    if (hopperStatus.isLeft()) return (hopperStatus.leftOrNull() ?: CcTalkStatus.Unknown).left()
    hopperStatus.getOrNull()?.forEach { statuses::add }

    // Perform sensor level check
    val sensorLevelsEither = getHopperSensors()
    if (sensorLevelsEither.isLeft()) return (sensorLevelsEither.leftOrNull() ?: CcTalkStatus.Unknown).left()
    var (lowLevel, highLevel) = sensorLevelsEither.getOrNull() ?: HopperSensorLevels()

    return WhPayoutStatus(
      status = statuses,
      remaining = remaining,
      lastPayout = lastPayout,
      lastUnpaid = lastUnpaid,
      lowLevelSensorStatus = lowLevel,
      highLevelSensorStatus = highLevel
    ).right()
  }

  private suspend fun getHopperSensors(): Either<CcTalkStatus, HopperSensorLevels> {
    val levelSensorEither = talkCc { header(217u) }
    if (levelSensorEither.isLeft()) return (levelSensorEither.leftOrNull() ?: CcTalkStatus.Unknown).left()
    val levelSensor = levelSensorEither.getOrNull()!!
    val levelSensorDataLength = levelSensor.dataLength.toInt()
    if (levelSensorDataLength < 0) return CcTalkStatus.DataFormat.left()

    var lowLevelSensorStatus = computeSensorStatus(levelSensor.data[0], isHighLevel = false)
    var highLevelSensorStatus = computeSensorStatus(levelSensor.data[0], isHighLevel = true)
    return HopperSensorLevels(lowLevelSensorStatus, highLevelSensorStatus).right()
  }

  private fun computeSensorStatus(value: UByte, isHighLevel: Boolean): PayoutSensorStatus {
    val supportedFlag = if (isHighLevel) 0x20 else 0x10
    val isTriggeredFlag = if (isHighLevel) 0x02 else 0x01

    if ((value.toInt() and supportedFlag) == 0) return PayoutSensorStatus.NotSupported
    return if ((value.toInt() and isTriggeredFlag) == 0) PayoutSensorStatus.Triggered else PayoutSensorStatus.Untriggered
  }

  private suspend fun getHopperStatusTest(): Either<CcTalkStatus, EnumSet<PayoutStatusFlag>> {
    val statusTestEither = talkCc { header(163u) }
    if (statusTestEither.hasError()) return statusTestEither.error().left()

    val packet = statusTestEither.packet()
    val dataLength = packet.dataLength.toInt()
    if (dataLength == 0) return CcTalkStatus.DataFormat.left()

    var statuses = EnumSet.noneOf<PayoutStatusFlag>(PayoutStatusFlag::class.java)
    packet.data
      .mapIndexed { index: Int, value: UByte -> value.toLong() shl (index * 8) }
      .map { PayoutStatusFlag::fromCode }
      .forEach { statuses::add }

    return statuses.right()
  }

  suspend fun setPayoutEnabled(enabled: Boolean): CcTalkStatus {
    return talkCc {
      header(164u)
      data(ubyteArrayOf(if (enabled) 165u else 0u))
    }.fold(
      { return it },
      { return CcTalkStatus.Ok }
    )
  }

  suspend fun payout(coins: Int): CcTalkStatus {
    if (coins !in 1..255) return CcTalkStatus.WrongParameter
    if (multiCoin) return CcTalkStatus.UnSupported

    return when (payoutMode) {
      PayoutMode.SerialNumber -> handleSerialNumberPayout(coins)
      PayoutMode.NoEncryption -> handleNoEncryptionPayout(coins)
      PayoutMode.Encrypted -> CcTalkStatus.UnSupported
    }
  }

  private suspend fun handleSerialNumberPayout(coins: Int): CcTalkStatus {
    return talkCc { header(242u) }
      .fold(
        { return it },
        {
          if (it.dataLength.toInt() < 3) {
            return CcTalkStatus.DataFormat
          }

          val payload = ubyteArrayOf(it.data[0], it.data[1], it.data[2], coins.toUByte())
          return talkCc { header(167u); data(payload) }
            .fold(
              { return it },
              { return CcTalkStatus.Ok }
            )
        }
      )
  }

  private suspend fun handleNoEncryptionPayout(coins: Int): CcTalkStatus {
    val pumpRngEither = talkCc { header(161u); data(UByteArray(8) { 0u }) }
    if (pumpRngEither.isLeft()) return pumpRngEither.leftOrNull()!!
    val requestCipherKey = talkCc { header(160u) }
    if (requestCipherKey.isLeft()) return requestCipherKey.leftOrNull()!!
    val payout = talkCc { header(167u); data(UByteArray(9) { 0u }.also { it[8] = coins.toUByte() }) }
    if (payout.isLeft()) return payout.leftOrNull()!!
    return CcTalkStatus.Ok
  }

  suspend fun emergencyStop(): Either<CcTalkStatus, Int> {
    return talkCc { header(172u) }
      .fold(
        { return it.left() },
        { return if (it.dataLength.toInt() > 0) it.data[0].toInt().right() else 0.right() }
      )
  }

  suspend fun purge(): CcTalkStatus {
    if (multiCoin) return CcTalkStatus.UnSupported

    return talkCc {
      header(121u)
      data(ubyteArrayOf(0u))
    }.fold(
      { return it },
      { return CcTalkStatus.Ok }
    )
  }

  suspend fun getAllHopperCoinValue(ignoreCache: Boolean = false): Either<CcTalkStatus, List<CoinValue>> {
    if (coinValues.isNotEmpty() && !ignoreCache) {
      return Either.Right(coinValues)
    }

    coinTypes = 0
    coinValues.clear()
    for (i in 0 until 16) {
      val result = getHopperCoinValue(i).fold(
        { return it.left() },
        {
          coinValues.add(it)
          coinTypes++
          return CcTalkStatus.Ok.left()
        }
      )
      if (result != CcTalkStatus.Ok.left()) {
        return result.left()
      }
    }
    multiCoin = coinTypes > 1
    return coinValues.right()
  }

  suspend fun getHopperCoinValue(coinNumber: Int): Either<CcTalkStatus, CoinValue> {
    return getHopperCoinId(coinNumber)
      .fold(
        { Either.Left(it) },
        {
          if (it.name.length < 6) {
            return Either.Left(CcTalkStatus.DataFormat)
          }

          val id = it.name.substring(0, 2)
          var factorChar = ' '
          val valueString = buildString {
            for (i in 2 until 5) {
              val currentChar = it.name[i]
              if (currentChar.isDigit()) {
                append(currentChar)
              } else {
                append('.')
                factorChar = currentChar
              }
            }
          }

          val currency = Currency.Search.byCcTalkID(id)
          if (currency == null) {
            return Either.Left(CcTalkStatus.DataFormat)
          }

          val coinValue = try {
            valueString.toDouble() / 10.0.pow(currency.decimals.toDouble())
          } catch (e: NumberFormatException) {
            return Either.Left(CcTalkStatus.DataFormat)
          }

          val factor = ValueFactor.fromChar(factorChar) ?: ValueFactor(' ', 1.0)
          return Either.Right(CoinValue(it.id, coinValue * factor.face, currency.decimals))
        }
      )
  }

  suspend fun getHopperCoinId(coinNumber: Int): Either<CcTalkStatus, CoinId> {
    if (coinNumber !in 0..15) {
      return Either.Left(CcTalkStatus.WrongParameter)
    }

    return talkCc {
      header(131u)
      destination(address.toUByte())
      source(CcTalkCommand.SOURCE_ADDRESS)
      data(ubyteArrayOf((coinNumber + 1).toUByte()))
      checksumType(checksumType)
    }.fold(
      { error -> Either.Left(error) },
      {
        val dataLength = it.dataLength.toInt()
        if (dataLength < 8) return Either.Left(CcTalkStatus.DataFormat)

        val coinName = buildString {
          it.data
            .dropLast(2)
            .map { it.toInt().toChar() }
            .forEach { append(it) }
        }
        val coinId = it.data[dataLength - 2] + 256u * it.data[dataLength - 1]
        return Either.Right(CoinId(coinId.toString(), coinName.toString()))
      }
    )
  }
}