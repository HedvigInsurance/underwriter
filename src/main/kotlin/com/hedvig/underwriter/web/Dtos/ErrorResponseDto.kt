package com.hedvig.underwriter.web.Dtos

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue

data class ErrorResponseDto(
    val errorCode: ErrorCodes = ErrorCodes.UNKNOWN_ERROR_CODE,
    val errorMessage: String
)

enum class ErrorCodes {
    MEMBER_HAS_EXISTING_INSURANCE,
    MEMBER_BREACHES_UW_GUIDELINES,
    MEMBER_QUOTE_HAS_EXPIRED,

    @JsonEnumDefaultValue
    UNKNOWN_ERROR_CODE
}
