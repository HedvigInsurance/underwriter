package com.hedvig.underwriter.serviceIntegration.memberService.dtos

import com.hedvig.underwriter.util.logging.Masked

data class UpdateContactInformationRequest(
    var memberId: String,
    @Masked val firstName: String,
    @Masked val lastName: String,
    @Masked val email: String,
    @Masked val phoneNumber: String,
    val addressMemberService: AddressMemberService,
    @Masked val ssn: String

)
