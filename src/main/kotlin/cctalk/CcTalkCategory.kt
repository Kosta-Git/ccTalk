package cctalk

/**
 * ccTalk cash devices categories.
 *
 * <p>Currently only CoinSelector and BillValidator are implemented.</p>
 */
enum class CcTalkCategory {
  /**
   * Unknown device.
   */
  Unknown,

  /**
   * Coin Selector.
   */
  CoinSelector,

  /**
   * Bill Validator.
   */
  BillValidator,

  /**
   * Card Reader.
   */
  CardReader,

  /**
   * Hopper (payout device).
   */
  PayOut,

  /**
   * Coin Scale.
   */
  CoinScale,

  /**
   * Dongle (Peripheral Device).
   */
  Peripheral,

  /**
   * Change Giver (MDB device via dongle).
   */
  ChangeGiver,

  /**
   * Changer (Group of hoppers).
   */
  Changer,

  /**
   * Coin Feeder.
   */
  CoinFeeder,

  /**
   * Cashless Payment System (MDB device via dongle).
   */
  Cashless,

  /**
   * Multi path sorter with escrow function.
   */
  EscrowSorter,

  /**
   * Bootloader device.
   */
  Bootloader;

  companion object Parse {
    /**
     * Parse category from device type.
     *
     * @param type Device type.
     * @return Category.
     */
    fun from(type: String): CcTalkCategory {
      return when (type.lowercase().replace(" ", "")) {
        "coinselector" -> CoinSelector
        "billvalidator" -> BillValidator
        "cardreader" -> CardReader
        "payout" -> PayOut
        "coinscale" -> CoinScale
        "peripheral" -> Peripheral
        "changegiver" -> ChangeGiver
        "changer" -> Changer
        "coinfeeder" -> CoinFeeder
        "cashless" -> Cashless
        "escrowsorter" -> EscrowSorter
        "bootloader" -> Bootloader
        else -> Unknown
      }
    }
  }
}