package cctalk.device

import arrow.core.Either
import arrow.core.raise.either
import be.inotek.communication.CcTalkChecksumTypes
import be.inotek.communication.packet.CcTalkPacket
import be.inotek.communication.packet.CcTalkPacketBuilder
import cctalk.CcTalkCategory
import cctalk.CcTalkError
import cctalk.CcTalkStatus
import cctalk.serial.CcTalkPort

open class CcTalkDevice(
  val port: CcTalkPort,
  val address: Byte,
  val checksumType: CcTalkChecksumTypes
) : CcTalkPort by port {
  object CcTalkCommand {
    const val SOURCE_ADDRESS: UByte = 1u

    // Common headers
    const val SIMPLE_POLL: UByte = 254u
    const val REQUEST_MANUFACTURER_ID: UByte = 246u
    const val REQUEST_CATEGORY_ID: UByte = 245u
    const val REQUEST_PRODUCT_CODE: UByte = 244u
    const val REQUEST_SERIAL_NUMBER: UByte = 242u
    const val REQUEST_SOFTWARE_REVISION: UByte = 241u

    // Dongle headers
    const val LED_ON_OFF: UByte = 107u
    const val LED_BLINK: UByte = 108u

    // Payout headers
    const val REQUEST_HOPPER_STATUS: UByte = 166u
    const val REQUEST_HOPPER_LEVEL_STATUS: UByte = 217u
    const val REQUEST_HOPPER_TEST: UByte = 163u
    const val HOPPER_ENABLE: UByte = 164u
    const val REQUEST_HOPPER_DISPENSE: UByte = 167u
    const val REQUEST_HOPPER_PUMP_RNG: UByte = 161u
    const val REQUEST_HOPPER_CIPHER_KEY: UByte = 160u
    const val REQUEST_HOPPER_EMERGENCY_STOP: UByte = 172u
    const val REQUEST_HOPPER_PURGE: UByte = 121u
    const val REQUEST_HOPPER_COIN_ID: UByte = 131u

  }

  private val manufacturerId by cachedCcTalkProp {
    talkCcStringResponse {
      withDefaults(this@CcTalkDevice)
      header(CcTalkCommand.REQUEST_MANUFACTURER_ID)
    }
  }
  suspend fun manufacturerId(forceRefresh: Boolean = false): Either<CcTalkError, String> = manufacturerId.get(forceRefresh)

  private val equipmentCategoryId by cachedCcTalkProp {
    talkCcStringResponse {
      withDefaults(this@CcTalkDevice)
      header(CcTalkCommand.REQUEST_CATEGORY_ID)
    }
  }
  suspend fun equipmentCategoryId(forceRefresh: Boolean = false): Either<CcTalkError, String> = equipmentCategoryId.get(forceRefresh)

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
  suspend fun softwareVersion(forceRefresh: Boolean = false): Either<CcTalkError, String> = softwareVersion.get(forceRefresh)

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
