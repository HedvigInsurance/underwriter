package com.hedvig.underwriter.web

import com.hedvig.underwriter.TestFakesConfiguration
import com.hedvig.underwriter.serviceIntegration.memberService.dtos.Flag
import com.hedvig.underwriter.serviceIntegration.memberService.dtos.PersonStatusDto
import com.hedvig.underwriter.serviceIntegration.priceEngine.dtos.PriceQueryResponse
import com.hedvig.underwriter.testhelp.TestHttpClient
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.javamoney.moneta.Money
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
internal class ExternalQuoteControllerTest {

    @Autowired
    lateinit var client: TestHttpClient

    @Autowired
    lateinit var config: TestFakesConfiguration

    @BeforeEach
    fun setUp() {
        config.memberServiceClient.personStatus = PersonStatusDto(Flag.GREEN)
        config.priceEngineClient.response = PriceQueryResponse(
            UUID.randomUUID(),
            Money.of(100, "SEK")
        )
    }

    @Test
    fun `can create a basic quote`() {
        val response = createQuote().assert2xx().body<CreateQuoteOutput>()
        assertThat(response.id).isNotNull()
        assertThat(response.price).isEqualTo(BigDecimal("100"))
        assertThat(response.currency).isEqualTo("SEK")
    }

    @Test
    fun `gets 422 underwriting guidelines breached if too young`() {
        val response = createQuote {
            birthDate = LocalDate.of(2017, 1, 1)
            ssn = "201701012393"
        }.assertStatus(HttpStatus.UNPROCESSABLE_ENTITY).body<CreateQuoteOutput>()

        assertThat(response.breachedUnderwritingGuidelines).isNotEmpty
    }

    @Test
    fun `can get a quote by id`() {
        val created = createQuote().body<CreateQuoteOutput>()
        val response = getQuote(created.id!!).body<Quote>()
        assertThat(response.id).isEqualTo(created.id)
    }

    @Test
    fun `quote initiatedFrom can be injected`() {
        val created = createQuote {
            initiatedFrom = "SELF_CHANGE"
        }.body<CreateQuoteOutput>()
        val response = getQuote(created.id!!).body<Quote>()
        assertThat(response.initiatedFrom).isEqualTo("SELF_CHANGE")
    }

    private fun createQuote(
        bodyChanges: CreateQuoteInput.() -> Unit = {}
    ): TestHttpClient.Response {
        val body = CreateQuoteInput(
            memberId = "mid",
            firstName = "Test",
            lastName = "Tester",
            birthDate = LocalDate.of(1991, 7, 27),
            ssn = "199107273097",
            startDate = LocalDate.of(2021, 6, 1),
            initiatedFrom = "APP",
            swedishApartmentData = mutableMapOf(
                "street" to "Fakestreet 123",
                "zipCode" to "12345",
                "livingSpace" to 44,
                "householdSize" to 1,
                "subType" to "RENT"
            )
        )
        body.bodyChanges()

        return client.post("/quotes", body)
    }

    private fun getQuote(id: UUID): TestHttpClient.Response = client.get("/quotes/$id")

    private data class CreateQuoteInput(
        var memberId: String? = null,
        var firstName: String? = null,
        var lastName: String? = null,
        var birthDate: LocalDate? = null,
        var ssn: String? = null,
        var startDate: LocalDate? = null,
        var initiatedFrom: String? = null,
        var swedishApartmentData: MutableMap<String, Any>? = null
    )

    private data class CreateQuoteOutput(
        val id: UUID? = null,
        val price: BigDecimal? = null,
        val currency: String? = null,
        val validTo: Instant? = null,
        val breachedUnderwritingGuidelines: List<Any>? = null
    )

    private data class Quote(
        val id: UUID,
        val initiatedFrom: String,
        val contractId: UUID? = null,
        val agreementId: UUID? = null
    )
}
