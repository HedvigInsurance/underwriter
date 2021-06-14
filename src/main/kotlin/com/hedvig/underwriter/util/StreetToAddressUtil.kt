package com.hedvig.underwriter.util

import com.hedvig.underwriter.service.model.DanishAddressData

fun splitStreet(street: String): List<String> {
    val nonWordCharactersNotIncludingLettersWithAccents = Regex("[^éëèæäåøö \\w]")
    val splitStreet = (street as CharSequence).replace(nonWordCharactersNotIncludingLettersWithAccents, "").split(' ')
    return splitStreet.filter { it != "" }
}

fun List<String>.combineStringParts(): String =
    this.take(this.size).joinToString(postfix = " ").trim().replace(",", "")

fun isValidatedWord(addressInput: String): Boolean =
    addressInput.replace(
        Regex("[éëèæäåøö]"), "o"
    ).split(" ")
        .all {
            it.matches(Regex("[a-zA-Z]+"))
        }

fun isValidatedNumber(addressInput: String): Boolean =
    addressInput.matches(Regex("\\d+"))

fun isLikelyApartmentOrFloorNumber(maybeApartmentOrFloor: String): Boolean {
    return if (isValidatedNumber(maybeApartmentOrFloor)) true else maybeApartmentOrFloor.matches(Regex("[a-zA-Z]{2}"))
}

fun isLikelyStreetNumber(streetNumber: String): Boolean {
    return streetNumber.matches(Regex("\\d+\\w")) || isValidatedNumber(streetNumber)
}

fun getAddressFromStreet(street: String?, zipCode: String): DanishAddressData? {
    street ?: return null

    val streetParts = splitStreet(street)

    val streetIncludesZipCode = streetParts.contains(zipCode)
    val indexOfZipCode = streetParts.lastIndexOf(zipCode)

    return when {
        streetIncludesZipCode && streetParts.last() == zipCode -> {
            patternIsCorrectForAddressIncludingZipCode(streetParts = streetParts, city = null)
        }
        streetIncludesZipCode -> {
            patternIsCorrectForAddressIncludingZipCode(
                streetParts = streetParts.take(indexOfZipCode + 1),
                city = streetParts.takeLast(streetParts.size - indexOfZipCode - 1).combineStringParts()
            )
        }
        else -> patternIsCorrectForAddressWithoutZipCode(streetParts)
    }
}

// accepted formats
// street name, street number, apartment, floor, zipcode, city
// street name, street number, apartment, floor, zipcode
// street name, street number, apartment, floor

fun patternIsCorrectForAddressIncludingZipCode(streetParts: List<String>, city: String?): DanishAddressData? {
    if (streetParts.size < 5) return null

    return validateAndBuildDanishAddressData(
        apartmentNumber = streetParts[streetParts.size - 2],
        floorNumber = streetParts[streetParts.size - 3],
        streetNumber = streetParts[streetParts.size - 4],
        streetName = streetParts.take(streetParts.size - 4),
        street = streetParts.take(streetParts.size - 3).combineStringParts(),
        city = city
    )
}

fun patternIsCorrectForAddressWithoutZipCode(streetParts: List<String>): DanishAddressData? {
    if (streetParts.size < 4) return null

    return validateAndBuildDanishAddressData(
        apartmentNumber = streetParts[streetParts.size - 1],
        floorNumber = streetParts[streetParts.size - 2],
        streetNumber = streetParts[streetParts.size - 3],
        streetName = streetParts.take(streetParts.size - 3),
        street = streetParts.take(streetParts.size - 2).combineStringParts(),
        city = null
    )
}

fun validateAndBuildDanishAddressData(
    apartmentNumber: String,
    floorNumber: String,
    streetNumber: String,
    streetName: List<String>,
    street: String,
    city: String?
): DanishAddressData? {

    if (isLikelyApartmentOrFloorNumber(apartmentNumber) &&
        isLikelyApartmentOrFloorNumber(floorNumber) &&
        isLikelyStreetNumber(streetNumber) &&
        streetName.all { isValidatedWord(it) } &&
        if (city != null) isValidatedWord(city) else true
    ) {
        return DanishAddressData(
            street = street,
            apartment = apartmentNumber,
            floor = floorNumber,
            city = city
        )
    }
    return null
}
