package com.hedvig.underwriter.graphql

import com.hedvig.graphql.commons.errors.GraphQlErrorException
import graphql.ErrorType

class EmptyBundleQueryException : GraphQlErrorException(
    "You need to supply at least one contractId",
    ErrorType.ValidationError,
    mapOf("code" to "InputvalidationError")
)
