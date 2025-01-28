package cctalk.device

import arrow.core.Either
import arrow.core.raise.either
import be.inotek.communication.CcTalkChecksumTypes
import cctalk.CcTalkError
import cctalk.CcTalkStatus
import cctalk.LedStatus
import cctalk.has
import cctalk.serial.CcTalkPort

class DongleDevice(
  port: CcTalkPort,
  address: Int = 80,
  checksumType: CcTalkChecksumTypes = CcTalkChecksumTypes.Simple8
) : CcTalkDevice(port, address, checksumType) {

  suspend fun setLedState(ledNumber: Int, status: LedStatus, time: Int): Either<CcTalkError, CcTalkStatus> = either {
    if (ledNumber < 0 || ledNumber > 7)
      raise(CcTalkError.WrongParameterError("led number must be between 0 and 7, got: $ledNumber"))
    if (status.has(LedStatus.BLINK) && time !in 50..12500)
      raise(CcTalkError.WrongParameterError("time must be between 50 and 12500, got: $time"))

    talkCcNoResponse {
      withDefaults(this@DongleDevice)
      var payload = ByteArray(2)
      payload[0] = ledNumber.toByte()
      when (status) {
        LedStatus.OFF -> {
          header(107)
          payload[1] = 1
        }

        LedStatus.ON -> {
          header(107)
          payload[1] = 0
        }

        LedStatus.BLINK -> {
          header(108)
          payload[1] = (time / 50).toByte()
        }
      }
      data(payload)
    }.bind()
  }
}