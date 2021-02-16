package com.hedvig.underwriter.web

import com.hedvig.underwriter.service.SignService
import com.hedvig.underwriter.service.model.CompleteSignSessionData
import com.hedvig.underwriter.web.dtos.SignRequest
import java.util.UUID
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(
    "/_/v1/signSession"
)
class SignSessionController @Autowired constructor(
    val signService: SignService
) {

    @PostMapping("/swedishBankid/{sessionId}/completed")
    fun swedishQuoteWasSigned(
        @PathVariable sessionId: UUID,
        @RequestBody requestBody: SignRequest
    ) {
        signService.completedSignSession(
            sessionId, CompleteSignSessionData.SwedishBankIdDataComplete(
                requestBody.referenceToken,
                requestBody.signature,
                requestBody.oscpResponse
            )
        )
    }

    @PostMapping("/{sessionId}/completed")
    fun signSessionComplete(
        @PathVariable sessionId: UUID
    ) {
        signService.completedSignSession(
            sessionId, CompleteSignSessionData.NoMandate
        )
    }
}
