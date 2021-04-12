package com.hedvig.underwriter.service.model

import com.hedvig.underwriter.web.dtos.ErrorResponseDto

object StartSignErrors {

    val memberIsAlreadySigned = StartSignResponse.FailedToStartSign(
        "An error occurred: Unable to sign member",
        "MEMBER_IS_ALREADY_SIGNED"
    )

    val variousMemberId = StartSignResponse.FailedToStartSign(
        "creation and signing must be made by the same member",
        "DIFFERENT_MEMBER_ID_ON_QUOTE_AND_SIGN_REQUEST"
    )

    val noMemberIdOnQuote = StartSignResponse.FailedToStartSign(
        "quotes must have member id to be able to sign",
        "NO_MEMBER_ID_ON_QUOTE"
    )

    val personalInfoNotMatching = StartSignResponse.FailedToStartSign(
        "quotes must have matching firstName, lastName, ssn, birthDate and email",
        "PERSONAL_INFO_NOT_MATCHING"
    )

    val targetURLNotProvided =
        StartSignResponse.FailedToStartSign(
            "Bad request: Must provide `successUrl` and `failUrl` when starting norwegian sign",
            "TARGET_URL_NOT_PROVIDED"
        )

    fun fromErrorResponse(error: ErrorResponseDto) =
        StartSignResponse.FailedToStartSign(
            error.errorMessage,
            error.errorCode.name
        )

    fun failedToStartSign(message: String) =
        StartSignResponse.FailedToStartSign(
            message,
            "FAILED_TO_START_SIGN"
        )

    val quotesCanNotBeBundled =
        StartSignResponse.FailedToStartSign(
            "Quotes can not be bundled",
            "QUOTES_CAN_NOT_BE_BUNDLED"
        )

    val singleQuoteCanNotBeSignedAlone =
        StartSignResponse.FailedToStartSign(
            "Single quote can not be signed alone",
            "SINGLE_QUOTE_CAN_NOT_BE_SIGNED_ALONE"
        )

    val noQuotes =
        StartSignResponse.FailedToStartSign(
            "Quotes can not be empty",
            "EMPTY_LIST_OF_QUOTES"
        )

    fun failedToStartSimpleSign(message: String) =
        StartSignResponse.FailedToStartSign(
            message,
            "FAILED_TO_START_SIMPLE_SIGN"
        )
}
