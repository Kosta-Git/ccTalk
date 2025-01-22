package cctalk.device

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.right
import be.inotek.communication.CcTalkChecksumTypes
import cctalk.CcTalkError
import cctalk.CcTalkStatus
import cctalk.EscrowState
import cctalk.currency.CoinValue
import cctalk.currency.Currency
import cctalk.currency.VALUE_FACTORS
import cctalk.device.CcTalkDevice.CcTalkCommand.COIN_PRECISION
import cctalk.device.CcTalkDevice.CcTalkCommand.GET_DEFAULT_SORTER_PATH
import cctalk.device.CcTalkDevice.CcTalkCommand.GET_MASTER_INHIBIT
import cctalk.device.CcTalkDevice.CcTalkCommand.MODIFY_SORTER_OVERRIDE
import cctalk.device.CcTalkDevice.CcTalkCommand.MODIFY_SORTER_PATH
import cctalk.device.CcTalkDevice.CcTalkCommand.READ_BUFFERED_CREDIT
import cctalk.device.CcTalkDevice.CcTalkCommand.SETUP_ESCROW_STATE
import cctalk.device.CcTalkDevice.CcTalkCommand.SET_DEFAULT_SORTER_PATH
import cctalk.device.CcTalkDevice.CcTalkCommand.SET_MASTER_INHIBIT
import cctalk.device.SelectorDevice.Companion.MAX_EVENT_POLL
import cctalk.selector.*
import cctalk.selector.SelectorPollEvent.Companion.COIN_EVENTS
import cctalk.serial.CcTalkPort
import kotlin.math.min
import kotlin.math.pow

@OptIn(ExperimentalUnsignedTypes::class)
class SelectorDevice(
  port: CcTalkPort,
  address: Byte = 2,
  checksumType: CcTalkChecksumTypes = CcTalkChecksumTypes.Simple8
) : CcTalkDevice(port, address, checksumType) {
  companion object {
    const val MAX_EVENT_POLL = 5
  }

  private var eventCounter: Int = -1

  /**
   * Set the master inhibit status.
   * @param enabled true to enable the master inhibit, false to disable it.
   */
  suspend fun setMasterInhibit(enabled: Boolean): Either<CcTalkError, CcTalkStatus> = either {
    talkCcNoResponse {
      withDefaults(this@SelectorDevice)
      header(SET_MASTER_INHIBIT)
      data(ubyteArrayOf(if (enabled) 0u else 1u))
    }.bind()
  }

  /**
   * Get the master inhibit status.
   * @return true if the master inhibit is enabled, false otherwise.
   */
  suspend fun getMasterInhibit(): Either<CcTalkError, Boolean> = either {
    talkCcLongResponse {
      withDefaults(this@SelectorDevice)
      header(GET_MASTER_INHIBIT)
    }.bind() == 0L
  }

  suspend fun setDefaultPath(defaultPath: Int): Either<CcTalkError, CcTalkStatus> = either {
    talkCcNoResponse {
      withDefaults(this@SelectorDevice)
      header(SET_DEFAULT_SORTER_PATH)
      data(ubyteArrayOf(defaultPath.toUByte()))
    }.bind()
  }

  suspend fun getDefaultPath(): Either<CcTalkError, Int> = either {
    talkCc {
      withDefaults(this@SelectorDevice)
      header(GET_DEFAULT_SORTER_PATH)
    }.bind().data.firstOrNull()?.toInt() ?: -1
  }

  /**
   * Sets the state of the escrow (if connected).
   * If state == [EscrowState.Collect] or state == [EscrowState.Return]
   * the flaps will open for a fixed time span allowing the coins to drop.
   * @param state State of the flaps.
   * @return [CcTalkStatus.Ok] if successful otherwise an error code.
   */
  suspend fun setupEscrow(state: EscrowState): Either<CcTalkError, CcTalkStatus> = either {
    talkCcNoResponse {
      withDefaults(this@SelectorDevice)
      header(SETUP_ESCROW_STATE)
      data(
        ubyteArrayOf(
          when (state) {
            EscrowState.Collect -> 4u
            EscrowState.Return -> 2u
            else -> raise(
              CcTalkError.WrongParameterError(
                "invalid escrow state $state, must be either Collect or Return"
              )
            )
          }
        )
      )
    }.bind()
  }

  /**
   * Sets the price to be withdrawn from the medium for an EMP with NFC module.
   * If state == [EscrowState.Collect] or state == [EscrowState.Return]
   * the flaps will open for a fixed time span allowing the coins to drop.
   * @param priceSetting Price Setting [SelCoinPriceSetting].
   * @return [CcTalkStatus.Ok] if successful otherwise an error code.
   */
  suspend fun setupPrice(priceSetting: SelCoinPriceSetting): Either<CcTalkError, CcTalkStatus> = either {
    val iPrice = (priceSetting.price * 100).toInt()
    var flag: UByte = 0u
    if (priceSetting.cashlessPaymentBlocking) flag = flag or 0x01u
    if (priceSetting.machineOccupied) flag = flag or 0x02u
    if (priceSetting.serviceModeActive) flag = flag or 0x04u

    talkCcNoResponse {
      withDefaults(this@SelectorDevice)
      header(COIN_PRECISION)
      data(
        ubyteArrayOf(
          9u,
          iPrice.toUByte(),
          (iPrice shr 8).toUByte(),
          flag
        )
      )
    }.bind()
  }

  /**
   * Sets the override status of the 16 coins.
   * On power on override status for all coins is set to "false".
   * Use [getCoinValues] to identify available coin values and currencies
   * and [getCoinStates] to determine current override status.
   * @param coinStatuses Array of [SelCoinStatus] - must have at least 16 elements.
   * Only then [SelCoinStatus.override] field will be used.
   * @return [CcTalkStatus.Ok] if successful otherwise an error code.
   */
  suspend fun setCoinOverride(coinStatuses: List<SelCoinStatus>): Either<CcTalkError, CcTalkStatus> = either {
    ensure(coinStatuses.size < 16) {
      raise(CcTalkError.WrongParameterError("coinStatuses must have at least 16 elements"))
    }

    var ovr = 0xffff
    var msk = 0x0001
    for (i in 0 until 16) {
      if (coinStatuses[i].overridePath) ovr = ovr and msk.inv()
      msk = msk shl 1
    }

    talkCcNoResponse {
      withDefaults(this@SelectorDevice)
      header(MODIFY_SORTER_OVERRIDE)
      data(ubyteArrayOf(ovr.toUByte(), (ovr shr 8).toUByte()))
    }.bind()
  }

  /**
   * Sets the sorter path of the 16 coins.
   * On power on the sorter path for all coins is set to a default value.
   * Use [getCoinValues] to identify available coin values and currencies
   * and [getCoinStates] to determine current sorter paths.
   * @param sorterPaths Array of [SelCoinStatus] - must have at least 16 elements.
   * Only then [SelCoinStatus.sorterPath] field will be used.
   * @return [CcTalkStatus.Ok] if successful otherwise an error code.
   */
  suspend fun setCoinSorterPaths(sorterPaths: List<SelCoinStatus>): Either<CcTalkError, CcTalkStatus> = either {
    ensure(sorterPaths.size < 16) {
      raise(CcTalkError.WrongParameterError("currvals must have at least 16 elements"))
    }

    for (i in 0 until 16) {
      talkCcNoResponse {
        withDefaults(this@SelectorDevice)
        header(MODIFY_SORTER_PATH)
        data(ubyteArrayOf((i + 1).toUByte(), sorterPaths[i].sorterPath))
      }.bind()
    }
    CcTalkStatus.Ok
  }

  /**
   * Polls the device to retrieve current events.
   * Up to [MAX_EVENT_POLL] events can be retrieved. Poll should be performed app. every
   * 200msecs otherwise events especially credit may be lost.
   * If [PollSelectorResponse.status] == [SelectorPollEvent.Coin]
   * use [PollSelectorResponse.coinIndex]
   * to retrieve further information from the arrays returned by [getCoinValues] and [getCoinStates].
   * @return [PollSelectorResponseList] if successful otherwise an error code.
   */
  suspend fun pollSelector(): Either<CcTalkError, PollSelectorResponseList> = either {
    val pollResponses = mutableListOf<PollSelectorResponse>()
    val eventsData = talkCc {
      withDefaults(this@SelectorDevice)
      header(READ_BUFFERED_CREDIT)
    }.bind()

    ensure(eventsData.dataLength.toInt() >= 11) {
      raise(CcTalkError.DataFormatError(11, eventsData.dataLength.toInt()))
    }

    val receivedEventCounter = eventsData.data[0].toInt()

    // Just reset
    if (receivedEventCounter == 0 && eventCounter < 0) {
      eventCounter = 0
      pollResponses.add(PollSelectorResponse(false, SelectorPollEvent.Reset, -1, -1))
      return PollSelectorResponseList(1, 0, pollResponses).right()
    }

    // Get number of events since last poll
    val receivedEvents = if (receivedEventCounter >= eventCounter)
      receivedEventCounter - eventCounter
    else
      (255 - eventCounter) + receivedEventCounter

    val lostEvents = if (receivedEvents > MAX_EVENT_POLL) receivedEvents - MAX_EVENT_POLL else 0

    for (i in 0 until min(receivedEvents, MAX_EVENT_POLL)) {
      var status = SelectorPollEvent.Unknown
      var coinIndex = -1
      var coinPath = -1
      if (eventsData.data[i * 2 + 1].toInt() == 0) {
        status = SelectorPollEvent.fromCode(eventsData.data[i * 2 + 2].toInt())
      } else {
        status = SelectorPollEvent.Coin
        coinIndex = (eventsData.data[i * 2 + 1].toInt() - 1) and 0x000f
        coinPath = eventsData.data[i * 2 + 2].toInt()
      }
      val coinInserted = COIN_EVENTS.any { it == status }
      pollResponses.add(PollSelectorResponse(coinInserted, status, coinIndex, coinPath))
    }

    PollSelectorResponseList(
      min(receivedEvents, MAX_EVENT_POLL),
      lostEvents,
      pollResponses
    )
  }

  /**
   * Polls the device to retrieve pre coin info.
   * The intention is to give e.g. a sorting equipment more time to get ready for the coin.
   * @return [PollSelectorResponse] holding the information about an identified but not yet accepted coins.
   * @return [CcTalkError] if unsuccessful.
   */
  suspend fun pollPreCoinInfo(): Either<CcTalkError, PollSelectorResponse> = either {
    val preCoinData = talkCc {
      withDefaults(this@SelectorDevice)
      header(COIN_PRECISION)
      data(ubyteArrayOf(8u))
    }.bind()

    var coinInserted = false
    var status = SelectorPollEvent.Unknown
    var coinIndex = -1
    var coinPath = -1

    if (preCoinData.dataLength.toInt() < 2) {
      status = SelectorPollEvent.Null
      coinInserted = false
    } else {
      coinInserted = true
      if (preCoinData.data[0].toInt() == 0) {
        status = SelectorPollEvent.fromCode(preCoinData.data[1].toInt())
      } else {
        status = SelectorPollEvent.Coin
        coinIndex = (preCoinData.data[0].toInt() - 1) and 0x000f
        coinPath = preCoinData.data[1].toInt()
      }
    }
    PollSelectorResponse(coinInserted, status, coinIndex, coinPath)
  }

  suspend fun getCoinIdString(coinNumber: Int): Either<CcTalkError, String> = either {
    var builder = StringBuilder()
    talkCc {
      withDefaults(this@SelectorDevice)
      header(CcTalkCommand.REQUEST_COIN_ID)
      if (coinNumber > -1) data(ubyteArrayOf((coinNumber + 1).toUByte()))
    }.bind()
      .data
      .forEach { builder.append(it.toInt().toChar()) }
    builder.toString()
  }

  suspend fun getCoinValue(coinNumber: Int): Either<CcTalkError, CoinValue> = either {
    val separator = (5.5).toString()[1]
    var valueStr = ""
    var factorChar = ' '

    val coinId = getCoinIdString(coinNumber).bind()
    ensure(coinId.length >= 6) {
      raise(CcTalkError.WrongParameterError("coinId must have at least 6 characters"))
    }

    val id = coinId.substring(0, 2)
    for (i in 2 until 5) {
      if (coinId[i].isDigit()) {
        valueStr += coinId[i]
      } else {
        valueStr += separator
        factorChar = coinId[i]
      }
    }

    val decimals = Currency.byCcTalkID(id.uppercase())?.decimals ?: 2
    if (valueStr.contains(",")) raise(CcTalkError.PayloadError("valueStr must not contain ','"))

    val parsed = try {
      valueStr.toDouble()
    } catch (e: Exception) {
      raise(CcTalkError.PayloadError("valueStr must be a valid double"))
    }

    var factor = 1.0
    for (i in VALUE_FACTORS) {
      if (i.factor == factorChar) {
        factor = i.face
        break
      }
    }

    val value = (parsed / 10.0.pow(decimals.toDouble())) * factor
    val intValue = (value * 10.0.pow(decimals.toDouble()) * factor).toInt()

    CoinValue(id, value, intValue)
  }

  /**
   * Retrieves values and currency IDs of the 16 coins.
   * @return List of [CoinValue] if successful otherwise an error code.
   */
  suspend fun getCoinValues(): Either<CcTalkError, List<CoinValue>> = either {
    val coinValues = mutableListOf<CoinValue>()
    for (i in 0 until 16) {
      val coinValue = getCoinValue(i).bind()
      coinValues.add(coinValue)
    }
    coinValues
  }

  /**
   * Retrieves the current inhibit status, sorter path and override status of the 16 coins.
   * @return List of [SelCoinStatus] if successful otherwise an error code.
   */
  suspend fun getCoinStates(): Either<CcTalkError, List<SelCoinStatus>> = either {
    TODO()
  }

  /*

        /// <summary>
        /// Retrieves the current inhibit status, sorter path and override status of the 16 coins.
        /// </summary>
        /// <param name="currvals">Array of <see cref="SelCoinStatus"/> - must have at least 16 elements.</param>
        /// <returns>
        /// <see cref="CcTalkErrors"/>.Ok if successful otherwise an error code.
        /// </returns>
        public CcTalkErrors GetCoinStates(ref SelCoinStatus[] currvals)
        {
            int i;
            ushort inh, msk;

            if (currvals.Length < 16) return CcTalkErrors.WrongParameter;

            inh = (ushort)GetLongResponse(230);
            if (lasterr != CcTalkErrors.Ok) return lasterr;
            //ovr = (ushort)GetLongResponse(221);
            //if (lasterr != whCcTalkErrors.Ok) return lasterr;

            msk = 0x0001;
            for (i = 0; i < 16; i++)
            {
                currvals[i].Inhibit = (inh & msk) != 0;
                currvals[i].Override = false; // (ovr & msk) == 0;
                msk <<= 1;
                currvals[i].SorterPath = GetSorterPath(i);
                if (lasterr != CcTalkErrors.Ok) return lasterr;
            }
            return lasterr;
        }

        /// <summary>
        /// Retrieves override status for all sorter path.
        /// </summary>
        /// <param name="sorteroverrides">Array of bool, must have at least 8 elements.</param>
        /// <returns>
        /// <see cref="CcTalkErrors"/>.Ok if successful otherwise an error code.
        /// </returns>
        public CcTalkErrors GetSorterOverride(ref bool[] sorteroverrides)
        {
            CcTalkDataBlock sdta = new CcTalkDataBlock(cstype);
            CcTalkDataBlock rdta = new CcTalkDataBlock(cstype);

            if (sorteroverrides.Length < 8) return CcTalkErrors.WrongParameter;
            sdta.DataLength = 0;
            sdta.Header = 221;
            lasterr = TalkCc(sdta, ref rdta);
            for (int i = 0; i < 8; i++)
            {
                if ((lasterr == CcTalkErrors.Ok) && (rdta.DataLength > 0))
                    sorteroverrides[i] = (rdta.Data[0] & (0x01 << i)) == 0;
                else
                    sorteroverrides[i] = false;
            }
            return lasterr;
        }
        /// <summary>
        /// Sets the override status of up to 8 paths.
        /// </summary>
        /// <remarks>
        /// On power on override status for all paths is set to "false".
        /// </remarks>
        /// <param name="sorteroverrides">
        /// Array of bool - must have at least 8 elements.
        /// </param>
        /// <returns>
        /// <see cref="CcTalkErrors"/>.Ok if successful otherwise an error code.
        /// </returns>
        public CcTalkErrors SetSorterOverride(bool[] sorteroverrides)
        {
            CcTalkDataBlock sdta = new CcTalkDataBlock(cstype);
            CcTalkDataBlock rdta = new CcTalkDataBlock(cstype);

            if (sorteroverrides.Length < 8) return CcTalkErrors.WrongParameter;
            sdta.Data[0] = 0xff;
            for (int i = 0; i < 8; i++)
            {
                if (sorteroverrides[i]) sdta.Data[0] = (byte)(sdta.Data[0] & ~(0x01 << i));
            }
            sdta.DataLength = 1;
            sdta.Header = 222;
            lasterr = TalkCc(sdta, ref rdta);
            return lasterr;
        }

        /// <summary>
        /// Sets the inhibit status of the 16 coins.
        /// </summary>
        /// <remarks>
        /// On power on all coins are inhibited.
        /// Use <see cref="GetCoinValues"/> to identify available coin values and currencies
        /// and <see cref="GetCoinStates"/> to determine current inhibit status.
        /// </remarks>
        /// <param name="currvals">
        /// Array of <see cref="SelCoinStatus"/> - must have at least 16 elements.
        /// Only then <see cref="SelCoinStatus.Inhibit"/> field will be used.
        /// If it is set to true the coin will be enabled. This inconsistency is due to ccTalk terminology.
        /// </param>
        /// <returns>
        /// <see cref="CcTalkErrors"/>.Ok if successful otherwise an error code.
        /// </returns>
        public CcTalkErrors SetCoinInhibit(SelCoinStatus[] currvals)
        {
            int i;
            uint inh, msk;
            CcTalkDataBlock sdta = new CcTalkDataBlock(cstype);
            CcTalkDataBlock rdta = new CcTalkDataBlock(cstype);

            if (currvals.Length < 16) return CcTalkErrors.WrongParameter;
            msk = 0x0001;
            inh = 0x0000;
            for (i = 0; i < 16; i++)
            {
                if (currvals[i].Inhibit) inh |= msk;
                msk <<= 1;
            }
            sdta.DataLength = 0;        // für SR5 & Co.: Alle Bänke aktivieren.
            sdta.Header = 179;
            sdta.Data[0] = 1;
            TalkCc(sdta, ref rdta);     // Fehler wird nicht beachtet.

            sdta.DataLength = 2;
            sdta.Header = 231;
            sdta.Data[0] = (byte)(inh & 0x00ff);
            sdta.Data[1] = (byte)((inh >> 8) & 0x00ff);
            lasterr = TalkCc(sdta, ref rdta);
            return lasterr;
        }
        /// <summary>
        /// Sets the same inhibit status for all 16 coins.
        /// </summary>
        /// <remarks>
        /// On power on all coins are inhibited.
        /// </remarks>
        /// <param name="coinenable">
        /// The status that will be set for all coins. If true all coins will be enabled.
        /// </param>
        /// <returns>
        /// <see cref="CcTalkErrors"/>.Ok if successful otherwise an error code.
        /// </returns>
        public CcTalkErrors SetCoinInhibit(bool coinenable)
        {
            SelCoinStatus[] currvals = new SelCoinStatus[16];
            for (int i = 0; i < 16; i++) currvals[i].Inhibit = coinenable;
            return SetCoinInhibit(currvals);
        }
  *
  * */
}