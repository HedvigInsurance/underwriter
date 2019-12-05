package com.hedvig.underwriter.serviceIntegration.memberService

import com.hedvig.underwriter.serviceIntegration.memberService.dtos.EditMemberRequest
import com.hedvig.underwriter.serviceIntegration.memberService.dtos.HelloHedvigResponseDto
import com.hedvig.underwriter.serviceIntegration.memberService.dtos.IsSsnAlreadySignedMemberResponse
import com.hedvig.underwriter.serviceIntegration.memberService.dtos.PersonStatusDto
import com.hedvig.underwriter.serviceIntegration.memberService.dtos.UnderwriterQuoteSignResponse
import com.hedvig.underwriter.serviceIntegration.memberService.dtos.UpdateSsnRequest
import com.hedvig.underwriter.web.dtos.UnderwriterQuoteSignRequest
import feign.Headers
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader

@Headers("Accept: application/json;charset=utf-8")
@FeignClient(
    name = "memberServiceClient",
    url = "\${hedvig.member-service.url:member-service}"
)
interface MemberServiceClient {

    @PostMapping("v2/member/helloHedvig")
    fun createMember(): ResponseEntity<HelloHedvigResponseDto>

    @GetMapping("/_/person/status/{ssn}")
    fun personStatus(@PathVariable("ssn") ssn: String): ResponseEntity<PersonStatusDto>

    @PostMapping("/_/debt/check/{ssn}")
    fun checkPersonDebt(@PathVariable("ssn") ssn: String): ResponseEntity<Void>

    @PostMapping("/v2/member/sign/underwriter")
    fun signQuote(
        @RequestHeader(value = "hedvig.token") memberId: Long,
        @RequestBody underwriterQuoteSignRequest: UnderwriterQuoteSignRequest
    ): ResponseEntity<UnderwriterQuoteSignResponse>

    @PostMapping("/_/member/{memberId}/updateSSN")
    fun updateMemberSsn(@PathVariable memberId: Long, @RequestBody request: UpdateSsnRequest)

    @GetMapping("/v2/member/sign/signedSSN")
    fun checkIsSsnAlreadySignedMemberEntity(@RequestHeader ssn: String): IsSsnAlreadySignedMemberResponse

    @PostMapping("/_/member/{memberId}/edit")
    fun editMember(@RequestHeader("Authorization") token: String, @PathVariable memberId: Long, @RequestBody request: EditMemberRequest): ResponseEntity<Void>
}
