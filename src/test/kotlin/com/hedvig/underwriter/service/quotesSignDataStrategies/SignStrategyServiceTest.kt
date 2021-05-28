package com.hedvig.underwriter.service.quotesSignDataStrategies

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import com.hedvig.underwriter.model.QuoteInitiatedFrom
import com.hedvig.underwriter.service.model.StartSignErrors
import com.hedvig.underwriter.service.model.StartSignResponse
import com.hedvig.underwriter.testhelp.databuilder.DanishAccidentDataBuilder
import com.hedvig.underwriter.testhelp.databuilder.DanishHomeContentsDataBuilder
import com.hedvig.underwriter.testhelp.databuilder.DanishTravelDataBuilder
import com.hedvig.underwriter.testhelp.databuilder.NorwegianHomeContentDataBuilder
import com.hedvig.underwriter.testhelp.databuilder.NorwegianTravelDataBuilder
import com.hedvig.underwriter.testhelp.databuilder.SwedishHouseDataBuilder
import com.hedvig.underwriter.testhelp.databuilder.quote
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test

class SignStrategyServiceTest {

    private val signData = SignData("mid", null, null, null)

    private val swedishBankIdSignStrategy: SwedishBankIdSignStrategy = mockk(relaxed = true)
    private val simpleSignStrategy: SimpleSignStrategy = mockk(relaxed = true)
    private val selfChangeCommittingStrategy: SelfChangeCommittingStrategy = mockk(relaxed = true)

    private val cut = SignStrategyService(
        swedishBankIdSignStrategy,
        simpleSignStrategy,
        selfChangeCommittingStrategy
    )

    @Test
    fun `start sign with no quotes returns no quotes`() {
        val result = cut.startSign(emptyList(), signData)

        assertThat(result).isInstanceOf(StartSignResponse.FailedToStartSign::class)
        require(result is StartSignResponse.FailedToStartSign)
        assertThat(result.errorMessage).isEqualTo(StartSignErrors.noQuotes.errorMessage)
        assertThat(result.errorCode).isEqualTo(StartSignErrors.noQuotes.errorCode)
    }

    @Test
    fun `start sign swedish and norwegian quote returns can not be bundled`() {
        val result = cut.startSign(
            listOf(
                quote {},
                quote {
                    data = NorwegianTravelDataBuilder()
                }
            ),
            signData
        )

        assertThat(result).isInstanceOf(StartSignResponse.FailedToStartSign::class)
        require(result is StartSignResponse.FailedToStartSign)
        assertThat(result.errorMessage).isEqualTo(StartSignErrors.quotesCanNotBeBundled.errorMessage)
        assertThat(result.errorCode).isEqualTo(StartSignErrors.quotesCanNotBeBundled.errorCode)
    }

    @Test
    fun `start sign norwegian and danish quote returns can not be bundled`() {
        val result = cut.startSign(
            listOf(
                quote {
                    data = NorwegianTravelDataBuilder()
                },
                quote {
                    data = DanishHomeContentsDataBuilder()
                }
            ),
            signData
        )

        assertThat(result).isInstanceOf(StartSignResponse.FailedToStartSign::class)
        require(result is StartSignResponse.FailedToStartSign)
        assertThat(result.errorMessage).isEqualTo(StartSignErrors.quotesCanNotBeBundled.errorMessage)
        assertThat(result.errorCode).isEqualTo(StartSignErrors.quotesCanNotBeBundled.errorCode)
    }

    @Test
    fun `start sign with two swedish quotes returns can not be bundled`() {
        val result = cut.startSign(
            listOf(
                quote {},
                quote {
                    data = SwedishHouseDataBuilder()
                }
            ),
            signData
        )

        assertThat(result).isInstanceOf(StartSignResponse.FailedToStartSign::class)
        require(result is StartSignResponse.FailedToStartSign)
        assertThat(result.errorMessage).isEqualTo(StartSignErrors.quotesCanNotBeBundled.errorMessage)
        assertThat(result.errorCode).isEqualTo(StartSignErrors.quotesCanNotBeBundled.errorCode)
    }

    @Test
    fun `start sign with two norwegian home content quotes returns can not be bundled`() {
        val result = cut.startSign(
            listOf(
                quote {
                    data = NorwegianHomeContentDataBuilder()
                },
                quote {
                    data = NorwegianHomeContentDataBuilder()
                }
            ),
            signData
        )

        assertThat(result).isInstanceOf(StartSignResponse.FailedToStartSign::class)
        require(result is StartSignResponse.FailedToStartSign)
        assertThat(result.errorMessage).isEqualTo(StartSignErrors.quotesCanNotBeBundled.errorMessage)
        assertThat(result.errorCode).isEqualTo(StartSignErrors.quotesCanNotBeBundled.errorCode)
    }

    @Test
    fun `start sign with two norwegian travel quotes returns can not be bundled`() {
        val result = cut.startSign(
            listOf(
                quote {
                    data = NorwegianTravelDataBuilder()
                },
                quote {
                    data = NorwegianTravelDataBuilder()
                }
            ),
            signData
        )

        assertThat(result).isInstanceOf(StartSignResponse.FailedToStartSign::class)
        require(result is StartSignResponse.FailedToStartSign)
        assertThat(result.errorMessage).isEqualTo(StartSignErrors.quotesCanNotBeBundled.errorMessage)
        assertThat(result.errorCode).isEqualTo(StartSignErrors.quotesCanNotBeBundled.errorCode)
    }

    @Test
    fun `start sign with one danish accident quotes returns can not be bundled`() {
        val result = cut.startSign(
            listOf(
                quote {
                    data = DanishAccidentDataBuilder()
                }
            ),
            signData
        )

        assertThat(result).isInstanceOf(StartSignResponse.FailedToStartSign::class)
        require(result is StartSignResponse.FailedToStartSign)
        assertThat(result.errorMessage).isEqualTo(StartSignErrors.singleQuoteCanNotBeSignedAlone.errorMessage)
        assertThat(result.errorCode).isEqualTo(StartSignErrors.singleQuoteCanNotBeSignedAlone.errorCode)
    }

    @Test
    fun `start sign with one danish travel quotes returns can not be bundled`() {
        val result = cut.startSign(
            listOf(
                quote {
                    data = DanishTravelDataBuilder()
                }
            ),
            signData
        )

        assertThat(result).isInstanceOf(StartSignResponse.FailedToStartSign::class)
        require(result is StartSignResponse.FailedToStartSign)
        assertThat(result.errorMessage).isEqualTo(StartSignErrors.singleQuoteCanNotBeSignedAlone.errorMessage)
        assertThat(result.errorCode).isEqualTo(StartSignErrors.singleQuoteCanNotBeSignedAlone.errorCode)
    }

    @Test
    fun `start sign of one swedish quote calls swedishBankIdSignStrategy startSign`() {
        cut.startSign(
            listOf(
                quote {}
            ),
            signData
        )

        verify(exactly = 1) { swedishBankIdSignStrategy.startSign(any(), any()) }
    }

    @Test
    fun `start sign of norwegian quotes calls simpleSignStrategy startSign`() {
        cut.startSign(
            listOf(
                quote {
                    data = NorwegianHomeContentDataBuilder()
                },
                quote {
                    data = NorwegianTravelDataBuilder()
                }
            ),
            signData
        )

        verify(exactly = 1) { simpleSignStrategy.startSign(any(), any()) }
    }

    @Test
    fun `start sign of danish quotes calls simpleSignStrategy startSign`() {
        cut.startSign(
            listOf(
                quote {
                    data = DanishAccidentDataBuilder()
                },
                quote {
                    data = DanishTravelDataBuilder()
                },
                quote {
                    data = DanishHomeContentsDataBuilder()
                }
            ),
            signData
        )

        verify(exactly = 1) { simpleSignStrategy.startSign(any(), any()) }
    }

    @Test
    fun `start sign of SELF_CHANGE selfChangeCommittingStrategy startSign`() {
        cut.startSign(
            listOf(
                quote {
                    initiatedFrom = QuoteInitiatedFrom.SELF_CHANGE
                }
            ),
            signData
        )

        verify(exactly = 1) { selfChangeCommittingStrategy.startSign(any(), any()) }
    }
}
