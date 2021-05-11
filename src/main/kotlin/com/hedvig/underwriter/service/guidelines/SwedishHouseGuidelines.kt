package com.hedvig.underwriter.service.guidelines

import com.hedvig.underwriter.model.SwedishHouseData
import com.hedvig.underwriter.service.guidelines.BreachedGuidelinesCodes.OK
import com.hedvig.underwriter.service.guidelines.BreachedGuidelinesCodes.TOO_BIG_EXTRA_BUILDING_SIZE
import com.hedvig.underwriter.service.guidelines.BreachedGuidelinesCodes.TOO_EARLY_YEAR_OF_CONSTRUCTION
import com.hedvig.underwriter.service.guidelines.BreachedGuidelinesCodes.TOO_HIGH_NUMBER_OF_HOUSE_HOLD_SIZE
import com.hedvig.underwriter.service.guidelines.BreachedGuidelinesCodes.TOO_MANY_BATHROOMS
import com.hedvig.underwriter.service.guidelines.BreachedGuidelinesCodes.TOO_MANY_EXTRA_BUILDINGS
import com.hedvig.underwriter.service.guidelines.BreachedGuidelinesCodes.TOO_MUCH_LIVING_SPACE
import com.hedvig.underwriter.service.guidelines.BreachedGuidelinesCodes.TOO_SMALL_EXTRA_BUILDING_SIZE
import com.hedvig.underwriter.service.guidelines.BreachedGuidelinesCodes.TOO_SMALL_LIVING_SPACE
import com.hedvig.underwriter.service.guidelines.BreachedGuidelinesCodes.TOO_SMALL_NUMBER_OF_HOUSE_HOLD_SIZE

typealias SwedishHouseGuideline = BaseGuideline<SwedishHouseData>

object SwedishHouseGuidelines {
    val setOfRules = setOf(
        SwedishHouseHouseholdSizeGuideline,
        SwedishHouseLivingSpaceGuideline,
        SwedishHouseYearOfConstruction,
        SwedishHouseNumberOfBathrooms,
        SwedishHouseNumberOfExtraBuildingsWithAreaOverSixSqm,
        SwedishHouseExtraBuildingsSizeNotOverThan75Sqm,
        SwedishHouseExtraBuildingsSizeAtLeast1Sqm
    )
}

object SwedishHouseHouseholdSizeGuideline : SwedishHouseGuideline {

    override fun validate(data: SwedishHouseData): BreachedGuidelineCode {
        if (data.householdSize!! < 1) {
            return TOO_SMALL_NUMBER_OF_HOUSE_HOLD_SIZE
        }

        if (data.householdSize!! > 6) {
            return TOO_HIGH_NUMBER_OF_HOUSE_HOLD_SIZE
        }

        return OK
    }
}

object SwedishHouseLivingSpaceGuideline : SwedishHouseGuideline {

    override fun validate(data: SwedishHouseData): BreachedGuidelineCode {
        if (data.livingSpace!! < 1) {
            return TOO_SMALL_LIVING_SPACE
        }

        if (data.livingSpace!! + (data.ancillaryArea ?: 0) > 300) {
            return TOO_MUCH_LIVING_SPACE
        }

        return OK
    }
}

object SwedishHouseYearOfConstruction : SwedishHouseGuideline {

    override fun validate(data: SwedishHouseData): BreachedGuidelineCode {
        if (data.yearOfConstruction!! < 1925) {
            return TOO_EARLY_YEAR_OF_CONSTRUCTION
        }

        return OK
    }
}

object SwedishHouseNumberOfBathrooms : SwedishHouseGuideline {

    override fun validate(data: SwedishHouseData): BreachedGuidelineCode {
        if (data.numberOfBathrooms!! > 2) {
            return TOO_MANY_BATHROOMS
        }

        return OK
    }
}

object SwedishHouseNumberOfExtraBuildingsWithAreaOverSixSqm : SwedishHouseGuideline {

    override fun validate(data: SwedishHouseData): BreachedGuidelineCode {
        if (data.extraBuildings!!.filter { building -> building.area > 6 }.size > 4) {
            return TOO_MANY_EXTRA_BUILDINGS
        }

        return OK
    }
}

object SwedishHouseExtraBuildingsSizeNotOverThan75Sqm : SwedishHouseGuideline {

    override fun validate(data: SwedishHouseData): BreachedGuidelineCode {
        if (data.extraBuildings!!.any { building -> building.area > 75 }) {
            return TOO_BIG_EXTRA_BUILDING_SIZE
        }

        return OK
    }
}

object SwedishHouseExtraBuildingsSizeAtLeast1Sqm : SwedishHouseGuideline {

    override fun validate(data: SwedishHouseData): BreachedGuidelineCode {
        if (data.extraBuildings!!.any { building -> building.area < 1 }) {
            return TOO_SMALL_EXTRA_BUILDING_SIZE
        }

        return OK
    }
}
