package com.hedvig.underwriter.service.guidelines

import com.hedvig.underwriter.model.ApartmentProductSubType
import com.hedvig.underwriter.model.SwedishApartmentData
import com.hedvig.underwriter.model.birthDateFromSwedishSsn
import java.time.LocalDate
import java.time.temporal.ChronoUnit

object SwedishApartmentGuidelines {
    val setOfRules = setOf(
        SwedishApartmentHouseHoldSizeAtLeast1,
        SwedishApartmentLivingSpaceAtLeast1Sqm,
        SwedishApartmentHouseHoldSizeNotMoreThan6,
        SwedishApartmentLivingSpaceNotMoreThan250Sqm,
        SwedishStudentApartmentHouseholdSizeNotMoreThan2,
        SwedishStudentApartmentLivingSpaceNotMoreThan50Sqm,
        SwedishStudentApartmentAgeNotMoreThan30Years
    )
}

object SwedishApartmentHouseHoldSizeAtLeast1 : BaseGuideline<SwedishApartmentData> {
    override val guidelineBreached = GuidelineBreached(
        "breaches underwriting guideline household size, must be at least 1",
        "HOUSE_HOLD_SIZE_LESS_THAN_1"
    )

    override val validate = { data: SwedishApartmentData -> data.householdSize!! < 1 }
}

object SwedishApartmentLivingSpaceAtLeast1Sqm : BaseGuideline<SwedishApartmentData> {
    override val guidelineBreached = GuidelineBreached(
        "breaches underwriting guideline living space, must be at least 1 sqm",
        "LIVING_SPACE_LESS_THAN_1"
    )

    override val validate = { data: SwedishApartmentData -> data.livingSpace!! < 1 }
}

object SwedishApartmentHouseHoldSizeNotMoreThan6 : BaseGuideline<SwedishApartmentData> {
    override val guidelineBreached = GuidelineBreached(
        "breaches underwriting guideline household size must be less than or equal to 6",
        "HOUSE_HOLD_SIZE_MORE_THAN_6"
    )

    override val validate = { data: SwedishApartmentData -> data.householdSize!! > 6 }
}

object SwedishApartmentLivingSpaceNotMoreThan250Sqm : BaseGuideline<SwedishApartmentData> {
    override val guidelineBreached = GuidelineBreached(
        "breaches underwriting guideline living space must be less than or equal to 250 sqm",
        "LIVING_SPACE_MORE_THAN_250"
    )

    override val validate = { data: SwedishApartmentData -> data.livingSpace!! > 250 }
}

object SwedishStudentApartmentHouseholdSizeNotMoreThan2 : BaseGuideline<SwedishApartmentData> {
    override val guidelineBreached = GuidelineBreached(
        "breaches underwriting guideline household size must be less than 2",
        "STUDENT_HOUSE_HOLD_SIZE_MORE_THAN_2"
    )

    override val validate =
        { data: SwedishApartmentData ->
            (data.subType == ApartmentProductSubType.STUDENT_RENT || data.subType == ApartmentProductSubType.STUDENT_BRF) &&
                data.householdSize!! > 2
        }
}

object SwedishStudentApartmentLivingSpaceNotMoreThan50Sqm : BaseGuideline<SwedishApartmentData> {
    override val guidelineBreached = GuidelineBreached(
        "breaches underwriting guideline living space must be less than or equal to 50sqm",
        "STUDENT_LIVING_SPACE_MORE_THAN_50"
    )

    override val validate =
        { data: SwedishApartmentData ->
            (data.subType == ApartmentProductSubType.STUDENT_RENT || data.subType == ApartmentProductSubType.STUDENT_BRF) &&
                data.livingSpace!! > 50
        }
}

object SwedishStudentApartmentAgeNotMoreThan30Years : BaseGuideline<SwedishApartmentData> {
    override val guidelineBreached = GuidelineBreached(
        "breaches underwriting guidelines member must be 30 years old or younger",
        "STUDENT_AGE_MORE_THAN_30"
    )

    override val validate =
        { data: SwedishApartmentData ->
            (data.subType == ApartmentProductSubType.STUDENT_RENT || data.subType == ApartmentProductSubType.STUDENT_BRF) &&
                data.ssn!!.birthDateFromSwedishSsn().until(LocalDate.now(), ChronoUnit.YEARS) > 30
        }
}
