package com.hedvig.underwriter.service.model

import com.hedvig.underwriter.graphql.type.SignMethod as SignMethodGraphQL

enum class SignMethod {
    SWEDISH_BANK_ID,
    NORWEGIAN_BANK_ID,
    DANISH_BANK_ID,
    SIMPLE_SIGN,
    APPROVE_ONLY;

    fun toGraphQL() = when (this) {
        SWEDISH_BANK_ID -> SignMethodGraphQL.SWEDISH_BANK_ID
        NORWEGIAN_BANK_ID -> SignMethodGraphQL.NORWEGIAN_BANK_ID
        DANISH_BANK_ID -> SignMethodGraphQL.DANISH_BANK_ID
        SIMPLE_SIGN -> SignMethodGraphQL.SIMPLE_SIGN
        APPROVE_ONLY -> SignMethodGraphQL.APPROVE_ONLY
    }
}
