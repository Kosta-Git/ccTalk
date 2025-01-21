package cctalk.device

import arrow.core.Either
import be.inotek.communication.CcTalkChecksumTypes
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
  }

  suspend fun deviceCategory(): Either<CcTalkError, String> =
    talkCcStringResponse {
      destination(address.toUByte())
      source(CcTalkCommand.SOURCE_ADDRESS)
      header(248u)
      checksumType(checksumType)
    }

  suspend fun simplePoll(): Either<CcTalkError, CcTalkStatus> =
    talkCcNoResponse {
      destination(address.toUByte())
      source(CcTalkCommand.SOURCE_ADDRESS)
      header(254u)
      checksumType(checksumType)
    }
}
