package cctalk

sealed class CcTalkError(
  val message: String,
  val status: CcTalkStatus = CcTalkStatus.Unknown
) {
  override fun toString(): String = "$status: $message"

  data class DataLengthError(
    val startRange: Int,
    val endRange: Int,
    val actualLength: Int
  ) : CcTalkError("expected from $startRange bytes to $endRange, got $actualLength", CcTalkStatus.DataFormat)

  data class DataFormatError(
    val expectedLength: Int,
    val actualLength: Int
  ) : CcTalkError("expected $expectedLength bytes, got $actualLength", CcTalkStatus.DataFormat)

  class PayloadError(message: String) : CcTalkError(message, CcTalkStatus.DataFormat)

  class WrongParameterError(message: String) : CcTalkError(message, CcTalkStatus.WrongParameter)

  class ChecksumError() : CcTalkError("invalid checksum", CcTalkStatus.ChSumErr)

  class TimeoutError() : CcTalkError("Timeout error", CcTalkStatus.RcvTimeout)

  data class UnknownError(val error: String = "unknown") : CcTalkError(error, CcTalkStatus.Unknown)

  data class PortError(val port: String) : CcTalkError("$port is not available", CcTalkStatus.BadLine)

  data class CommunicationError(val issue: String) : CcTalkError(issue, CcTalkStatus.CommError)

  class ReadError(message: String) : CcTalkError(message, CcTalkStatus.ReceiveError)

  class WriteError(message: String) : CcTalkError(message, CcTalkStatus.SendErr)

  class UnsupportedError(message: String) : CcTalkError(message, CcTalkStatus.UnSupported)
}