package cctalk.device

import arrow.core.Either
import arrow.core.raise.either
import be.inotek.communication.CcTalkChecksumTypes
import cctalk.CcTalkCategory
import cctalk.CcTalkError
import cctalk.CcTalkStatus
import cctalk.serial.CcTalkPort

open class CcTalkDevice(
  val port: CcTalkPort,
  val address: Int,
  val checksumType: CcTalkChecksumTypes
) : CcTalkPort by port {
  object CcTalkCommand {
    const val SOURCE_ADDRESS = 1

    // Common headers
    const val SIMPLE_POLL = 254
    const val REQUEST_MANUFACTURER_ID = 246
    const val REQUEST_CATEGORY_ID = 245
    const val REQUEST_PRODUCT_CODE = 244
    const val REQUEST_SERIAL_NUMBER = 242
    const val REQUEST_SOFTWARE_REVISION = 241

    // Dongle headers
    const val LED_ON_OFF = 107
    const val LED_BLINK = 108

    // Payout headers
    const val REQUEST_HOPPER_STATUS = 166
    const val REQUEST_HOPPER_LEVEL_STATUS = 217
    const val REQUEST_HOPPER_TEST = 163
    const val HOPPER_ENABLE = 164
    const val REQUEST_HOPPER_DISPENSE = 167
    const val REQUEST_HOPPER_PUMP_RNG = 161
    const val REQUEST_HOPPER_CIPHER_KEY = 160
    const val REQUEST_HOPPER_EMERGENCY_STOP = 172
    const val REQUEST_HOPPER_PURGE = 121
    const val REQUEST_HOPPER_COIN_ID = 131

    // Coin selector headers
    const val SET_MASTER_INHIBIT = 228
    const val GET_MASTER_INHIBIT = 227
    const val SET_DEFAULT_SORTER_PATH = 189
    const val GET_DEFAULT_SORTER_PATH = 188
    const val SETUP_ESCROW_STATE = 240
    const val COIN_PRECISION = 100
    const val MODIFY_SORTER_OVERRIDE = 222
    const val MODIFY_SORTER_PATH = 210
    const val READ_BUFFERED_CREDIT = 229
    const val REQUEST_COIN_ID = 184
    const val REQUEST_SEL_INHIBIT_STATUS = 230
    const val REQUEST_SORTER_PATH = 209
    const val REQUEST_SORTER_OVERRIDE = 221
    const val PRE_COIN_INHIBIT_COMPAT = 179
    const val MODIFY_INHIBIT_STATUS = 231

  }

  private val manufacturerId by cachedCcTalkProp {
    talkCcStringResponse {
      withDefaults(this@CcTalkDevice)
      header(CcTalkCommand.REQUEST_MANUFACTURER_ID)
    }
  }

  suspend fun manufacturerId(forceRefresh: Boolean = false): Either<CcTalkError, String> =
    manufacturerId.get(forceRefresh)

  private val equipmentCategoryId by cachedCcTalkProp {
    talkCcStringResponse {
      withDefaults(this@CcTalkDevice)
      header(CcTalkCommand.REQUEST_CATEGORY_ID)
    }
  }

  suspend fun equipmentCategoryId(forceRefresh: Boolean = false): Either<CcTalkError, String> =
    equipmentCategoryId.get(forceRefresh)

  private val productCode by cachedCcTalkProp {
    talkCcStringResponse {
      withDefaults(this@CcTalkDevice)
      header(CcTalkCommand.REQUEST_PRODUCT_CODE)
    }
  }

  suspend fun productCode(forceRefresh: Boolean = false): Either<CcTalkError, String> = productCode.get(forceRefresh)

  private val serialNumber by cachedCcTalkProp {
    talkCcLongResponseReversed {
      withDefaults(this@CcTalkDevice)
      header(CcTalkCommand.REQUEST_SERIAL_NUMBER)
    }
  }

  suspend fun serialNumber(forceRefresh: Boolean = false): Either<CcTalkError, Long> = serialNumber.get(forceRefresh)

  private val softwareVersion by cachedCcTalkProp {
    talkCcStringResponse {
      withDefaults(this@CcTalkDevice)
      header(CcTalkCommand.REQUEST_SOFTWARE_REVISION)
    }
  }

  suspend fun softwareVersion(forceRefresh: Boolean = false): Either<CcTalkError, String> =
    softwareVersion.get(forceRefresh)

  suspend fun simplePoll(): Either<CcTalkError, CcTalkStatus> =
    talkCcNoResponse {
      withDefaults(this@CcTalkDevice)
      header(CcTalkCommand.SIMPLE_POLL)
    }

  suspend fun deviceInformation(): Either<CcTalkError, DeviceInformation> = either {
    DeviceInformation(
      manufacturerId().bind(),
      equipmentCategoryId().bind(),
      CcTalkCategory.from(equipmentCategoryId().bind()),
      productCode().bind(),
      serialNumber().bind(),
      softwareVersion().bind()
    )
  }
}
