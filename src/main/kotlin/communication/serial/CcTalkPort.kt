package communication.serial

import arrow.core.Either
import be.inotek.communication.packet.CcTalkPacket
import be.inotek.communication.packet.CcTalkPacketBuilder
import communication.CcTalkStatus
import communication.serde.CcTalkSerializer
import communication.serial.ConcurrentSerialPort.SerialError
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

interface CcTalkPort {
  suspend fun talkCcNoResponse(packet: CcTalkPacketBuilder.() -> Unit): CcTalkStatus
  suspend fun talkCcNoResponse(packet: CcTalkPacket): CcTalkStatus
  suspend fun talkCcLongResponse(packet: CcTalkPacketBuilder.() -> Unit): Either<CcTalkStatus, Long>
  suspend fun talkCcLongResponse(packet: CcTalkPacket): Either<CcTalkStatus, Long>
  suspend fun talkCcLongResponseReversed(packet: CcTalkPacketBuilder.() -> Unit): Either<CcTalkStatus, Long>
  suspend fun talkCcLongResponseReversed(packet: CcTalkPacket): Either<CcTalkStatus, Long>
  suspend fun talkCcStringResponse(packet: CcTalkPacketBuilder.() -> Unit): Either<CcTalkStatus, String>
  suspend fun talkCcStringResponse(packet: CcTalkPacket): Either<CcTalkStatus, String>
  suspend fun talkCcStringResponseReverse(packet: CcTalkPacketBuilder.() -> Unit): Either<CcTalkStatus, String>
  suspend fun talkCcStringResponseReverse(packet: CcTalkPacket): Either<CcTalkStatus, String>
  suspend fun <T> talkCc(packet: CcTalkPacketBuilder.() -> Unit, onSuccess: (CcTalkPacket) -> T): Either<CcTalkStatus, T>
  suspend fun <T> talkCc(packet: CcTalkPacket, onSuccess: (CcTalkPacket) -> T): Either<CcTalkStatus, T>
  suspend fun talkCc(packet: CcTalkPacketBuilder.() -> Unit): Either<CcTalkStatus, CcTalkPacket>
  suspend fun talkCc(packet: CcTalkPacket): Either<CcTalkStatus, CcTalkPacket>
}

class CcTalkPortImp(
  private val port: ConcurrentPort,
  private val serializer: CcTalkSerializer,
): CcTalkPort {
  override suspend fun talkCcNoResponse(packet: CcTalkPacketBuilder.() -> Unit): CcTalkStatus =
    talkCcNoResponse(CcTalkPacket.build(packet))

  override suspend fun talkCcNoResponse(packet: CcTalkPacket): CcTalkStatus =
    talkCc(packet)
      .fold(
        { error -> error },
        { response -> CcTalkStatus.Ok }
      )

  override suspend fun talkCcLongResponse(packet: CcTalkPacketBuilder.() -> Unit): Either<CcTalkStatus, Long> =
    talkCcLongResponse(CcTalkPacket.build(packet))

  @OptIn(ExperimentalUnsignedTypes::class)
  override suspend fun talkCcLongResponse(packet: CcTalkPacket): Either<CcTalkStatus, Long> =
    talkCc(packet) { response ->
      response.data
        .sumOf { it.toLong() }
    }

  override suspend fun talkCcLongResponseReversed(packet: CcTalkPacketBuilder.() -> Unit): Either<CcTalkStatus, Long> =
    talkCcLongResponseReversed(CcTalkPacket.build(packet))

  @OptIn(ExperimentalUnsignedTypes::class)
  override suspend fun talkCcLongResponseReversed(packet: CcTalkPacket): Either<CcTalkStatus, Long> =
    talkCc(packet) { response ->
      response.data
        .reversed()
        .sumOf { it.toLong() }
    }

  override suspend fun talkCcStringResponse(packet: CcTalkPacketBuilder.() -> Unit): Either<CcTalkStatus, String> =
    talkCcStringResponse(CcTalkPacket.build(packet))

  @OptIn(ExperimentalUnsignedTypes::class)
  override suspend fun talkCcStringResponse(packet: CcTalkPacket): Either<CcTalkStatus, String> =
    talkCc(packet) { response ->
      response.data
        .filter { it >= 0x1fu && it <= 0x80u }
        .map { it.toInt().toChar() }
        .joinToString("", transform = { it.toString() })
    }

  override suspend fun talkCcStringResponseReverse(packet: CcTalkPacketBuilder.() -> Unit): Either<CcTalkStatus, String> =
    talkCcStringResponseReverse(CcTalkPacket.build(packet))

  @OptIn(ExperimentalUnsignedTypes::class)
  override suspend fun talkCcStringResponseReverse(packet: CcTalkPacket): Either<CcTalkStatus, String> =
    talkCc(packet) { response ->
      response.data
        .reversed()
        .filter { it >= 0x1fu && it <= 0x80u }
        .map { it.toInt().toChar() }
        .joinToString("", transform = { it.toString() })
    }

  override suspend fun <T> talkCc(
    packet: CcTalkPacketBuilder.() -> Unit,
    onSuccess: (CcTalkPacket) -> T
  ): Either<CcTalkStatus, T> =
    talkCc(packet)
      .fold(
        { error -> Either.Left(error) },
        { response -> Either.Right(onSuccess(response)) }
      )

  override suspend fun <T> talkCc(packet: CcTalkPacket, onSuccess: (CcTalkPacket) -> T): Either<CcTalkStatus, T> =
    talkCc(packet)
      .fold(
        { error -> Either.Left(error) },
        { response -> Either.Right(onSuccess(response)) }
      )

  override suspend fun talkCc(packet: CcTalkPacketBuilder.() -> Unit): Either<CcTalkStatus, CcTalkPacket> =
    talkCc(CcTalkPacket.build(packet))

  override suspend fun talkCc(packet: CcTalkPacket): Either<CcTalkStatus, CcTalkPacket> = withContext(NonCancellable) {
    val (serialized, _) = serializer.serialize(packet)

    port
      .sendPacket(serialized)
      .fold(
        { error -> Either.Left(error.status) },
        { response -> serializer.deserialize(response, packet.checksumType) }
      )
  }
}