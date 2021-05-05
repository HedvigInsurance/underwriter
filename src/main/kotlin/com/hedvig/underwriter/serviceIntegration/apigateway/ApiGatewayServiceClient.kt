package com.hedvig.underwriter.serviceIntegration.apigateway

import feign.Headers
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable

@Headers("Accept: application/json;charset=utf-8")
@FeignClient(
    name = "apiGatewayServiceClient",
    url = "\${hedvig.api-gateway.url:api-gateway}"
)
interface ApiGatewayServiceClient {

    @DeleteMapping("_/member/{memberId}")
    fun deleteMember(@PathVariable memberId: String): ResponseEntity<Any>
}
