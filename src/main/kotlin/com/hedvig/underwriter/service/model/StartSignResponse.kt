package com.hedvig.underwriter.service.model

import java.util.UUID

sealed class StartSignResponse {

    data class SwedishBankIdSession(
        val autoStartToken: String
    ) : StartSignResponse()

    @Deprecated("Not in use anymore")
    data class NorwegianBankIdSession(
        val redirectUrl: String
    ) : StartSignResponse()

    @Deprecated("Not in use anymore")
    data class DanishBankIdSession(
        val redirectUrl: String
    ) : StartSignResponse()

    data class SimpleSignSession(
        val id: UUID
    ) : StartSignResponse()

    object AlreadyCompleted : StartSignResponse() {
        val ignore = true
    }

    data class FailedToStartSign(
        val errorMessage: String,
        val errorCode: String
    ) : StartSignResponse()
}
