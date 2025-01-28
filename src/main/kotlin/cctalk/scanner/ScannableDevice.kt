package cctalk.scanner

import arrow.core.Either
import be.inotek.communication.CcTalkChecksumTypes
import cctalk.CcTalkStatus
import cctalk.device.CcTalkDevice
import cctalk.serial.CcTalkPort

suspend fun scanDevices(port: CcTalkPort, devices: List<ScannableDevice>): List<CcTalkDevice> = devices
    .map { CcTalkDevice(port, it.address, it.checksumType) }
    .filter { it.simplePoll() is Either.Right }
    .filter { (it.simplePoll().getOrNull() ?: CcTalkStatus.Unknown) == CcTalkStatus.Ok }
    .toList()

data class ScannableDevice(
  val address: Int,
  val checksumType: CcTalkChecksumTypes
)

const val CC_CHANGE_GIVER_ADDRESS = 240
const val CC_BILL_VALIDATOR = 242
const val CC_CASHLESS = 244
// MDB Addresses
const val MDB_CHANGE_GIVER = 0x08;
const val MDB_BILL_VALIDATOR = 0x30;
//const val MDB_CASHLESS = new byte[] { 0x60, 0x10 }; // 0x18 };
