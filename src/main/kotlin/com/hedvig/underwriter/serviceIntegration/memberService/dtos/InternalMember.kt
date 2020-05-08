package com.hedvig.underwriter.serviceIntegration.memberService.dtos

import java.time.Instant
import java.time.LocalDate

enum class Gender {
    MALE,
    FEMALE
}

data class InternalMember(
    val memberId: Long,
    val status: String?,
    val ssn: String,
    val gender: Gender?,
    val firstName: String,
    val lastName: String,
    val street: String?,
    val floor: Int?,
    val apartment: String?,
    val city: String?,
    val zipCode: String?,
    val country: String?,
    val email: String?,
    val phoneNumber: String?,
    val birthDate: LocalDate,
    val signedOn: Instant?,
    val createdOn: Instant?,
    val fraudulentStatus: String?,
    val fraudulentDescription: String?,
    val acceptLanguage: String?
)
