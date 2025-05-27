package cctalk.validator

enum class ValidatorBillPosition(val code: Int) {
    /**
     * Unknown position.
     */
    Unknown(-1),
    /**
     * Bill stacked.
     */
    Stacked(0),
    /**
     * Bill in escrow.
     */
    Escrow(1),
    /**
     * AFD Dispenser SS1.
     */
    AFD_DispenserSS1(0x12),
    /**
     * AFD Dispenser SS2.
     */
    AFD_DispenserSS2(0x13),
    /**
     * AFD Dispenser SS3.
     */
    AFD_DispenserSS3(0x14),

    /**
     * Bill form recycler stored in cash box.
     */
    AFD_Stored(0x20);

    companion object {
        fun fromCode(code: Int): ValidatorBillPosition = ValidatorBillPosition.entries.firstOrNull { it.code == code } ?: Unknown
    }
}