package com.hedvig.underwriter.serviceIntegration.memberService.dtos

data class UpdateMemberNameAndSsnDto(
    val firstName: String,
    val lastName: String,
    val ssn: String
)
