package cctalk.selector

data class SelCoinStatus(
  val inhibit: Boolean,
  val sorterPath: UByte,
  val overridePath: Boolean
)
