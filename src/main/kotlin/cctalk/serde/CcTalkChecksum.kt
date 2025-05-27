package cctalk.serde

import be.inotek.communication.CcTalkChecksumTypes.CRC16
import be.inotek.communication.CcTalkChecksumTypes.Simple8
import cctalk.packet.CcTalkPacket

fun CcTalkPacket.getCheckSum() = when (checksumType) {
  Simple8 -> computeSimple8Checksum()
  CRC16 -> computeCrc16Checksum()
}

fun CcTalkPacket.isChecksumValid() = when (checksumType) {
  Simple8 -> isSimple8ChecksumValid()
  CRC16 -> isCrc16ChecksumValid()
}

fun CcTalkPacket.isCrc16ChecksumValid(): Boolean {
  val computedChecksum = computeCrc16Checksum()
  return (computedChecksum and 0xFF) == source && ((computedChecksum shr 8) and 0xFF) == checksum
}

fun CcTalkPacket.computeCrc16Checksum(): Int {
  var crc: UShort = 0x0000u
  (intArrayOf(destination, dataLength, header) + data).forEach {
    crc = crc xor (it shl 8).toUShort()
    for (j in 0 until 8) {
      if ((crc and 0x8000u).toInt() != 0)
        crc = ((crc.toInt() shl 1) xor 0x1021).toUShort()
      else
        crc = (crc.toInt() shl 1).toUShort()
    }
  }
  return crc.toInt()
}

fun CcTalkPacket.isSimple8ChecksumValid(): Boolean = computeSimple8Checksum() == checksum

fun CcTalkPacket.computeSimple8Checksum(): Int {
  var crc = destination.toInt() + dataLength.toInt() + source.toInt() + header.toInt()
  data.forEach { crc += it }
  return (256 - (crc and 0x00FF))
}