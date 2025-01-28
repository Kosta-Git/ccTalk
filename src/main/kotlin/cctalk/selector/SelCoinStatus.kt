package cctalk.selector

data class SelCoinStatus(
  val inhibit: Boolean,
  val sorterPath: Int,
  val overridePath: Boolean
)
