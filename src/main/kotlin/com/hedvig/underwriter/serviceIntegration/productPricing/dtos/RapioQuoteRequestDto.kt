package com.hedvig.underwriter.serviceIntegration.productPricing.dtos

import com.hedvig.underwriter.model.HomeProductSubType
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

data class RapioQuoteRequestDto(
    val currentTotalPrice: BigDecimal,
    val firstName: String,
    val lastName: String,
    var birthDate: LocalDate,
    val isStudent: Boolean,
    val address: Address,
    val livingSpace: Float,
    val houseType: HomeProductSubType,
    val currentInsurer: String?,
    val houseHoldSize: Int,
    val activeFrom: LocalDateTime?,
    val ssn: String,
    val emailAddress: String,
    val phoneNumber: String
)
