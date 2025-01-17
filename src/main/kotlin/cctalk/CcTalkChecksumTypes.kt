package be.inotek.communication

/**
 * ccTalk cash devices checksum type.
 *
 *
 * Currently simple 8-Bit and 16-bit CRC (x^16+x^12+x^5+1) are implemented.
 */
enum class CcTalkChecksumTypes {
  /**
   * Simple 8-bit checksum.
   */
  Simple8,

  /**
   * 16-bit CRC checksum.
   */
  CRC16
}