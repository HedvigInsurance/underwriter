package com.hedvig.underwriter.utils

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import com.hedvig.underwriter.util.getAddressFromStreet
import com.hedvig.underwriter.util.isValidatedWord
import org.junit.Test

class StreetToAddressUtilTest {

    @Test
    fun `street number is mix of number and letter works`() {
        val address = getAddressFromStreet("Pile Alle 19D, 3. tv. 2000 Frederiksberg", "2000")
        assertThat(address).isNotNull()
        assertThat(address!!.street).isEqualTo("Pile Alle 19D")
        assertThat(address.apartment).isEqualTo("tv")
        assertThat(address.floor).isEqualTo("3")
        assertThat(address.city).isEqualTo("Frederiksberg")
    }

    @Test
    fun `apartment number of 4 digits works`() {
        val address = getAddressFromStreet("Fynsgade 10, st. 0001. 6700 Esbjerg", "6700")
        assertThat(address).isNotNull()
        assertThat(address!!.street).isEqualTo("Fynsgade 10")
        assertThat(address.apartment).isEqualTo("0001")
        assertThat(address.floor).isEqualTo("st")
        assertThat(address.city).isEqualTo("Esbjerg")
    }

    @Test
    fun `works with zipCode and one-word city`() {
        val address = getAddressFromStreet("Hørsholm Park 16, 3. mf. 2970 Hørsholm", "2970")
        assertThat(address).isNotNull()
        assertThat(address!!.street).isEqualTo("Hørsholm Park 16")
        assertThat(address.apartment).isEqualTo("mf")
        assertThat(address.floor).isEqualTo("3")
        assertThat(address.city).isEqualTo("Hørsholm")
    }

    @Test
    fun `works with zipCode and two-word city`() {
        val address = getAddressFromStreet("Tinghaven 4, 3, tr. 9310 Store Heddinge", "9310")
        assertThat(address).isNotNull()
        assertThat(address!!.street).isEqualTo("Tinghaven 4")
        assertThat(address.apartment).isEqualTo("tr")
        assertThat(address.floor).isEqualTo("3")
        assertThat(address.city).isEqualTo("Store Heddinge")
    }

    @Test
    fun `with zipcode city and direction works`() {
        val address = getAddressFromStreet("Jesper Brochmands Gade 6, 3. th. 2200 København N", "2200")
        assertThat(address).isNotNull()
        assertThat(address!!.street).isEqualTo("Jesper Brochmands Gade 6")
        assertThat(address.apartment).isEqualTo("th")
        assertThat(address.floor).isEqualTo("3")
        assertThat(address.city).isEqualTo("København N")
    }

    @Test
    fun `address ending in zipcode works`() {
        val address = getAddressFromStreet("Jesper Brochmands Gade 6, 3. th. 2200", "2200")
        assertThat(address).isNotNull()
        assertThat(address!!.street).isEqualTo("Jesper Brochmands Gade 6")
        assertThat(address.apartment).isEqualTo("th")
        assertThat(address.floor).isEqualTo("3")
        assertThat(address.city).isNull()
    }

    @Test
    fun `apartment number is tv works`() {
        val address = getAddressFromStreet("Søvang Alle 27, 1. tv. 2770 Kastrup", "2770")
        assertThat(address).isNotNull()
        assertThat(address!!.street).isEqualTo("Søvang Alle 27")
        assertThat(address.apartment).isEqualTo("tv")
        assertThat(address.floor).isEqualTo("1")
        assertThat(address.city).isEqualTo("Kastrup")
    }

    @Test
    fun `without zipcode and city works`() {
        val address = getAddressFromStreet("Søvang Alle 27, 1. tv.", "2770")
        assertThat(address).isNotNull()
        assertThat(address!!.street).isEqualTo("Søvang Alle 27")
        assertThat(address.apartment).isEqualTo("tv")
        assertThat(address.floor).isEqualTo("1")
        assertThat(address.city).isNull()
    }

    @Test
    fun `weird format and characters are removed from street`() {
        val address = getAddressFromStreet("Søvang  Alle  27,- 1. tv.", "2770")
        assertThat(address).isNotNull()
        assertThat(address!!.street).isEqualTo("Søvang Alle 27")
        assertThat(address.apartment).isEqualTo("tv")
        assertThat(address.floor).isEqualTo("1")
        assertThat(address.city).isNull()
    }

    @Test
    fun `returns null if street is empty`() {
        val address = getAddressFromStreet("", "2770")
        assertThat(address).isNull()
    }

    @Test
    fun `will not work if only has floor or apartment number as won't know which is which`() {
        val address = getAddressFromStreet("Sundgade 1A, 3. 6400 Sønderborg", "6400")
        assertThat(address).isNull()
    }

    //    potential improvement
    @Test
    fun `will not work if not given apartment and floor but given zipcode and city`() {
        val address = getAddressFromStreet("Tinghaven 4 9310 Vodskov", "9310")
        assertThat(address).isNull()
    }

    @Test
    fun `is validated word works if there is only one word`() {
        val isWord = isValidatedWord("city")
        assertThat(isWord).isEqualTo(true)
    }

    @Test
    fun `is validated word returns true if there are capitals in the word`() {
        val isWord = isValidatedWord("CITY")
        assertThat(isWord).isEqualTo(true)

        val isWord2 = isValidatedWord("citY")
        assertThat(isWord2).isEqualTo(true)

        val isWord3 = isValidatedWord("City")
        assertThat(isWord3).isEqualTo(true)
    }

    @Test
    fun `is validated word returns false if word is empty string`() {
        val isWord = isValidatedWord("")
        assertThat(isWord).isEqualTo(false)
    }

    @Test
    fun `is validated word returns false if there are numbers or characters in the word`() {
        val isWord = isValidatedWord("city3")
        assertThat(isWord).isEqualTo(false)

        val isWord2 = isValidatedWord("city-")
        assertThat(isWord2).isEqualTo(false)
    }
}
