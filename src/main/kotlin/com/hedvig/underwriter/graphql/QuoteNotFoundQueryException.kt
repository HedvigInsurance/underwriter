package com.hedvig.underwriter.graphql

import com.hedvig.graphql.commons.errors.GraphQlErrorException
import graphql.ErrorType

class QuoteNotFoundQueryException(message: String) : GraphQlErrorException(
    message,
    ErrorType.ValidationError,
    mapOf("code" to "InputvalidationError")
)
