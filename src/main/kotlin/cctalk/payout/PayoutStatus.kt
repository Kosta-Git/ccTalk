package cctalk.payout

import java.util.*

data class PayoutStatus(
  val status: PayoutStatuses,
  val remaining: Int,
  val lastPayout: Int,
  val lastUnpaid: Int,
  val highLevelSensorStatus: PayoutSensorStatus,
  var lowLevelSensorStatus: PayoutSensorStatus
)

data class WhPayoutStatus(
  val status: PayoutStatuses,
  val remaining: Double,
  val lastPayout: Double,
  val lastUnpaid: Double,
  val highLevelSensorStatus: PayoutSensorStatus = PayoutSensorStatus.Unknown,
  var lowLevelSensorStatus: PayoutSensorStatus = PayoutSensorStatus.Unknown
)

data class PayoutStatusExtended(
  val event: Int,
  val status: PayoutStatuses,
  val remaining: Int,
  val lastPayout: Int,
  val lastUnpaid: Int,
  val highLevelSensorStatus: PayoutSensorStatus,
  val lowLevelSensorStatus: PayoutSensorStatus
) {
  fun toPayoutStatus(): PayoutStatus {
    return PayoutStatus(
      status,
      remaining,
      lastPayout,
      lastUnpaid,
      highLevelSensorStatus,
      lowLevelSensorStatus
    )
  }
}