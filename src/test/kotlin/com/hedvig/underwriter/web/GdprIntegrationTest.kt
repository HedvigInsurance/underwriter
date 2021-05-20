package com.hedvig.underwriter.web

import com.hedvig.underwriter.serviceIntegration.memberService.dtos.IsSsnAlreadySignedMemberResponse
import com.hedvig.underwriter.serviceIntegration.memberService.dtos.UnderwriterQuoteSignResponse
import com.hedvig.underwriter.serviceIntegration.notificationService.NotificationServiceClient
import com.hedvig.underwriter.serviceIntegration.priceEngine.dtos.PriceQueryResponse
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.contract.CreateContractResponse
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.javamoney.moneta.Money
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isGreaterThan
import assertk.assertions.isNotEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import com.graphql.spring.boot.test.GraphQLTestTemplate
import com.hedvig.graphql.commons.type.MonetaryAmountV2
import com.hedvig.productPricingObjects.dtos.Agreement
import com.hedvig.productPricingObjects.enums.AgreementStatus
import com.hedvig.underwriter.graphql.type.InsuranceCost
import com.hedvig.underwriter.model.DanishAccidentData
import com.hedvig.underwriter.model.DanishHomeContentsData
import com.hedvig.underwriter.model.DanishTravelData
import com.hedvig.underwriter.model.NorwegianHomeContentsData
import com.hedvig.underwriter.model.NorwegianTravelData
import com.hedvig.underwriter.model.QuoteData
import com.hedvig.underwriter.model.SwedishApartmentData
import com.hedvig.underwriter.model.SwedishHouseData
import com.hedvig.underwriter.serviceIntegration.apigateway.ApiGatewayServiceClient
import com.hedvig.underwriter.serviceIntegration.memberService.MemberServiceClient
import com.hedvig.underwriter.serviceIntegration.memberService.dtos.Flag
import com.hedvig.underwriter.serviceIntegration.memberService.dtos.HelloHedvigResponseDto
import com.hedvig.underwriter.serviceIntegration.memberService.dtos.PersonStatusDto
import com.hedvig.underwriter.serviceIntegration.priceEngine.PriceEngineClient
import com.hedvig.underwriter.serviceIntegration.productPricing.ProductPricingClient
import com.hedvig.underwriter.testhelp.GdprClient
import com.hedvig.underwriter.testhelp.QuoteClient
import io.mockk.mockk
import io.mockk.verify
import org.jdbi.v3.core.Jdbi
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.ResponseEntity
import org.springframework.test.context.junit4.SpringRunner
import java.lang.RuntimeException
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Random
import java.util.UUID

@RunWith(SpringRunner::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GdprIntegrationTest {

    @Autowired
    lateinit var quoteClient: QuoteClient

    @Autowired
    lateinit var gdprClient: GdprClient

    @Autowired
    lateinit var graphQLTestTemplate: GraphQLTestTemplate

    @Autowired
    lateinit var jdbi: Jdbi

    @MockkBean(relaxed = true)
    lateinit var notificationServiceClient: NotificationServiceClient

    @MockkBean(relaxed = true)
    lateinit var apiGatewayServiceClient: ApiGatewayServiceClient

    @MockkBean(relaxed = true)
    lateinit var priceEngineClient: PriceEngineClient

    @MockkBean(relaxed = true)
    lateinit var memberServiceClient: MemberServiceClient

    @MockkBean(relaxed = true)
    lateinit var productPricingClient: ProductPricingClient

    val activeAgreement = Agreement.SwedishApartment(UUID.randomUUID(), mockk(), mockk(), mockk(), null, AgreementStatus.ACTIVE, mockk(), mockk(), 0, 100)

    @Before
    fun setup() {

        // Added this snippet to make sure we do not forget to add cleaning/deletion tests for new types when added
        val makeSureAllTypesAreTested = fun (type: QuoteData) {
            when (type) {
                is SwedishApartmentData,
                is SwedishHouseData,
                is NorwegianHomeContentsData,
                is NorwegianTravelData,
                is DanishAccidentData,
                is DanishHomeContentsData,
                is DanishTravelData -> "Done"
            }
        }

        every { memberServiceClient.personStatus(any()) } returns ResponseEntity.status(200).body(PersonStatusDto(Flag.GREEN))
        every { memberServiceClient.checkPersonDebt(any()) } returns ResponseEntity.status(200).body(null)
        every { memberServiceClient.checkIsSsnAlreadySignedMemberEntity(any()) } returns IsSsnAlreadySignedMemberResponse(false)
        every { memberServiceClient.createMember() } returns ResponseEntity.status(200).body(HelloHedvigResponseDto("12345"))
        every { memberServiceClient.updateMemberSsn(any(), any()) } returns Unit
        every { memberServiceClient.signQuote(any(), any()) } returns ResponseEntity.status(200).body(UnderwriterQuoteSignResponse(1L, true))
        every { memberServiceClient.finalizeOnBoarding(any(), any()) } returns ResponseEntity.status(200).body("")
        every { productPricingClient.createContract(any(), any()) } returns listOf(CreateContractResponse(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()))
        every { productPricingClient.getAgreement(any()) } returns ResponseEntity.status(200).body(activeAgreement)
        every { productPricingClient.calculateInsuranceCost(any(), any()) } returns ResponseEntity.status(200).body(InsuranceCost(
            MonetaryAmountV2.Companion.of(11.0, "SEK"),
            MonetaryAmountV2.Companion.of(12.1, "SEK"),
            MonetaryAmountV2.Companion.of(12.0, "SEK"),
            LocalDate.now()))
        every { priceEngineClient.queryPrice(any()) } returns PriceQueryResponse(UUID.randomUUID(), Money.of(12, "NOK"))
    }

    @Test
    fun `Test deleting quotes`() {

        val seApartmentRsp = quoteClient.createSwedishApartmentQuote()
        val seHouseRsp = quoteClient.createSwedishHouseQuote()
        val noHomeRsp = quoteClient.createNorwegianHomeContentQuote()
        val noTravelRsp = quoteClient.createNorwegianTravelQuote()
        val dkHomeRsp = quoteClient.createDanishHomeContentQuote()
        val dkAccidentRsp = quoteClient.createDanishAccidentQuote()
        val dkTravelRsp = quoteClient.createDanishTravelQuote()

        assertDeleteQuote(seApartmentRsp.id, "SwedishApartmentData")
        assertDeleteQuote(seHouseRsp.id, "SwedishHouseData")
        assertDeleteQuote(noHomeRsp.id, "NorwegianHomeContentsData")
        assertDeleteQuote(noTravelRsp.id, "NorwegianTravelData")
        assertDeleteQuote(dkHomeRsp.id, "DanishHomeContentsData")
        assertDeleteQuote(dkAccidentRsp.id, "DanishAccidentData")
        assertDeleteQuote(dkTravelRsp.id, "DanishTravelData")
    }

    private fun assertDeleteQuote(quoteId: UUID, expType: String) {
        val revs = getQuoteRevsFromDb(quoteId)

        assertThat(revs.size).isGreaterThan(0)
        assertThat(revs.all { it.seApartmentId != null })

        // Delete it
        val delResponse1 = quoteClient.deleteQuote(quoteId)
        assertThat(delResponse1.statusCodeValue).isEqualTo(204)

        // Validate all gone
        assertNoQuoteInDb(quoteId, revs)

        // Validate we have an anonymised copy
        val deleted = getDeletedQuoteFromDb(quoteId)
        assertThat(deleted.type).isEqualTo(expType)

        // Delete it again
        val delResponse2 = quoteClient.deleteQuote(quoteId)
        assertThat(delResponse2.statusCodeValue).isEqualTo(404)
    }

    @Test
    fun `Test that deleting quote with agreement fails`() {

        with(quoteClient.createSwedishApartmentQuote(
            ssn = "199110112399",
            street = "ApStreet 12312",
            zip = "1234",
            city = "ApCity"
        )) {
            // Sign it
            quoteClient.signQuote(id, "Apan", "Apansson", "apan@apansson.se")

            val revs = getQuoteRevsFromDb(id)

            assertThat(revs.size).isGreaterThan(0)
            assertThat(revs.all { it.seApartmentId != null })

            val response = quoteClient.deleteQuote(id)

            assertThat(response.statusCodeValue).isEqualTo(403)
        }
    }

    @Test
    fun `Test quote cleaning job`() {

        val seApartmentRsp = quoteClient.createSwedishApartmentQuote()
        val seHouseRsp = quoteClient.createSwedishHouseQuote()
        val noHomeRsp = quoteClient.createNorwegianHomeContentQuote()
        val noTravelRsp = quoteClient.createNorwegianTravelQuote()
        val dkHomeRsp = quoteClient.createDanishHomeContentQuote()
        val dkAccidentRsp = quoteClient.createDanishAccidentQuote()
        val dkTravelRsp = quoteClient.createDanishTravelQuote()

        assertCleanJob(seApartmentRsp.id)
        assertCleanJob(seHouseRsp.id)
        assertCleanJob(noHomeRsp.id)
        assertCleanJob(noTravelRsp.id)
        assertCleanJob(dkHomeRsp.id)
        assertCleanJob(dkAccidentRsp.id)
        assertCleanJob(dkTravelRsp.id)
    }

    @Test
    fun `Test hashed ssn for deleted quotes`() {

        val ssn1quote1 = quoteClient.createSwedishApartmentQuote(ssn = "199205262398")
        val ssn1quote2 = quoteClient.createSwedishApartmentQuote(ssn = "199205262398")
        val ssn2quote1 = quoteClient.createSwedishApartmentQuote(ssn = "199507272392")

        val now = Instant.now()
        val nowMinus30d = now.plus(-30, ChronoUnit.DAYS)

        updateCreatedAt(ssn1quote1.id, nowMinus30d)
        updateCreatedAt(ssn1quote2.id, nowMinus30d)
        updateCreatedAt(ssn2quote1.id, nowMinus30d)

        gdprClient.clean()

        assertNoQuoteExist(ssn1quote1.id)
        assertNoQuoteExist(ssn1quote2.id)
        assertNoQuoteExist(ssn2quote1.id)

        val ssn1Quote1Hash = getDeletedQuoteFromDb(ssn1quote1.id).hashedSsn
        val ssn1Quote2Hash = getDeletedQuoteFromDb(ssn1quote2.id).hashedSsn
        val ssn2Quote1Hash = getDeletedQuoteFromDb(ssn2quote1.id).hashedSsn

        assertThat(ssn1Quote1Hash).isNotNull()
        assertThat(ssn1Quote2Hash).isNotNull()
        assertThat(ssn2Quote1Hash).isNotNull()
        assertThat(ssn1Quote1Hash).isEqualTo(ssn1Quote2Hash)
        assertThat(ssn1Quote1Hash).isNotEqualTo(ssn2Quote1Hash)
    }

    @Test
    fun `Test quote cleaning job is not removing quotes with an agreement`() {

        val seApartmentRsp = quoteClient.createSwedishApartmentQuote()

        quoteClient.signQuote(seApartmentRsp.id)

        assertCleanJob(seApartmentRsp.id, false)
    }

    @Test
    fun `Test cleaning quote with member`() {

        val memberId = Random().nextLong().toString()

        val quoteId = createGraphQlQuote(memberId)

        assertCleanJob(quoteId)

        verify(exactly = 1) { notificationServiceClient.deleteMember(memberId) }
        verify(exactly = 1) { apiGatewayServiceClient.deleteMember(any(), memberId) }
        verify(exactly = 1) { memberServiceClient.deleteMember(memberId) }

        // TODO: Add verify checks to member and lookup services when implemented
    }

    @Test
    fun `Test cleaning job with requested dry-run`() {

        val memberId = Random().nextLong().toString()
        val quoteId = createGraphQlQuote(memberId)

        val nowMinus30d = Instant.now().plus(-30, ChronoUnit.DAYS)
        updateCreatedAt(quoteId, nowMinus30d)

        gdprClient.clean(dryRun = true)

        assertQuoteExist(quoteId)

        verify(exactly = 0) { notificationServiceClient.deleteMember(memberId) }
        verify(exactly = 0) { apiGatewayServiceClient.deleteMember(any(), memberId) }
        verify(exactly = 0) { memberServiceClient.deleteMember(memberId) }
    }

    @Test
    fun `Test cleaning job with requested days`() {

        val memberId = Random().nextLong().toString()
        val quoteId = createGraphQlQuote(memberId)

        val nowMinus20d = Instant.now().plus(-20, ChronoUnit.DAYS)
        updateCreatedAt(quoteId, nowMinus20d)

        gdprClient.clean(days = 10)

        assertQuoteExist(quoteId)

        verify(exactly = 0) { notificationServiceClient.deleteMember(memberId) }
        verify(exactly = 0) { apiGatewayServiceClient.deleteMember(any(), memberId) }
        verify(exactly = 0) { memberServiceClient.deleteMember(memberId) }

        val nowMinus60d = Instant.now().plus(-60, ChronoUnit.DAYS)
        updateCreatedAt(quoteId, nowMinus60d)

        gdprClient.clean(days = 61)

        assertQuoteExist(quoteId)

        verify(exactly = 0) { notificationServiceClient.deleteMember(memberId) }
        verify(exactly = 0) { apiGatewayServiceClient.deleteMember(any(), memberId) }
        verify(exactly = 0) { memberServiceClient.deleteMember(memberId) }

        gdprClient.clean(days = 59, dryRun = true)

        assertQuoteExist(quoteId)

        verify(exactly = 0) { notificationServiceClient.deleteMember(memberId) }
        verify(exactly = 0) { apiGatewayServiceClient.deleteMember(any(), memberId) }
        verify(exactly = 0) { memberServiceClient.deleteMember(memberId) }

        gdprClient.clean(days = 59)

        assertNoQuoteExist(quoteId)

        verify(exactly = 1) { notificationServiceClient.deleteMember(memberId) }
        verify(exactly = 1) { apiGatewayServiceClient.deleteMember(any(), memberId) }
        verify(exactly = 1) { memberServiceClient.deleteMember(memberId) }
    }

    @Test
    fun `Test cleaning quote with member having other quotes`() {

        val memberId = Random().nextLong().toString()

        val quoteId1 = createGraphQlQuote(memberId)
        val quoteId2 = createGraphQlQuote(memberId)

        assertCleanJob(quoteId1)

        // Since user has another quote than the quote deleted he/she is not removed in other services
        verify(exactly = 0) { notificationServiceClient.deleteMember(memberId) }
        verify(exactly = 0) { apiGatewayServiceClient.deleteMember(any(), memberId) }
        verify(exactly = 0) { memberServiceClient.deleteMember(memberId) }
    }

    private fun createMutation() =
        javaClass.getResource("/mutations/createApartmentQuote.graphql")
            .readText()
            .replace("00000000-0000-0000-0000-000000000000", UUID.randomUUID().toString())

    private fun assertCleanJob(quoteId: UUID, cleaned: Boolean = true) {

        val now = Instant.now()
        val nowMinus29d = now.plus(-29, ChronoUnit.DAYS)
        val nowMinus30d = now.plus(-30, ChronoUnit.DAYS)

        assertQuoteExist(quoteId)

        gdprClient.clean()

        assertQuoteExist(quoteId)

        updateCreatedAt(quoteId, nowMinus29d)

        gdprClient.clean()

        assertQuoteExist(quoteId)

        updateCreatedAt(quoteId, nowMinus30d)

        gdprClient.clean()

        if (cleaned) {
            assertNoQuoteExist(quoteId)
        } else {
            assertQuoteExist(quoteId)
        }
    }

    private fun assertNoQuoteExist(id: UUID) {
        assertThat(quoteClient.getQuote(id)).isNull()
    }

    private fun assertQuoteExist(id: UUID) {
        assertThat(quoteClient.getQuote(id)).isNotNull()
    }

    private fun getQuoteRevsFromDb(quoteId: UUID): List<QuoteRev> {
        return jdbi.withHandle<List<QuoteRev>, RuntimeException> { handle ->

            val sql = """
                SELECT 
                r.master_quote_id as quoteId,
                se_a.internal_id as seApartmentId,
                se_h.internal_id as seHomeId,
                no_h.internal_id as noHomeId,
                no_t.internal_id as noTravelId,
                dk_h.internal_id as dkHomeId,
                dk_a.internal_id as dkAccidentId,
                dk_t.internal_id as dkTravelId
                FROM quote_revisions r
                LEFT JOIN quote_revision_apartment_data se_a ON r.quote_apartment_data_id = se_a.internal_id
                LEFT JOIN quote_revision_house_data se_h ON r.quote_house_data_id = se_h.internal_id
                LEFT JOIN quote_revision_norwegian_home_contents_data no_h ON r.quote_norwegian_home_contents_data_id = no_h.internal_id
                LEFT JOIN quote_revision_norwegian_travel_data no_t ON r.quote_norwegian_travel_data_id = no_t.internal_id
                LEFT JOIN quote_revision_danish_home_contents_data dk_h ON r.quote_danish_home_contents_data_id = dk_h.internal_id
                LEFT JOIN quote_revision_danish_accident_data dk_a ON r.quote_danish_accident_data_id = dk_a.internal_id
                LEFT JOIN quote_revision_danish_travel_data dk_t ON r.quote_danish_travel_data_id = dk_t.internal_id
                WHERE r.master_quote_id = :quoteId
            """.trimIndent()

            handle.createQuery(sql)
                .bind("quoteId", quoteId)
                .mapTo(QuoteRev::class.java)
                .list()
        }
    }

    data class QuoteRev(
        val quoteId: UUID,
        val seApartmentId: Int?,
        val seHouseId: Int?,
        val noHomeId: Int?,
        val noTravelId: Int?,
        val dkHomeId: Int?,
        val dkAccidentId: Int?,
        val dkTravelId: Int?
    )

    private fun assertNoQuoteInDb(quoteId: UUID, quoteRevs: List<QuoteRev>) {
        jdbi.withHandle<Unit, RuntimeException> { handle ->

            assertThat(
                handle.createQuery("SELECT count(*) FROM master_quotes WHERE id = :quoteId")
                    .bind("quoteId", quoteId)
                    .mapTo(Int::class.java)
                    .findOnly()
            ).isEqualTo(0)

            assertThat(
                handle.createQuery("SELECT count(*) FROM quote_revisions WHERE master_quote_id = :quoteId")
                    .bind("quoteId", quoteId)
                    .mapTo(Int::class.java)
                    .findOnly()
            ).isEqualTo(0)

            quoteRevs.forEach {
                it.seApartmentId?.let {
                    assertThat(
                        handle.createQuery("SELECT count(*) FROM quote_revision_apartment_data WHERE internal_id = :internalId")
                            .bind("internalId", it)
                            .mapTo(Int::class.java)
                            .findOnly()
                    ).isEqualTo(0)
                }
                it.seHouseId?.let {
                    assertThat(
                        handle.createQuery("SELECT count(*) FROM quote_revision_house_data WHERE internal_id = :internalId")
                            .bind("internalId", it)
                            .mapTo(Int::class.java)
                            .findOnly()
                    ).isEqualTo(0)
                }
                it.noHomeId?.let {
                    assertThat(
                        handle.createQuery("SELECT count(*) FROM quote_revision_norwegian_home_contents_data WHERE internal_id = :internalId")
                            .bind("internalId", it)
                            .mapTo(Int::class.java)
                            .findOnly()
                    ).isEqualTo(0)
                }
                it.noTravelId?.let {
                    assertThat(
                        handle.createQuery("SELECT count(*) FROM quote_revision_norwegian_travel_data WHERE internal_id = :internalId")
                            .bind("internalId", it)
                            .mapTo(Int::class.java)
                            .findOnly()
                    ).isEqualTo(0)
                }
                it.dkHomeId?.let {
                    assertThat(
                        handle.createQuery("SELECT count(*) FROM quote_revision_danish_home_contents_data WHERE internal_id = :internalId")
                            .bind("internalId", it)
                            .mapTo(Int::class.java)
                            .findOnly()
                    ).isEqualTo(0)
                }
                it.dkAccidentId?.let {
                    assertThat(
                        handle.createQuery("SELECT count(*) FROM quote_revision_danish_accident_data WHERE internal_id = :internalId")
                            .bind("internalId", it)
                            .mapTo(Int::class.java)
                            .findOnly()
                    ).isEqualTo(0)
                }
                it.dkTravelId?.let {
                    assertThat(
                        handle.createQuery("SELECT count(*) FROM quote_revision_danish_travel_data WHERE internal_id = :internalId")
                            .bind("internalId", it)
                            .mapTo(Int::class.java)
                            .findOnly()
                    ).isEqualTo(0)
                }
            }
        }
    }

    data class DeletedQuote(
        val quoteId: UUID,
        val createdAt: Instant,
        val deletedAt: Instant,
        val type: String,
        val memberId: String?,
        val hashedSsn: String?,
        val quote: String,
        val revs: String
    )

    private fun getDeletedQuoteFromDb(quoteId: UUID): DeletedQuote =
        jdbi.withHandle<DeletedQuote, RuntimeException> { handle ->
            handle.createQuery("SELECT * FROM deleted_quotes WHERE quote_id = :quoteId")
                .bind("quoteId", quoteId)
                .mapTo(DeletedQuote::class.java)
                .findOnly()
        }

    private fun updateCreatedAt(quoteId: UUID, createdAt: Instant): Unit =
        jdbi.withHandle<Unit, RuntimeException> { handle ->
            handle.createUpdate("UPDATE master_quotes SET created_at = :createdAt WHERE id = :quoteId")
                .bind("quoteId", quoteId)
                .bind("createdAt", createdAt)
                .execute()
        }

    private fun createGraphQlQuote(memberId: String): UUID {
        graphQLTestTemplate.clearHeaders()
        graphQLTestTemplate.addHeader("hedvig.token", memberId)

        val response = graphQLTestTemplate.postMultipart(createMutation(), "{}")
        assert(response.isOk)

        return UUID.fromString(response.readTree()["data"]["createQuote"]["id"].asText())
    }
}
