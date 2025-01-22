package cctalk.device

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import be.inotek.communication.packet.CcTalkPacket
import be.inotek.communication.packet.CcTalkPacketBuilder
import cctalk.CcTalkError
import cctalk.CcTalkStatus
import cctalk.serial.CcTalkPort

@OptIn(ExperimentalUnsignedTypes::class)
class TestCcTalkPort(
  private val deviceAddress: UByte = 2u,
  private val responses: Map<UByte, (CcTalkPacket) -> Either<CcTalkError, CcTalkPacket>> = emptyMap(),
  private var forcedError: CcTalkError? = null
) : CcTalkPort {

  // Track the last packet sent for verification in tests
  var lastPacketSent: CcTalkPacket? = null
    private set

  // Track how many times each header was called
  private val headerCallCounts = mutableMapOf<UByte, Int>()

  fun getHeaderCallCount(header: UByte): Int = headerCallCounts[header] ?: 0

  override suspend fun talkCcNoResponse(packet: CcTalkPacketBuilder.() -> Unit): Either<CcTalkError, CcTalkStatus> =
    talkCcNoResponse(CcTalkPacket.build(packet))

  override suspend fun talkCcNoResponse(packet: CcTalkPacket): Either<CcTalkError, CcTalkStatus> =
    talkCc(packet)
      .fold(
        { error -> error.left() },
        { response -> CcTalkStatus.Ok.right() }
      )

  override suspend fun talkCcLongResponse(packet: CcTalkPacketBuilder.() -> Unit): Either<CcTalkError, Long> =
    talkCcLongResponse(CcTalkPacket.build(packet))

  override suspend fun talkCcLongResponse(packet: CcTalkPacket): Either<CcTalkError, Long> =
    talkCc(packet) { response ->
      response.data.sumOf { it.toLong() }
    }

  override suspend fun talkCcLongResponseReversed(packet: CcTalkPacketBuilder.() -> Unit): Either<CcTalkError, Long> =
    talkCcLongResponseReversed(CcTalkPacket.build(packet))

  override suspend fun talkCcLongResponseReversed(packet: CcTalkPacket): Either<CcTalkError, Long> =
    talkCc(packet) { response ->
      response.data.reversed().sumOf { it.toLong() }
    }

  override suspend fun talkCcStringResponse(packet: CcTalkPacketBuilder.() -> Unit): Either<CcTalkError, String> =
    talkCcStringResponse(CcTalkPacket.build(packet))

  override suspend fun talkCcStringResponse(packet: CcTalkPacket): Either<CcTalkError, String> =
    talkCc(packet) { response ->
      response.data
        .filter { it >= 0x1fu && it <= 0x80u }
        .map { it.toInt().toChar() }
        .joinToString("")
    }

  override suspend fun talkCcStringResponseReverse(packet: CcTalkPacketBuilder.() -> Unit): Either<CcTalkError, String> =
    talkCcStringResponseReverse(CcTalkPacket.build(packet))

  override suspend fun talkCcStringResponseReverse(packet: CcTalkPacket): Either<CcTalkError, String> =
    talkCc(packet) { response ->
      response.data
        .reversed()
        .filter { it >= 0x1fu && it <= 0x80u }
        .map { it.toInt().toChar() }
        .joinToString("")
    }

  override suspend fun <T> talkCc(
    packet: CcTalkPacketBuilder.() -> Unit,
    onSuccess: (CcTalkPacket) -> T
  ): Either<CcTalkError, T> =
    talkCc(CcTalkPacket.build(packet))
      .map(onSuccess)

  override suspend fun <T> talkCc(
    packet: CcTalkPacket,
    onSuccess: (CcTalkPacket) -> T
  ): Either<CcTalkError, T> =
    talkCc(packet)
      .map(onSuccess)

  override suspend fun talkCc(packet: CcTalkPacketBuilder.() -> Unit): Either<CcTalkError, CcTalkPacket> =
    talkCc(CcTalkPacket.build(packet))

  override suspend fun talkCc(packet: CcTalkPacket): Either<CcTalkError, CcTalkPacket> {
    lastPacketSent = packet
    headerCallCounts[packet.header] = (headerCallCounts[packet.header] ?: 0) + 1

    // Return forced error if set
    forcedError?.let { return Either.Left(it) }

    // Check if packet is intended for this device
    if (packet.destination != deviceAddress) {
      return Either.Left(CcTalkError.WrongParameterError("Packet not intended for this device"))
    }

    // Get response handler for this header
    val responseHandler = responses[packet.header] ?: { p ->
      // Default to ACK response if no handler defined
      Either.Right(CcTalkPacket.build {
        destination(1u) // Back to host
        source(deviceAddress)
        header(0u) // ACK
        checksumType(p.checksumType)
      })
    }

    return responseHandler(packet)
  }

  companion object {
    // Helper function to create standard response data
    fun createStringResponse(text: String): (CcTalkPacket) -> Either<CcTalkError, CcTalkPacket> = { packet ->
      Either.Right(CcTalkPacket.build {
        destination(1u)
        source(packet.destination)
        header(0u)
        data(text.map { it.code.toUByte() }.toUByteArray())
        checksumType(packet.checksumType)
      })
    }

    fun createLongResponse(value: Long): (CcTalkPacket) -> Either<CcTalkError, CcTalkPacket> = { packet ->
      Either.Right(CcTalkPacket.build {
        destination(1u)
        source(packet.destination)
        header(0u)
        data(ubyteArrayOf(value.toUByte()))
        checksumType(packet.checksumType)
      })
    }
  }
}