package cctalk.selector

enum class SelectorPollEvent(val code: Int) {
  /**
   * A coin was accepted.
   */
  Coin(512),
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
  Null(0),
  /**
   * Reject: coin not recognized.
   */
  CoinReject(1),
  /**
   * Reject: coin inhibited (see: SelectorComm.MasterInhibit).
   */
  CoinInhibit(2),
  /**
   * Multiple Window.
   */
  MultipleWindow(3),
  /**
   * Wake-up timeout.
   */
  WakeUpTimeout(4),
  /**
   * Validation timeout.
   */
  ValidationTimeout(5),
  /**
   * Credit sensor timeout.
   */
  CreditSensorTimeout(6),
  /**
   * Sorter opto timeout.
   */
  SorterOptoTimeout(7),
  /**
   * Reject: follow up coin.
   */
  FollowUp(8),
  /**
   * Accept gate not ready.
   */
  AcceptGateNotReady(9),
  /**
   * Credit sensor not ready.
   */
  CreditSensorNotReady(10),
  /**
   * Sorter not ready.
   */
  SorterNotReady(11),
  /**
   * Validation sensor not ready.
   */
  ValidationSensorNotReady(12),
  /**
   * Device busy.
   */
  Busy(13),
  /**
   * Credit sensor blocked.
   */
  CreditSensorBlocked(14),
  /**
   * Sorter opto blocked.
   */
  SorterOptoBlocked(15),
  /**
   * Credit sequence blocked.
   */
  CreditSequenceError(16),
  /**
   * Coin going backwards.
   */
  CoinGoingBackwards(17),
  /**
   * Coin too fast.
   */
  CoinTooFast(18),
  /**
   * Coin too slow.
   */
  CoinJam(19),
  /**
   * Coin on a string.
   */
  CoinOnString(20),
  /**
   * DCE(?) opto timeout.
   */
  DceOptoTimeout(21),
  /**
   * DCE(?) opto not seen.
   */
  DceOptoNotSeen(22),
  /**
   * Credit sensor reached too early.
   */
  CreditSensorReachedTooEarly(23),
  /**
   * Reject coin.
   */
  RejectCoin(24),
  /**
   * Reject slug.
   */
  RejectSlug(25),
  /**
   * Reject sensor blocked.
   */
  RejectSensorBlocked(26),
  /**
   * Games overload.
   */
  GamesOverload(27),
  /**
   * Maximum coin meter pulses exceeded.
   */
  MaxPulsesExceeded(28),
  /**
   * Accept gate open not closed.
   */
  AcceptGateNotClosed(29),
  /**
   * Accept gate closed not open.
   */
  AcceptGateNotOpen(30),
  /**
   * Manifold opto timeout.
   */
  ManifoldOptoTimeout(31),
  /**
   * Manifold opto blocked.
   */
  ManifeldOptoBlocked(32),
  /**
   * Manifold not ready.
   */
  ManifoldNotReady(33),
  /**
   * Security status changed.
   */
  SecurityStatusChanged(34),
  /**
   * Motor exception.
   */
  MotorException(35),
  /**
   * Swallowed coin.
   */
  SwallowedCoin(36),
  /**
   * Coin too fast (over validation sensor).
   */
  CoinTooFast2(37),
  /**
   * Coin too slow (over validation sensor).
   */
  CoinTooSlow(38),
  /**
   * Coin incorrectly sorted.
   */
  CoinIncorrectlySorted(39),
  /**
   * External light attack.
   */
  ExternalLightAttack(40),
  /**
   * Coin number 1 inhibited.
   */
  CoinInhibit00(128),
  /**
   * Coin number 2 inhibited.
   */
  CoinInhibit01(129),
  /**
   * Coin number 3 inhibited.
   */
  CoinInhibit02(130),
  /**
   * Coin number 4 inhibited.
   */
  CoinInhibit03(131),
  /**
   * Coin number 5 inhibited.
   */
  CoinInhibit04(132),
  /**
   * Coin number 6 inhibited.
   */
  CoinInhibit05(133),
  /**
   * Coin number 7 inhibited.
   */
  CoinInhibit06(134),
  /**
   * Coin number 8 inhibited.
   */
  CoinInhibit07(135),
  /**
   * Coin number 9 inhibited.
   */
  CoinInhibit08(136),
  /**
   * Coin number 10 inhibited.
   */
  CoinInhibit09(137),
  /**
   * Coin number 11 inhibited.
   */
  CoinInhibit10(138),
  /**
   * Coin number 12 inhibited.
   */
  CoinInhibit11(139),
  /**
   * Coin number 13 inhibited.
   */
  CoinInhibit12(140),
  /**
   * Coin number 14 inhibited.
   */
  CoinInhibit13(141),
  /**
   * Coin number 15 inhibited.
   */
  CoinInhibit14(142),
  /**
   * Coin number 16 inhibited.
   */
  CoinInhibit15(143),
  /**
   * Data block request.
   */
  DataBlockRequest(253),
  /**
   * Return lever pushed.
   */
  Return(254),
  /**
   * Unspecified alarm code.
   */
  UnspecifiedCode(255),
  /**
   * Change giver is paying out.
   */
  PayoutBusy(640),
  /**
   * Coin routing error.
   */
  RoutingError(641),
  /**
   * Coin acceptor was removed from the change giver.
   */
  Unplugged(642),
  /**
   * Change giver tube has a defective sensor.
   */
  TubeSensor(643),
  /**
   * Change giver tube is jammed.
   */
  TubeJam(644),
  /**
   * Change giver ROM checksum error.
   */
  RomCheckSum(645),
  /**
   * A credited coin was possibly removed from the change giver.
   */
  CoinRemoved(646);

  companion object {
    fun fromCode(code: Int): SelectorPollEvent = SelectorPollEvent.entries.firstOrNull { it.code == code } ?: Unknown

    val COIN_EVENTS: List<SelectorPollEvent> = listOf(
      Coin,
      CoinInhibit,
      CoinInhibit00,
      CoinInhibit01,
      CoinInhibit02,
      CoinInhibit03,
      CoinInhibit04,
      CoinInhibit05,
      CoinInhibit06,
      CoinInhibit07,
      CoinInhibit08,
      CoinInhibit09,
      CoinInhibit10,
      CoinInhibit11,
      CoinInhibit12,
      CoinInhibit13,
      CoinInhibit14,
      CoinInhibit15,
      CoinReject,
    )
  }
}