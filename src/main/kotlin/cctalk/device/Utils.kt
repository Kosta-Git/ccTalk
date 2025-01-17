package cctalk.device

import arrow.core.Either
import be.inotek.communication.packet.CcTalkPacket
import cctalk.CcTalkStatus

fun Either<CcTalkStatus, CcTalkPacket>.hasError(): Boolean {
  return isLeft()
}

fun Either<CcTalkStatus, CcTalkPacket>.error(): CcTalkStatus {
  return leftOrNull() ?: CcTalkStatus.Unknown
}

fun Either<CcTalkStatus, CcTalkPacket>.packet(): CcTalkPacket {
  return getOrNull()!!
}