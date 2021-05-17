package com.hedvig.underwriter.web

import com.hedvig.libs.logging.calls.LogCall
import com.hedvig.underwriter.service.GdprService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/_/v1/gdpr")
class GdprController(
    val gdprService: GdprService
) {
    @PostMapping("/clean")
    @LogCall
    fun clean(@RequestParam("dry-run") dryRun: Boolean?, @RequestParam("days") days: Long?): ResponseEntity<out Any> {

        gdprService.clean(dryRun, days)

        return ResponseEntity.noContent().build()
    }
}
