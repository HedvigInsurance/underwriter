package com.hedvig.underwriter.service.guidelines

import com.hedvig.underwriter.model.NorwegianTravelData
import com.hedvig.underwriter.service.guidelines.BreachedGuidelinesCodes.NEGATIVE_NUMBER_OF_CO_INSURED
import com.hedvig.underwriter.service.guidelines.BreachedGuidelinesCodes.TOO_HIGH_NUMBER_OF_CO_INSURED
import com.hedvig.underwriter.service.guidelines.BreachedGuidelinesCodes.YOUTH_OVERAGE
import java.time.LocalDate
import java.time.temporal.ChronoUnit

object NorwegianTravelGuidelines {
    val setOfRules = setOf(
        NorwegianTravelCoInsuredCantBeNegative,
        NorwegianTravelCoInsuredNotMoreThan5,
        NorwegianYouthTravelAgeNotMoreThan30Years,
        NorwegianYouthTravelCoInsuredNotMoreThan0
    )
}

object NorwegianTravelCoInsuredCantBeNegative : BaseGuideline<NorwegianTravelData> {
    override val breachedGuideline = BreachedGuideline(
        "coInsured cant be negative",
        NEGATIVE_NUMBER_OF_CO_INSURED
    )

    override val validate = { data: NorwegianTravelData -> data.coInsured < 0 }
}

object NorwegianYouthTravelAgeNotMoreThan30Years : BaseGuideline<NorwegianTravelData> {
    override val breachedGuideline = BreachedGuideline(
        "breaches underwriting guidelines member must be 30 years old or younger",
        YOUTH_OVERAGE
    )

    override val validate =
        { data: NorwegianTravelData ->
            (data.isYouth) &&
                data.birthDate.until(LocalDate.now(), ChronoUnit.YEARS)> 30
        }
}

object NorwegianYouthTravelCoInsuredNotMoreThan0 : BaseGuideline<NorwegianTravelData> {
    override val breachedGuideline = BreachedGuideline(
        "coInsured size must be less than or equal to 0",
        NEGATIVE_NUMBER_OF_CO_INSURED
    )

    override val validate =
        { data: NorwegianTravelData ->
            (data.isYouth) &&
                data.coInsured > 0
        }
}

object NorwegianTravelCoInsuredNotMoreThan5 : BaseGuideline<NorwegianTravelData> {
    override val breachedGuideline = BreachedGuideline(
        "coInsured size must be less than or equal to 5",
        TOO_HIGH_NUMBER_OF_CO_INSURED
    )

    override val validate = { data: NorwegianTravelData -> data.coInsured > 5 }
}
