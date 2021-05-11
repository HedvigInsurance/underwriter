package com.hedvig.underwriter.service

import com.hedvig.underwriter.model.Quote
import javax.money.MonetaryAmount

interface RequotingService {

    fun blockDueToExistingAgreement(quote: Quote): Boolean
    fun useOldOrNewPrice(quote: Quote, newPrice: MonetaryAmount): MonetaryAmount
}
