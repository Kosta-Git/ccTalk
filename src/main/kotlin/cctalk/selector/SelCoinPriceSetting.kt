package cctalk.selector

data class SelCoinPriceSetting(
  val price: Double,
  val cashlessPaymentBlocking: Boolean,
  val machineOccupied: Boolean,
  val serviceModeActive: Boolean,
)