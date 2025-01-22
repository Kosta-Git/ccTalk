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
  protected val port: CcTalkPort,
  protected val address: Byte,
  protected val checksumType: CcTalkChecksumTypes
) : CcTalkPort by port {
  object CcTalkCommand {
    const val SOURCE_ADDRESS: UByte = 1u

    const val SIMPLE_POLL: UByte = 254u
    const val REQUEST_MANUFACTURER_ID: UByte = 246u
    const val REQUEST_CATEGORY_ID: UByte = 245u
    const val REQUEST_PRODUCT_CODE: UByte = 244u
    const val REQUEST_SERIAL_NUMBER: UByte = 242u
    const val REQUEST_SOFTWARE_REVISION: UByte = 241u
  }

  private val manufacturerId by cachedCcTalkProp {
    talkCcStringResponse {
      destination(address.toUByte())
      source(CcTalkCommand.SOURCE_ADDRESS)
      header(CcTalkCommand.REQUEST_MANUFACTURER_ID)
      checksumType(checksumType)
    }
  }
  suspend fun manufacturerId(forceRefresh: Boolean = false): Either<CcTalkError, String> = manufacturerId.get(forceRefresh)

  private val equipmentCategoryId by cachedCcTalkProp {
    talkCcStringResponse {
      destination(address.toUByte())
      source(CcTalkCommand.SOURCE_ADDRESS)
      header(CcTalkCommand.REQUEST_CATEGORY_ID)
      checksumType(checksumType)
    }
  }
  suspend fun equipmentCategoryId(forceRefresh: Boolean = false): Either<CcTalkError, String> = equipmentCategoryId.get(forceRefresh)

  private val productCode by cachedCcTalkProp {
    talkCcStringResponse {
      destination(address.toUByte())
      source(CcTalkCommand.SOURCE_ADDRESS)
      header(CcTalkCommand.REQUEST_PRODUCT_CODE)
      checksumType(checksumType)
    }
  }
  suspend fun productCode(forceRefresh: Boolean = false): Either<CcTalkError, String> = productCode.get(forceRefresh)

  private val serialNumber by cachedCcTalkProp {
    talkCcLongResponseReversed {
      destination(address.toUByte())
      source(CcTalkCommand.SOURCE_ADDRESS)
      header(CcTalkCommand.REQUEST_SERIAL_NUMBER)
      checksumType(checksumType)
    }
  }
  suspend fun serialNumber(forceRefresh: Boolean = false): Either<CcTalkError, Long> = serialNumber.get(forceRefresh)

  private val softwareVersion by cachedCcTalkProp {
    talkCcStringResponse {
      destination(address.toUByte())
      source(CcTalkCommand.SOURCE_ADDRESS)
      header(CcTalkCommand.REQUEST_SOFTWARE_REVISION)
      checksumType(checksumType)
    }
  }
  suspend fun softwareVersion(forceRefresh: Boolean = false): Either<CcTalkError, String> = softwareVersion.get(forceRefresh)

  suspend fun simplePoll(): Either<CcTalkError, CcTalkStatus> =
    talkCcNoResponse {
      destination(address.toUByte())
      source(CcTalkCommand.SOURCE_ADDRESS)
      header(CcTalkCommand.SIMPLE_POLL)
      checksumType(checksumType)
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
