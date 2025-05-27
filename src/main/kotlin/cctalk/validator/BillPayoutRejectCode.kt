package cctalk.validator

enum class BillPayoutRejectCode(val code: Int) {
    Normal(0),
    LengthError(1),
    LevelError(2),
    OtherError(3);

    companion object {
        fun fromCode(code: Int): BillPayoutRejectCode = BillPayoutRejectCode.entries.firstOrNull { it.code == code } ?: Normal
    }
}