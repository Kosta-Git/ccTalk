package cctalk.serial

import arrow.core.Either
import arrow.core.raise.either
import be.inotek.communication.packet.CcTalkPacket
import be.inotek.communication.packet.CcTalkPacketBuilder
import cctalk.CcTalkError
import cctalk.CcTalkStatus
import cctalk.serde.CcTalkSerializer
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

interface CcTalkPort {
  suspend fun talkCcNoResponse(packet: CcTalkPacketBuilder.() -> Unit): Either<CcTalkError, CcTalkStatus>
  suspend fun talkCcNoResponse(packet: CcTalkPacket): Either<CcTalkError, CcTalkStatus>
  suspend fun talkCcLongResponse(packet: CcTalkPacketBuilder.() -> Unit): Either<CcTalkError, Long>
  suspend fun talkCcLongResponse(packet: CcTalkPacket): Either<CcTalkError, Long>
  suspend fun talkCcLongResponseReversed(packet: CcTalkPacketBuilder.() -> Unit): Either<CcTalkError, Long>
  suspend fun talkCcLongResponseReversed(packet: CcTalkPacket): Either<CcTalkError, Long>
  suspend fun talkCcStringResponse(packet: CcTalkPacketBuilder.() -> Unit): Either<CcTalkError, String>
  suspend fun talkCcStringResponse(packet: CcTalkPacket): Either<CcTalkError, String>
  suspend fun talkCcStringResponseReverse(packet: CcTalkPacketBuilder.() -> Unit): Either<CcTalkError, String>
  suspend fun talkCcStringResponseReverse(packet: CcTalkPacket): Either<CcTalkError, String>
  suspend fun <T> talkCc(
    packet: CcTalkPacketBuilder.() -> Unit,
    onSuccess: (CcTalkPacket) -> T
  ): Either<CcTalkError, T>

  suspend fun <T> talkCc(packet: CcTalkPacket, onSuccess: (CcTalkPacket) -> T): Either<CcTalkError, T>
  suspend fun talkCc(packet: CcTalkPacketBuilder.() -> Unit): Either<CcTalkError, CcTalkPacket>
  suspend fun talkCc(packet: CcTalkPacket): Either<CcTalkError, CcTalkPacket>
}

class CcTalkPortImp(
  private val port: ConcurrentPort,
  private val serializer: CcTalkSerializer,
) : CcTalkPort {
  override suspend fun talkCcNoResponse(packet: CcTalkPacketBuilder.() -> Unit): Either<CcTalkError, CcTalkStatus> =
    talkCcNoResponse(CcTalkPacket.build(packet))

  override suspend fun talkCcNoResponse(packet: CcTalkPacket): Either<CcTalkError, CcTalkStatus> = either {
    talkCc(packet).bind()
    CcTalkStatus.Ok
  }

  override suspend fun talkCcLongResponse(packet: CcTalkPacketBuilder.() -> Unit): Either<CcTalkError, Long> =
    talkCcLongResponse(CcTalkPacket.build(packet))

  @OptIn(ExperimentalUnsignedTypes::class)
  override suspend fun talkCcLongResponse(packet: CcTalkPacket): Either<CcTalkError, Long> = either {
    talkCc(packet)
      .bind()
      .data
      .sumOf { it.toLong() }
  }

  override suspend fun talkCcLongResponseReversed(packet: CcTalkPacketBuilder.() -> Unit): Either<CcTalkError, Long> =
    talkCcLongResponseReversed(CcTalkPacket.build(packet))

  @OptIn(ExperimentalUnsignedTypes::class)
  override suspend fun talkCcLongResponseReversed(packet: CcTalkPacket): Either<CcTalkError, Long> = either {
    talkCc(packet)
      .bind()
      .data
      .reversed()
      .sumOf { it.toLong() }
  }

  override suspend fun talkCcStringResponse(packet: CcTalkPacketBuilder.() -> Unit): Either<CcTalkError, String> =
    talkCcStringResponse(CcTalkPacket.build(packet))

  @OptIn(ExperimentalUnsignedTypes::class)
  override suspend fun talkCcStringResponse(packet: CcTalkPacket): Either<CcTalkError, String> = either {
    talkCc(packet)
      .bind()
      .data
      .filter { it >= 0x1fu && it <= 0x80u }
      .map { it.toInt().toChar() }
      .joinToString("", transform = { it.toString() })
  }

  override suspend fun talkCcStringResponseReverse(packet: CcTalkPacketBuilder.() -> Unit): Either<CcTalkError, String> =
    talkCcStringResponseReverse(CcTalkPacket.build(packet))

  @OptIn(ExperimentalUnsignedTypes::class)
  override suspend fun talkCcStringResponseReverse(packet: CcTalkPacket): Either<CcTalkError, String> = either {
    talkCc(packet)
      .bind()
      .data
      .reversed()
      .filter { it >= 0x1fu && it <= 0x80u }
      .map { it.toInt().toChar() }
      .joinToString("", transform = { it.toString() })
  }

  override suspend fun <T> talkCc(
    packet: CcTalkPacketBuilder.() -> Unit,
    onSuccess: (CcTalkPacket) -> T
  ): Either<CcTalkError, T> = either {
    onSuccess(talkCc(packet).bind())
  }

  override suspend fun <T> talkCc(packet: CcTalkPacket, onSuccess: (CcTalkPacket) -> T): Either<CcTalkError, T> =
    either {
      onSuccess(talkCc(packet).bind())
    }

  override suspend fun talkCc(packet: CcTalkPacketBuilder.() -> Unit): Either<CcTalkError, CcTalkPacket> =
    talkCc(CcTalkPacket.build(packet))

  override suspend fun talkCc(packet: CcTalkPacket): Either<CcTalkError, CcTalkPacket> = withContext(NonCancellable) {
    either {
      val (serialized, _) = serializer.serialize(packet)
      val response = port.sendPacket(serialized).bind()
      serializer.deserialize(response, packet.checksumType).bind()
    }
  }
}