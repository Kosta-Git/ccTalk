package cctalk

enum class EscrowState(val state: Int) {
  Unknown(-1),
  Closed(0),
  Collect(1),
  Return(2)
}