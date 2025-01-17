package cctalk.payout

import java.util.EnumSet

enum class PayoutStatusFlag(val code: Long) {
  /**
   * Nothing to report.
   */
  Nothing(0x000000L),
  // Register 1
  /**
   * Absolute maximum current exceeded.
   */
  CurrentExceeded(0x000001L),
  /**
   * Payout timeout occurred.
   */
  PayoutTimeout(0x000002L),
  /**
   * Motor reversed to clear a jam.
   */
  MotorReversed(0x000004L),
  /**
   * Opto fraud attempt, path blocked during idle.
   */
  OptoIdleBlocked(0x000008L),
  /**
   * Opto fraud attempt, short-circuit during idle.
   */
  OptoIdleShort(0x000010L),
  /**
   * Opto fraud attempt, blocked during payout.
   */
  OptoPayoutBlocked(0x000020L),
  /**
   * Power-up detected.
   */
  PowerUp(0x000040L),
  /**
   * Payout disabled.
   */
  PayoutDisabled(0x000080L),
  // Register 2
  /**
   * Opto fraud attempt, short-circuit during payout.
   */
  OptoPayoutShort(0x000100L),
  /**
   * Single coin mode.
   */
  SingleCoin(0x000200L),
  /**
   * Use other payout for remaining change.
   */
  UseOtherPayout(0x000400L),
  /**
   * Opto fraud attempt.
   */
  OptFraud(0x000800L),
  /**
   * Motor reverse limit reached.
   */
  ReverseLimit(0x001000L),
  /**
   * Inductive coil fault.
   */
  InductiveCoilFault(0x002000L),
  /**
   * Power fail during non-volatile memory write.
   */
  PowerFail(0x004000L),
  /**
   * PIN number mechanism.
   */
  PinNumber(0x008000L),
  // Register 3
  /**
   * Power down during payout.
   */
  PowerDownPayout(0x010000L),
  /**
   * Unknown coin type paid out.
   */
  UnknownCoin(0x020000L),
  /**
   * PIN number incorrect.
   */
  WrongPIN(0x040000L),
  /**
   * Cipher key incorrect.
   */
  WrongKey(0x080000L),
  /**
   * Encryption enabled.
   */
  Encryption(0x100000L),
  /**
   * Proprietary: card pending.
   */
  CardPending(0x800000L),
  // Register 4
  /**
   * X5: Hall sensor faulty.
   */
  HallSensorError(0x01000000L),
  /**
   * X5: Right pocket is blocked.
   */
  RightPocketBlocked(0x02000000L),
  /**
   * X5: Left pocket is blocked.
   */
  LeftPocketBlocked(0x04000000L),
  /**
   * X5: Coin didn't pass or stopped on the recovery light barrier.
   */
  CoinRecoveryError(0x08000000L),
  /**
   * X5: Coin didn't pass or stopped on the payment light barrier.
   */
  CoinDeliveryError(0x10000000L),
  /**
   * X5: Lack of polling during purge.
   */
  PurgeTimeoutError(0x20000000L),
  /**
   * X5: Sensor calibration is running.
   */
  SensorCalibration(0x40000000L),
  // Kommen nicht aus den Bytes
  /**
   * Payout was reset.
   */
  Reset(0x0100000000L),
  /**
   * Hopper is busy purging.
   */
  Purging(0x8000000000L);

  companion object {
    fun fromCode(code: Long): PayoutStatusFlag {
      return PayoutStatusFlag.entries.firstOrNull { it.code == code } ?: Nothing
    }
  }

  infix fun PayoutStatusFlag.and(other: PayoutStatusFlag): PayoutStatuses = EnumSet.of(this, other)
  infix fun PayoutStatuses.and(other: PayoutStatusFlag): PayoutStatuses {
    this.add(other)
    return this
  }
}

infix fun PayoutStatusFlag.and(other: PayoutStatusFlag): PayoutStatuses = EnumSet.of(this, other)
infix fun PayoutStatuses.and(other: PayoutStatusFlag): PayoutStatuses {
  this.add(other)
  return this
}

infix fun PayoutStatuses.has(other: PayoutStatusFlag): Boolean = this.contains(other)
infix fun PayoutStatusFlag.has(other: PayoutStatusFlag): Boolean = this == other

typealias PayoutStatuses = EnumSet<PayoutStatusFlag>