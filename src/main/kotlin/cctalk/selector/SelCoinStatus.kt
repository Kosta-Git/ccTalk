package cctalk.selector

data class SelCoinStatus(
    val coinable: Boolean,
    val sorterPath: Int,
    val overridePath: Boolean
)
