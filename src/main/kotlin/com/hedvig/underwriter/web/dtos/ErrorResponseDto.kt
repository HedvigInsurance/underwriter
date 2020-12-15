package com.hedvig.underwriter.web.dtos

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue

data class BreachedGuideline(
    val message: String,
    val code: String
) {
    override fun toString() = "[code: $code, message: $message]"
}

data class ErrorResponseDto(
    override val errorCode: ErrorCodes = ErrorCodes.UNKNOWN_ERROR_CODE,
    override val errorMessage: String,
    val breachedUnderwritingGuidelines: List<BreachedGuideline>? = null
) : BasicErrorResponseDto

enum class ErrorCodes {
    MEMBER_HAS_EXISTING_INSURANCE,
    MEMBER_BREACHES_UW_GUIDELINES,
    MEMBER_QUOTE_HAS_EXPIRED,
    NO_SUCH_QUOTE,
    INVALID_STATE,
    MEMBER_DOES_NOT_HAVE_EXISTING_SIGNED_INSURANCE,
    MEMBER_ID_IS_NOT_PROVIDED,

    @JsonEnumDefaultValue
    UNKNOWN_ERROR_CODE
}
