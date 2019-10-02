package com.hedvig.underwriter.web.Dtos

import com.hedvig.underwriter.model.IncompleteQuoteData
import com.hedvig.underwriter.model.LineOfBusiness
import com.hedvig.underwriter.model.ProductType

data class PostIncompleteQuoteRequest(
        val productType: ProductType,
        val lineOfBusiness: LineOfBusiness?,
        val ssn: String?,
        val incompleteQuoteDataDto: IncompleteQuoteData?
)
