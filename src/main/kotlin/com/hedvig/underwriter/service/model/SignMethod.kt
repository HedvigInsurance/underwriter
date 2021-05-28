package com.hedvig.underwriter.service.model

import com.hedvig.underwriter.graphql.type.SignMethod as SignMethodGraphQL

enum class SignMethod {
    SWEDISH_BANK_ID,
    NORWEGIAN_BANK_ID,
    DANISH_BANK_ID,
    SIMPLE_SIGN,
    SELF_CHANGE_COMMIT;

    fun toGraphQL() = when (this) {
        SWEDISH_BANK_ID -> SignMethodGraphQL.SWEDISH_BANK_ID
        NORWEGIAN_BANK_ID -> SignMethodGraphQL.NORWEGIAN_BANK_ID
        DANISH_BANK_ID -> SignMethodGraphQL.DANISH_BANK_ID
        SIMPLE_SIGN -> SignMethodGraphQL.SIMPLE_SIGN
        SELF_CHANGE_COMMIT -> SignMethodGraphQL.SELF_CHANGE_COMMIT
    }
}
