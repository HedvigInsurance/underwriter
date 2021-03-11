package com.hedvig.underwriter.testhelp.databuilder

import com.hedvig.underwriter.model.DanishHomeContentsData
import com.hedvig.underwriter.model.DanishHomeContentsType
import com.hedvig.underwriter.model.QuoteData
import java.time.LocalDate
import java.util.UUID

data class DanishHomeContentsDataBuilder(
    val id: UUID = UUID.fromString("ab5924e4-0c72-11ea-a337-4865ee119be5"),
    val ssn: String? = "1212120000",
    val birthDate: LocalDate = LocalDate.of(1912, 12, 12),
    val firstName: String = "",
    val lastName: String = "",
    val email: String? = "em@i.l",
    val phoneNumber: String? = null,
    val street: String = "",
    val zipCode: String = "",
    val coInsured: Int = 3,
    val livingSpace: Int = 25,
    val isStudent: Boolean = false,
    val type: DanishHomeContentsType = DanishHomeContentsType.RENT,
    val bbrId: String? = "1234"
) : DataBuilder<QuoteData> {

    override fun build() = DanishHomeContentsData(
        id = id,
        ssn = ssn,
        birthDate = birthDate,
        firstName = firstName,
        lastName = lastName,
        email = email,
        phoneNumber = phoneNumber,
        street = street,
        zipCode = zipCode,
        livingSpace = livingSpace,
        coInsured = coInsured,
        isStudent = isStudent,
        type = type,
        bbrId = bbrId
    )
}
