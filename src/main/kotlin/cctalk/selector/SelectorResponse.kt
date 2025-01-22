package cctalk.selector

data class PollSelectorResponseList(
  val eventCount: Int,
  val missedEventCount: Int,
  val pollResponses: List<PollSelectorResponse>
)

data class PollSelectorResponse(
  val coinInserted: Boolean,
  val status: SelectorPollEvent,
  val coinIndex: Int,
  val coinPath: Int
)