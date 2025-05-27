package cctalk.device

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.right
import be.inotek.communication.CcTalkChecksumTypes
import cctalk.CcTalkError
import cctalk.CcTalkStatus
import cctalk.currency.Currency
import cctalk.device.CcTalkDevice.CcTalkCommand.GET_MASTER_INHIBIT
import cctalk.device.CcTalkDevice.CcTalkCommand.MODIFY_BILL_INHIBIT_STATUS
import cctalk.device.CcTalkDevice.CcTalkCommand.REQUEST_BILL_ID
import cctalk.device.CcTalkDevice.CcTalkCommand.REQUEST_BILL_INHIBIT_STATUS
import cctalk.device.CcTalkDevice.CcTalkCommand.REQUEST_BUFFERED_BILL_EVENTS
import cctalk.device.CcTalkDevice.CcTalkCommand.ROUTE_BILL
import cctalk.device.CcTalkDevice.CcTalkCommand.SET_MASTER_INHIBIT
import cctalk.device.CcTalkDevice.CcTalkCommand.SETUP_BILL_ESCROW
import cctalk.serial.CcTalkPort
import cctalk.validator.*
import kotlin.math.min
import kotlin.math.pow

class ValidatorDevice(
    port: CcTalkPort,
    address: Int = 40, // Default address for validators
    checksumType: CcTalkChecksumTypes = CcTalkChecksumTypes.CRC16
) : CcTalkDevice(port, address, checksumType) {
    companion object {
        const val MAX_EVENT_POLL = 16
    }

    private val countryScaleFactor = mutableListOf<CountryScaleFactor>()
    private var rcSupported = false
    private var rcConnected = false
    private var rcBox = 1
    private var rcIdx = 0
    private var rcCount = -1
    var billInEscrow = false
    private var buttonEnabled = false
    private var escrowEnabled = true
    private var autoResetEscrow = true
    private var masterInhibit = false
    private var type: BillRecyclerType = BillRecyclerType.None
    private var eventCounter: Int = -1

    init {
        for (i in 0..15) {
            countryScaleFactor.add(CountryScaleFactor("XX", 1, 2))
        }
    }

    /**
     * Set the master inhibit status.
     * @param enabled true to enable the master inhibit, false to disable it.
     */
    suspend fun setMasterInhibit(enabled: Boolean): Either<CcTalkError, CcTalkStatus> = either {
        talkCcNoResponse {
            withDefaults(this@ValidatorDevice)
            header(SET_MASTER_INHIBIT)
            data(byteArrayOf(if (enabled) 0 else 1))
        }.bind()
    }

    /**
     * Get the master inhibit status.
     * @return true if the master inhibit is enabled, false otherwise.
     */
    suspend fun getMasterInhibit(): Either<CcTalkError, Boolean> = either {
        talkCcLongResponse {
            withDefaults(this@ValidatorDevice)
            header(GET_MASTER_INHIBIT)
        }.bind() == 0L
    }

    /**
     * Get escrow enabled status.
     * @return true if escrow is enabled, false otherwise.
     */
    suspend fun getEscrowEnabled(): Either<CcTalkError, Boolean> = either {
        val result = talkCc {
            withDefaults(this@ValidatorDevice)
            header(152) // REQUEST_BILL_ESCROW_STATUS
        }.bind()
        
        ensure(result.dataLength >= 1) { CcTalkError.DataLengthError(1, 255, result.dataLength) }
        (result.data[0] and 0x02) != 0
    }

    /**
     * Set escrow enabled status.
     * @param enabled true to enable escrow, false to disable.
     */
    suspend fun setEscrowEnabled(enabled: Boolean): Either<CcTalkError, CcTalkStatus> = either {
        talkCcNoResponse {
            withDefaults(this@ValidatorDevice)
            header(SETUP_BILL_ESCROW)
            data(byteArrayOf(if (enabled) 2 else 0))
        }.bind()
        escrowEnabled = enabled
        CcTalkStatus.Ok
    }

    /**
     * Get bill values for all 16 bill positions.
     */
    suspend fun getBillValues(): Either<CcTalkError, List<BillValue>> = either {
        val billValues = mutableListOf<BillValue>()
        for (i in 0 until 16) {
            val billValue = getBillValue(i).bind()
            billValues.add(billValue)
        }
        billValues
    }

    /**
     * Get bill value for a specific bill number.
     */
    suspend fun getBillValue(billNumber: Int): Either<CcTalkError, BillValue> = either {
        if (billNumber !in 0..15) {
            raise(CcTalkError.WrongParameterError("billNumber must be between 0 and 15, got: $billNumber"))
        }

        val billId = getBillIdString(billNumber).bind()
        ensure(billId.length >= 6) {
            CcTalkError.PayloadError("bill ID should be at least 6 characters, got: ${billId.length}")
        }

        val currencyId = billId.substring(0, 2)
        if (currencyId == "..") {
            return BillValue(0.0, "", 0).right()
        }

        val valueString = billId.substring(2, 5)
        val currency = Currency.Search.byCcTalkID(currencyId) ?: 
            raise(CcTalkError.PayloadError("unknown currency: $currencyId"))

        val billValue = try {
            val value = valueString.toDouble()
            value / 10.0.pow(currency.decimals.toDouble())
        } catch (_: NumberFormatException) {
            raise(CcTalkError.PayloadError("invalid bill value: $valueString"))
        }

        BillValue(billValue, currencyId, currency.decimals)
    }

    /**
     * Get bill ID string for a specific bill number.
     */
    suspend fun getBillIdString(billNumber: Int): Either<CcTalkError, String> = either {
        if (billNumber !in 0..15) {
            raise(CcTalkError.WrongParameterError("billNumber must be between 0 and 15, got: $billNumber"))
        }

        val result = talkCc {
            withDefaults(this@ValidatorDevice)
            header(REQUEST_BILL_ID)
            data(byteArrayOf((billNumber + 1).toByte()))
        }.bind()

        buildString {
            result.data.forEach { append(it.toChar()) }
        }
    }

    /**
     * Get bill states (inhibit status) for all 16 bills.
     */
    suspend fun getBillStates(): Either<CcTalkError, List<ValidatorBillStatus>> = either {
        val inhibitStatus = talkCcLongResponse {
            withDefaults(this@ValidatorDevice)
            header(REQUEST_BILL_INHIBIT_STATUS)
        }.bind()

        (0 until 16).map { i ->
            val mask = 0x0001L shl i
            ValidatorBillStatus(inhibit = (inhibitStatus and mask) != 0L)
        }
    }

    /**
     * Set bill inhibit status for all 16 bills.
     */
    suspend fun setBillInhibit(billStatuses: List<ValidatorBillStatus>): Either<CcTalkError, CcTalkStatus> = either {
        ensure(billStatuses.size >= 16) {
            CcTalkError.WrongParameterError("billStatuses must have at least 16 elements")
        }

        var inhibit = 0
        for (i in 0 until 16) {
            if (billStatuses[i].inhibit) {
                inhibit = inhibit or (0x0001 shl i)
            }
        }

        talkCcNoResponse {
            withDefaults(this@ValidatorDevice)
            header(MODIFY_BILL_INHIBIT_STATUS)
            data(byteArrayOf((inhibit and 0x00ff).toByte(), ((inhibit shr 8) and 0x00ff).toByte()))
        }.bind()
    }

    /**
     * Set the same inhibit status for all 16 bills.
     */
    suspend fun setBillInhibit(billEnable: Boolean): Either<CcTalkError, CcTalkStatus> = either {
        val billStatuses = (0 until 16).map { ValidatorBillStatus(inhibit = billEnable) }
        setBillInhibit(billStatuses).bind()
    }

    /**
     * Route a bill held in escrow.
     */
    suspend fun routeBill(route: ValidatorBillRoute): Either<CcTalkError, CcTalkStatus> = either {
        val result = talkCc {
            withDefaults(this@ValidatorDevice)
            header(ROUTE_BILL)
            data(byteArrayOf(route.code.toByte()))
        }.bind()

        when (route) {
            ValidatorBillRoute.Return, ValidatorBillRoute.Stack -> billInEscrow = false
            ValidatorBillRoute.Hold -> {} // Keep escrow state
        }

        if (result.dataLength > 0) {
            when (result.data[0]) {
                254 -> raise(CcTalkError.PayloadError("Bill escrow empty"))
                255 -> raise(CcTalkError.PayloadError("Bill route failed"))
                else -> raise(CcTalkError.PayloadError("Unknown error: ${result.data[0]}"))
            }
        }
        CcTalkStatus.Ok
    }

    /**
     * Poll the validator to retrieve current events.
     */
    suspend fun pollValidator(): Either<CcTalkError, ValidatorPollResponseList> = either {
        val pollResponses = mutableListOf<ValidatorPollResponse>()
        val eventsData = talkCc {
            withDefaults(this@ValidatorDevice)
            header(REQUEST_BUFFERED_BILL_EVENTS)
        }.bind()

        ensure(eventsData.dataLength >= 11) {
            CcTalkError.DataFormatError(11, eventsData.dataLength)
        }

        val receivedEventCounter = eventsData.data[0].toUByte().toInt()

        // Just reset
        if (receivedEventCounter == 0 && eventCounter < 0) {
            eventCounter = 0
            pollResponses.add(ValidatorPollResponse(ValidatorPollEvent.Reset, -1, ValidatorBillPosition.Unknown))
            return ValidatorPollResponseList(1, 0, pollResponses).right()
        }

        // Get number of events since last poll
        val receivedEvents = if (receivedEventCounter >= eventCounter) {
            receivedEventCounter - eventCounter
        } else {
            (255 - eventCounter) + receivedEventCounter
        }

        eventCounter = receivedEventCounter
        val lostEvents = if (receivedEvents > MAX_EVENT_POLL) receivedEvents - MAX_EVENT_POLL else 0

        for (i in 0 until min(receivedEvents, MAX_EVENT_POLL)) {
            var status = ValidatorPollEvent.Unknown
            var billIndex = -1
            var billPosition = ValidatorBillPosition.Unknown

            if (eventsData.data[i * 2 + 1].toUByte().toInt() == 0) {
                status = ValidatorPollEvent.fromCode(eventsData.data[i * 2 + 2].toUByte().toInt())
                if (status == ValidatorPollEvent.Returned) billInEscrow = false
                if (status == ValidatorPollEvent.AFD_Locked) masterInhibit = true
                if (status == ValidatorPollEvent.AFD_Unlocked) masterInhibit = false
            } else {
                status = ValidatorPollEvent.Bill
                billIndex = (eventsData.data[i * 2 + 1].toUByte().toInt() - 1) and 0x001f
                billPosition = ValidatorBillPosition.fromCode(eventsData.data[i * 2 + 2].toUByte().toInt())
                
                when (billPosition) {
                    ValidatorBillPosition.Escrow -> billInEscrow = true
                    ValidatorBillPosition.Stacked,
                    ValidatorBillPosition.AFD_DispenserSS1,
                    ValidatorBillPosition.AFD_DispenserSS2,
                    ValidatorBillPosition.AFD_DispenserSS3 -> billInEscrow = false
                    else -> {}
                }
            }
            pollResponses.add(ValidatorPollResponse(status, billIndex, billPosition))
        }

        if (billInEscrow && escrowEnabled && autoResetEscrow) {
            routeBill(ValidatorBillRoute.Hold).bind()
        }

        ValidatorPollResponseList(
            min(receivedEvents, MAX_EVENT_POLL),
            lostEvents,
            pollResponses
        )
    }
}