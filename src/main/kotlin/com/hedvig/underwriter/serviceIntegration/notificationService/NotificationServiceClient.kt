package com.hedvig.underwriter.serviceIntegration.notificationService

import org.springframework.cloud.openfeign.FeignClient
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody

@FeignClient(
    name = "notification-service",
    url = "\${hedvig.notification-service.url:http://notification-service}"
)
interface NotificationServiceClient {

    @PostMapping("_/customerio/{memberId}")
    fun post(@PathVariable memberId: String, @RequestBody body: Map<String, Any?>): ResponseEntity<String>
}
