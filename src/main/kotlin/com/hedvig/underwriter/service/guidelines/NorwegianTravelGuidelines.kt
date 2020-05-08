package com.hedvig.underwriter.service.guidelines

import com.hedvig.underwriter.model.NorwegianTravelData
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
    override val errorMessage: String = "coInsured cant be negative"

    override val validate = { data: NorwegianTravelData -> data.coInsured < 0 }
}

object NorwegianYouthTravelAgeNotMoreThan30Years : BaseGuideline<NorwegianTravelData> {
    override val errorMessage: String =
        "breaches underwriting guidelines member must be 30 years old or younger"

    override val validate =
        { data: NorwegianTravelData ->
            (data.isYouth) &&
                data.birthDate.until(
                    LocalDate.now(),
                    ChronoUnit.YEARS
                ) > 30
        }
}

object NorwegianYouthTravelCoInsuredNotMoreThan0 : BaseGuideline<NorwegianTravelData> {
    override val errorMessage: String = "coInsured size must be less than or equal to 0"

    override val validate =
        { data: NorwegianTravelData ->
            (data.isYouth) &&
                data.coInsured > 0
        }
}

object NorwegianTravelCoInsuredNotMoreThan5 : BaseGuideline<NorwegianTravelData> {
    override val errorMessage: String = "coInsured size must be less than or equal to 5"

    override val validate = { data: NorwegianTravelData -> data.coInsured > 5 }
}
