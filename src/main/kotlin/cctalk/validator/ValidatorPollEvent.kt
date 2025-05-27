package cctalk.validator

enum class ValidatorPollEvent(val code: Int) {
    /**
     * A bill was accepted.
     */
    Bill(512),

    /**
     * Device was reset.
     */
    Reset(513),

    /**
     * Unknown event.
     */
    Unknown(514),

    /**
     * Nothing to report.
     */
    Null(515),

    /**
     * Master inhibit active.
     */
    MasterInhibit(0),

    /**
     * Bill returned from escrow.
     */
    Returned(1),

    /**
     * Invalid bill (due to validation fail).
     */
    ValidationFailed(2),

    /**
     * Invalid bill (due to transport problem)
     */
    TransportProblem(3),

    /**
     * Inhibited bill (on protocol).
     */
    Inhibit(4),

    /**
     * Inhibited bill (on DIP switches).
     */
    Switch(5),

    /**
     * Bill jammed in transport (unsafe mode).
     */
    UnsafeJam(6),

    /**
     * Bill jammed in stacker.
     */
    StackerJam(7),

    /**
     * Bill pulled backwards.
     */
    PulledBackwards(8),

    /**
     * Bill tamper.
     */
    Tamper(9),

    /**
     * Stacker OK.
     */
    StackerOk(10),

    /**
     * Stacker removed.
     */
    StackerRemoved(11),

    /**
     * Stacker inserted.
     */
    StackerInserted(12),

    /**
     * Stacker faulty.
     */
    StackerFaulty(13),

    /**
     * Stacker full.
     */
    StackerFull(14),

    /**
     * Stacker jammed.
     */
    StackerJammed(15),

    /**
     * Bill jammed in transport (safe mode).
     */
    SafeJam(16),

    /**
     * Opto fraud detected.
     */
    OptoFraud(17),

    /**
     * String fraud detected.
     */
    StringFraud(18),

    /**
     * Validator is busy.
     */
    Busy(640),

    /**
     * ROM Checksum error
     */
    RomChecksum(641),

    /**
     * One of the motors failed.
     */
    DefectiveMotor(642),

    /**
     * Invalid escrow request.
     */
    InvalidEscrow(643),

    /**
     * Bill validator disabled by host
     */
    Disabled(644),

    /**
     * A bill is being dispensed from the recycle box.
     */
    Paying(645),

    /**
     * A bill is being collected from recycle box to cash box.
     */
    Collecting(646),

    /**
     * A bill has been collected from recycle box to cash box.
     */
    Collected(647),

    /**
     * Dispense is complete.
     */
    PayValid(648),

    /**
     * A bill stays at the note payout slot.
     */
    PayStay(649),

    /**
     * Dispensing has been cancelled and bill was collected to cash box.
     */
    ReturnToBox(650),

    /**
     * Bill is collected to cash box since an error was detected while dispensing.
     */
    ReturnPayOutNote(651),

    /**
     * Error during the collection of dispensed bills after dispensing process has been cancelled.
     */
    ReturnError(652),

    /**
     * For adp AFD-MONO: Reset.
     */
    AFD_Reset(0x20),

    /**
     * For adp AFD-MONO: Connection to master.
     */
    AFD_Connect(0x21),

    /**
     * For adp AFD-MONO: Initialisation of MD-100.
     */
    AFD_Init(0x22),

    /**
     * For adp AFD-MONO: Married (encrypted version only).
     */
    AFD_Married(0x23),

    /**
     * For adp AFD-MONO: Update AFD / MD-100 active.
     */
    AFD_Update(0x24),

    /**
     * For adp AFD-MONO: Configuration of dispenser SS1...SS3.
     */
    AFD_Config(0x25),

    /**
     * For adp AFD-MONO: Bill in transport.
     */
    AFD_Busy(0x26),

    /**
     * For adp AFD-MONO: Idle - ready for work.
     */
    AFD_Idle(0x27),

    /**
     * For adp AFD-MONO: Globally locked.
     */
    AFD_Locked(0x28),

    /**
     * For adp AFD-MONO: Globally unlocked.
     */
    AFD_Unlocked(0x29),

    /**
     * For adp AFD-MONO: Bill moves to cash box after an error.
     */
    AFD_MoveCBox(0x2a),

    /**
     * For adp AFD-MONO: Bill paid out and staying in the bezel.
     */
    AFD_MoveBill(0x2b),

    /**
     * For adp AFD-MONO: Operating mode changed to Sys_S_Fill.
     */
    AFD_ChangeToFill(0x2c),

    /**
     * For adp AFD-MONO: Operating mode changed to Sys_S-Unload.
     */
    AFD_ChangeToUnload(0x2d),

    /**
     * For adp AFD-MONO: Operating mode changed to Sys_Game.
     */
    AFD_ChangeToGame(0x2e),

    /**
     * For adp AFD-MONO: Modifying bill types.
     */
    AFD_ModeType(0x2f),

    /**
     * For adp AFD-MONO: AFD tries to start pay out of one bill.
     */
    AFD_Payout(0x30),

    /**
     * For adp AFD-MONO: Specific error.
     */
    AFD_SpecificError(0x65);

    companion object {
        fun fromCode(code: Int): ValidatorPollEvent = ValidatorPollEvent.entries.firstOrNull { it.code == code } ?: Unknown
    }
}