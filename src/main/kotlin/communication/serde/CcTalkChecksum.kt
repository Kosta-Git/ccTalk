package communication.serde

import be.inotek.communication.CcTalkChecksumTypes.CRC16
import be.inotek.communication.CcTalkChecksumTypes.Simple8
import be.inotek.communication.packet.CcTalkPacket

fun CcTalkPacket.setCheckSum() = when (checksumType) {
  Simple8 -> setSimple8Checksum()
  CRC16 -> setCrc16Checksum()
}

fun CcTalkPacket.isChecksumValid() = when (checksumType) {
  Simple8 -> isSimple8ChecksumValid()
  CRC16 -> isCrc16ChecksumValid()
}

@OptIn(ExperimentalUnsignedTypes::class)
fun CcTalkPacket.isCrc16ChecksumValid(): Boolean {
  var crc: UShort = computeCrc16Checksum()
  return source == (crc.toInt() and 0xFF).toUByte() &&
      checksum == ((crc.toInt() shr 0xFF) and 0xFF).toUByte()
}

@OptIn(ExperimentalUnsignedTypes::class)
fun CcTalkPacket.setCrc16Checksum() {
  var crc: UShort = computeCrc16Checksum()
  source = (crc.toInt() and 0xFF).toUByte()
  checksum = ((crc.toInt() shr 0xFF) and 0xFF).toUByte()
}

@OptIn(ExperimentalUnsignedTypes::class)
fun CcTalkPacket.computeCrc16Checksum(): UShort {
  var crc: UShort = 0x0000u
  for (i in 0 until dataLength.toInt() + 4) {
    if (i != 2) {
      crc = crc xor (rawData[i].toInt() shl 8).toUShort()
      for (j in 0 until 8) {
        if ((crc and 0x8000u).toInt() != 0)
          crc = crc xor 0x1021u
        else
          crc = (crc.toInt() shl 1).toUShort()
      }
    }
  }
  return crc
}

@OptIn(ExperimentalUnsignedTypes::class)
fun CcTalkPacket.isSimple8ChecksumValid(): Boolean {
  return computeSimple8Checksum() == checksum
}


@OptIn(ExperimentalUnsignedTypes::class)
fun CcTalkPacket.setSimple8Checksum() {
  checksum = computeSimple8Checksum()
}

@OptIn(ExperimentalUnsignedTypes::class)
fun CcTalkPacket.computeSimple8Checksum(): UByte {
  var crc = destination.toInt() + dataLength.toInt() + source.toInt() + header.toInt()
  for (i in 0 until dataLength.toInt()) {
    crc += rawData[i + 4].toInt()
  }
  return (256 - (crc and 0x00FF)).toUByte()
}