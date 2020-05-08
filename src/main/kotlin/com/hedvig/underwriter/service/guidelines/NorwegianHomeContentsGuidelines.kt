package com.hedvig.underwriter.service.guidelines

import com.hedvig.underwriter.model.NorwegianHomeContentsData
import java.time.LocalDate
import java.time.temporal.ChronoUnit

object NorwegianHomeContentsGuidelines {
    val setOfRules = setOf(
        NorwegianHomeContentcoInsuredCantBeNegative,
        NorwegianHomeContentLivingSpaceAtLeast1Sqm,
        NorwegianHomeContentscoInsuredNotMoreThan5,
        NorwegianHomeContentsLivingSpaceNotMoreThan250Sqm,
        NorwegianYouthHomeContentsLivingSpaceNotMoreThan50Sqm,
        NorwegianYouthHomeContentsAgeNotMoreThan30Years,
        NorwegianYouthHomeContentsCoInsuredNotMoreThan2
    )
}

object NorwegianHomeContentcoInsuredCantBeNegative : BaseGuideline<NorwegianHomeContentsData> {
    override val errorMessage: String = "coInsured cant be negative"

    override val validate = { data: NorwegianHomeContentsData -> data.coInsured < 0 }
}

object NorwegianHomeContentLivingSpaceAtLeast1Sqm : BaseGuideline<NorwegianHomeContentsData> {
    override val errorMessage: String = "living space must be at least 1 sqm"

    override val validate = { data: NorwegianHomeContentsData -> data.livingSpace < 1 }
}

object NorwegianHomeContentscoInsuredNotMoreThan5 : BaseGuideline<NorwegianHomeContentsData> {
    override val errorMessage: String = "coInsured size must be less than or equal to 5"

    override val validate = { data: NorwegianHomeContentsData -> data.coInsured > 5 }
}

object NorwegianHomeContentsLivingSpaceNotMoreThan250Sqm : BaseGuideline<NorwegianHomeContentsData> {
    override val errorMessage: String =
        "living space must be less than or equal to 250 sqm"

    override val validate = { data: NorwegianHomeContentsData -> data.livingSpace > 250 }
}

object NorwegianYouthHomeContentsCoInsuredNotMoreThan2 : BaseGuideline<NorwegianHomeContentsData> {
    override val errorMessage: String = "coInsured size must be less than or equal to 2"

    override val validate =
        { data: NorwegianHomeContentsData ->
            (data.isYouth) &&
                data.coInsured > 2
        }
}

object NorwegianYouthHomeContentsLivingSpaceNotMoreThan50Sqm : BaseGuideline<NorwegianHomeContentsData> {
    override val errorMessage: String =
        "breaches underwriting guideline living space must be less than or equal to 50sqm"

    override val validate =
        { data: NorwegianHomeContentsData ->
            (data.isYouth) &&
                data.livingSpace > 50
        }
}

object NorwegianYouthHomeContentsAgeNotMoreThan30Years : BaseGuideline<NorwegianHomeContentsData> {
    override val errorMessage: String =
        "breaches underwriting guidelines member must be 30 years old or younger"

    override val validate =
        { data: NorwegianHomeContentsData ->
            (data.isYouth) &&
                data.birthDate.until(
                    LocalDate.now(),
                    ChronoUnit.YEARS
                ) > 30
        }
}
