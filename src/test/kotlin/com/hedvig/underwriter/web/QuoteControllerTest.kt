package com.hedvig.underwriter.web;

import com.hedvig.underwriter.model.*
import com.hedvig.underwriter.service.QuoteService;
import org.junit.Test;
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Instant
import java.time.LocalDate
import java.util.*

@RunWith(SpringRunner::class)
@WebMvcTest(controllers = [QuoteController::class], secure = false)
internal class QuoteControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    lateinit var quoteService:QuoteService

    val createQuoteRequestJson = """
        {
            "quoteState": "INCOMPLETE",
            "dateStartedRecievingQuoteInfo": "2019-09-17T13:32:00.783981Z",
            "productType": "HOME",
            "incompleteQuoteData": {
                "incompleteHomeQuoteData": {
                    "zipcode": "11216"
                }
            }
        }
    """.trimIndent()

    @Test
    fun createIncompleteQuote() {
        val request = post("/_/v1/quote/create")
                .content(createQuoteRequestJson)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)

        val result = mockMvc.perform(request)

        result.andExpect(status().is2xxSuccessful)
    }

    @Test
    fun getQuote() {

        val uuid: UUID = UUID.fromString("71919787-70d2-4614-bd4a-26427861991d")

        val incompleteQuote: IncompleteQuote = IncompleteQuote(
                createdAt = Instant.now(),
                quoteState = QuoteState.INCOMPLETE,
                productType = ProductType.HOME,
                incompleteQuoteData = IncompleteQuoteData.Home(
                        address = "123 Baker street",
                        numberOfRooms = 3,
                        zipCode = "11216",
                        floor = 1
                ),
                lineOfBusiness = LineOfBusiness.RENT,
                quoteInitiatedFrom = QuoteInitiatedFrom.APP,
                birthDate = LocalDate.parse("1990-05-05"),
                livingSpace = 50,
                houseHoldSize = 3,
                isStudent = false
                )

        Mockito.`when`(quoteService.findIncompleteQuoteById(uuid))
                .thenReturn(Optional.of(incompleteQuote))

        mockMvc
                .perform(
                        get("/_/v1/quote/71919787-70d2-4614-bd4a-26427861991d"))
                .andExpect(status().is2xxSuccessful)
                .andExpect(jsonPath("quoteState").value("INCOMPLETE"))
                .andExpect(jsonPath("productType").value("HOME"))
                .andExpect(jsonPath("incompleteQuoteData.numberOfRooms").value(3));
    }

    @Test
    fun createCompleteQuote() {

        val uuid: UUID = UUID.fromString("71919787-70d2-4614-bd4a-26427861991d")

        val incompleteQuote: IncompleteQuote = IncompleteQuote(
                createdAt = Instant.now(),
                quoteState = QuoteState.INCOMPLETE,
                productType = ProductType.HOME,
                incompleteQuoteData = IncompleteQuoteData.Home(
                        address = "123 Baker street",
                        numberOfRooms = 3,
                        zipCode = "11216",
                        floor = 1
                ),
                lineOfBusiness = LineOfBusiness.RENT,
                quoteInitiatedFrom = QuoteInitiatedFrom.APP,
                birthDate = LocalDate.parse("1990-05-05"),
                livingSpace = 50,
                houseHoldSize = 3,
                isStudent = false
        )

        Mockito.`when`(quoteService.findIncompleteQuoteById(uuid))
                .thenReturn(Optional.of(incompleteQuote))

        mockMvc
                .perform(
                        post("/_/v1/quote/71919787-70d2-4614-bd4a-26427861991d/completeQuote"))
                .andExpect(status().is2xxSuccessful)
    }

    @Test
    fun shouldNotCompleteQuoteIfDataIsIncomplete() {
//        TODO
    }

}