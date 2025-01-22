package cctalk.device

import be.inotek.communication.CcTalkChecksumTypes
import cctalk.serial.CcTalkPort

@OptIn(ExperimentalUnsignedTypes::class)
class SelectorDevice(
  port: CcTalkPort,
  address: Byte = 2,
  checksumType: CcTalkChecksumTypes = CcTalkChecksumTypes.Simple8
) : CcTalkDevice(port, address, checksumType) {
  companion object {
    const val MAX_EVENT_POLL = 5
  }

  
}