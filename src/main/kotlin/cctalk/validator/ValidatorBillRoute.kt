package cctalk.validator

enum class ValidatorBillRoute(val code: Int) {
    /**
     * Return the bill.
     */
    Return(0),
    /**
     * Stack the bill.
     */
    Stack(1),
    /**
     * Hold the bill in escrow.
     */
    Hold(255);

    companion object {
        fun fromCode(code: Int): ValidatorBillRoute = ValidatorBillRoute.entries.firstOrNull { it.code == code } ?: Return
    }
}