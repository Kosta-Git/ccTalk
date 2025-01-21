package cctalk.device

import arrow.core.Either
import arrow.core.raise.RaiseDSL
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

fun Either<CcTalkStatus, CcTalkPacket>.status(): CcTalkStatus {
  return fold(
    { error -> error },
    { response -> CcTalkStatus.Ok }
  )
}