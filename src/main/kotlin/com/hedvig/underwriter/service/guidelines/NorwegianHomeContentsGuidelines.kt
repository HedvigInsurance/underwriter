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
    override val guidelineBreached = GuidelineBreached("coInsured cant be negative", "NEGATIVE_NUMBER_OF_CO_INSURED")

    override val validate = { data: NorwegianHomeContentsData -> data.coInsured < 0 }
}

object NorwegianHomeContentLivingSpaceAtLeast1Sqm : BaseGuideline<NorwegianHomeContentsData> {
    override val guidelineBreached = GuidelineBreached("living space must be at least 1 sqm", "LIVING_SPACE_LESS_THAN_1")

    override val validate = { data: NorwegianHomeContentsData -> data.livingSpace < 1 }
}

object NorwegianHomeContentscoInsuredNotMoreThan5 : BaseGuideline<NorwegianHomeContentsData> {
    override val guidelineBreached = GuidelineBreached("coInsured size must be less than or equal to 5", "NUMBER_OF_CO_INSURED_MORE_THAN_5")

    override val validate = { data: NorwegianHomeContentsData -> data.coInsured > 5 }
}

object NorwegianHomeContentsLivingSpaceNotMoreThan250Sqm : BaseGuideline<NorwegianHomeContentsData> {
    override val guidelineBreached = GuidelineBreached(
        "living space must be less than or equal to 250 sqm",
        "LIVING_SPACE_MORE_THAN_250"
    )

    override val validate = { data: NorwegianHomeContentsData -> data.livingSpace > 250 }
}

object NorwegianYouthHomeContentsCoInsuredNotMoreThan2 : BaseGuideline<NorwegianHomeContentsData> {
    override val guidelineBreached = GuidelineBreached(
        "coInsured size must be less than or equal to 2",
        "YOUTH_NUMBER_OF_CO_INSURED_MORE_THAN_2"
    )

    override val validate =
        { data: NorwegianHomeContentsData ->
            (data.isYouth) &&
                data.coInsured > 2
        }
}

object NorwegianYouthHomeContentsLivingSpaceNotMoreThan50Sqm : BaseGuideline<NorwegianHomeContentsData> {
    override val guidelineBreached = GuidelineBreached(
        "breaches underwriting guideline living space must be less than or equal to 50sqm",
        "YOUTH_LIVING_SPACE_MORE_THAN_50"
    )

    override val validate =
        { data: NorwegianHomeContentsData ->
            (data.isYouth) &&
                data.livingSpace > 50
        }
}

object NorwegianYouthHomeContentsAgeNotMoreThan30Years : BaseGuideline<NorwegianHomeContentsData> {
    override val guidelineBreached = GuidelineBreached(
        "breaches underwriting guidelines member must be 30 years old or younger",
        "YOUTH_AGE_MORE_THAN_30"
    )

    override val validate =
        { data: NorwegianHomeContentsData ->
            (data.isYouth) &&
                data.birthDate.until(LocalDate.now(), ChronoUnit.YEARS) > 30
        }
}
