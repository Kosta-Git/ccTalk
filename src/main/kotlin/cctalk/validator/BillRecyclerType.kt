package cctalk.validator

enum class BillRecyclerType {
    /// No recycler present.
    None,
    /// JCM VEGA bill validator with recycler, connected via ccTalk.
    JCMVegaCcTalk,
}