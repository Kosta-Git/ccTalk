package cctalk.device

import be.inotek.communication.CcTalkChecksumTypes
import cctalk.CcTalkStatus
import cctalk.LedStatus
import cctalk.has
import cctalk.payout.has
import cctalk.serial.CcTalkPort

class DongleDevice(
  port: CcTalkPort,
  address: Byte = 80,
  checksumType: CcTalkChecksumTypes = CcTalkChecksumTypes.Simple8
) : CcTalkDevice(port, address, checksumType) {

  @OptIn(ExperimentalUnsignedTypes::class)
  suspend fun setLedState(ledNumber: Int, status: LedStatus, time: Int): CcTalkStatus {
    if (ledNumber < 0 || ledNumber > 7) return CcTalkStatus.WrongParameter
    if (status.has(LedStatus.BLINK) && time !in 50..12500) return CcTalkStatus.WrongParameter

    talkCc {
      var payload = ByteArray(2);
      payload[0] = ledNumber.toByte();
      when (status) {
        LedStatus.OFF -> {
          header(107u)
          payload[1] = 1
        }

        LedStatus.ON -> {
          header(107u)
          payload[1] = 0
        }

        LedStatus.BLINK -> {
          header(108u)
          payload[1] = (time / 50).toByte()
        }
      }
      destination(address.toUByte())
      source(CcTalkCommand.SOURCE_ADDRESS)
      data(payload.toUByteArray())
      checksumType(checksumType)
    }.fold(
      { error -> return error },
      { response -> return CcTalkStatus.Ok }
    )
  }
}