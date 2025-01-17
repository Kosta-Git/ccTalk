package communication

import java.util.EnumSet

enum class LedStatus(val code: Int) {
  OFF(0),
  ON(1),
  BLINK(2);

  infix fun LedStatus.and(other: LedStatus): LedStatuses = EnumSet.of(this, other)
  infix fun LedStatus.has(other: LedStatus): Boolean = this == other
}

typealias LedStatuses = EnumSet<LedStatus>

infix fun LedStatus.and(other: LedStatus): LedStatuses = EnumSet.of(this, other)
infix fun LedStatuses.and(other: LedStatus): LedStatuses {
  this.add(other)
  return this
}

infix fun LedStatuses.has(other: LedStatus): Boolean = this.contains(other)
infix fun LedStatus.has(other: LedStatus): Boolean = this == other