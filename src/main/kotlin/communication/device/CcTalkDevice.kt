package communication.device

import arrow.core.Either
import be.inotek.communication.CcTalkChecksumTypes
import communication.CcTalkStatus
import communication.serial.CcTalkPort

open class CcTalkDevice(
  protected val port: CcTalkPort,
  protected val address: Byte,
  protected val checksumType: CcTalkChecksumTypes
) : CcTalkPort by port {
  object CcTalkCommand {
    const val SOURCE_ADDRESS: UByte = 1u
  }

  suspend fun deviceCategory(): Either<CcTalkStatus, String> =
    talkCcStringResponse {
      destination(address.toUByte())
      source(CcTalkCommand.SOURCE_ADDRESS)
      header(248u)
      checksumType(checksumType)
    }

  suspend fun simplePoll(): CcTalkStatus {
    return talkCc {
      destination(address.toUByte())
      source(CcTalkCommand.SOURCE_ADDRESS)
      header(254u)
      checksumType(checksumType)
    }.fold(
      { error -> error },
      { response -> CcTalkStatus.Ok }
    )
  }
}
