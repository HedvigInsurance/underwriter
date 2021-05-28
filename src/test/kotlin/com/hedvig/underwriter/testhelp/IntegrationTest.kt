package com.hedvig.underwriter.testhelp

import com.hedvig.underwriter.serviceIntegration.apigateway.ApiGatewayServiceClient
import com.hedvig.underwriter.serviceIntegration.memberService.MemberServiceClient
import com.hedvig.underwriter.serviceIntegration.notificationService.NotificationServiceClient
import com.hedvig.underwriter.serviceIntegration.priceEngine.PriceEngineClient
import com.hedvig.underwriter.serviceIntegration.productPricing.ProductPricingClient
import com.ninjasquad.springmockk.MockkBean
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class IntegrationTest {

    @MockkBean(relaxed = true)
    lateinit var notificationServiceClient: NotificationServiceClient

    @MockkBean(relaxed = true)
    lateinit var priceEngineClient: PriceEngineClient

    @MockkBean(relaxed = true)
    lateinit var memberServiceClient: MemberServiceClient

    @MockkBean(relaxed = true)
    lateinit var productPricingClient: ProductPricingClient

    @MockkBean(relaxed = true)
    lateinit var apiGatewayServiceClient: ApiGatewayServiceClient
}
