package cctalk.validator

data class ValidatorPollResponse(
    val status: ValidatorPollEvent,
    val billIndex: Int,
    val billPosition: ValidatorBillPosition
)

data class ValidatorPollResponseList(
    val events: Int,
    val lostEvents: Int,
    val responses: List<ValidatorPollResponse>
)