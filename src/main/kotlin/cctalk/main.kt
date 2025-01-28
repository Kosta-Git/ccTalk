package cctalk

import arrow.core.Either
import be.inotek.communication.CcTalkChecksumTypes
import cctalk.device.PayoutDevice
import cctalk.device.SelectorDevice
import cctalk.scanner.ScannableDevice
import cctalk.serde.CcTalkSerializerImpl
import cctalk.serial.CcTalkPortImpl
import cctalk.serial.ConcurrentSerialPort
import com.fazecast.jSerialComm.SerialPort
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun main() = withContext(Dispatchers.Default) {
  val portName = "/dev/cu.usbserial-whCCT1478159"
  val serialPort = SerialPort.getCommPort(portName)
  serialPort.openPort()
  val port = ConcurrentSerialPort(serialPort, localEcho = true)
  port.open()
  val ccTalkPort = CcTalkPortImpl(port, CcTalkSerializerImpl())

  val devicesToScan = listOf(
    //ScannableDevice(2u, CcTalkChecksumTypes.Simple8),
    ScannableDevice(80, CcTalkChecksumTypes.Simple8),
    ScannableDevice(2, CcTalkChecksumTypes.CRC16),
    /*
        ScannableDevice(3u, CcTalkChecksumTypes.Simple8),
        ScannableDevice(4u, CcTalkChecksumTypes.Simple8),
        ScannableDevice(5u, CcTalkChecksumTypes.Simple8),
        ScannableDevice(6u, CcTalkChecksumTypes.Simple8),
        ScannableDevice(7u, CcTalkChecksumTypes.Simple8),
        ScannableDevice(8u, CcTalkChecksumTypes.Simple8),
        ScannableDevice(9u, CcTalkChecksumTypes.Simple8),
        ScannableDevice(10u, CcTalkChecksumTypes.Simple8),
        ScannableDevice(40u, CcTalkChecksumTypes.CRC16),
        ScannableDevice(40u, CcTalkChecksumTypes.Simple8),
        ScannableDevice(80u, CcTalkChecksumTypes.Simple8),
        ScannableDevice(130u, CcTalkChecksumTypes.Simple8),
        ScannableDevice(131u, CcTalkChecksumTypes.Simple8),
        ScannableDevice(132u, CcTalkChecksumTypes.Simple8),
        ScannableDevice(133u, CcTalkChecksumTypes.Simple8),
        ScannableDevice(134u, CcTalkChecksumTypes.Simple8),
        ScannableDevice(140u, CcTalkChecksumTypes.Simple8),
        ScannableDevice(160u, CcTalkChecksumTypes.Simple8)*/
  )

  var payout = PayoutDevice(ccTalkPort, 3, CcTalkChecksumTypes.Simple8)
  payout.deviceInformation().let { println(it.getOrNull() ?: "null") }
}
