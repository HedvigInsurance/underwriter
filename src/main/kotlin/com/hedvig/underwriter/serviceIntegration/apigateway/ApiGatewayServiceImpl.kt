package com.hedvig.underwriter.serviceIntegration.apigateway

import com.hedvig.underwriter.service.exceptions.NotFoundException
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.lang.RuntimeException

@Service
class ApiGatewayServiceImpl(private val client: ApiGatewayServiceClient) : ApiGatewayService {

    @Value("\${hedvig.api-gateway.token:}")
    private var token: String = ""

    override fun deleteMember(memberId: String) {
        val response = client.deleteMember(token, memberId)

        if (response.statusCodeValue == 404) {
            throw NotFoundException("Failed to delete member $memberId in API Gateway, member not found")
        }

        if (response.statusCode.isError) {
            throw RuntimeException("Failed to delete member $memberId in API Gateway: $response")
        }
    }
}
