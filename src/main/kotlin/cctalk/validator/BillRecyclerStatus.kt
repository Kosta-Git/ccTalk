package cctalk.validator

data class BillRecyclerStatus(
    val Status: RecyclerFlags,
    val PayOutReject: BillPayoutRejectCode,
    val Remaining: Int,
    val LastDispensed: Int,
    val LastUndispensed: Int,
    val Stored: Int,
    val Storing: Int,
    val PayRejectCount: Int,
    val PayRejectedCount: Int,
)