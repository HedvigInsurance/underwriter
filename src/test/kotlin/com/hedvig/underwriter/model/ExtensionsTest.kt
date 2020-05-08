package com.hedvig.underwriter.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class ExtensionsTest {

    @ParameterizedTest(name = "#{index} - Run test with args={0}")
    @ValueSource(
        strings = [
            "21126114165",
            "13103110933",
            "30112913943",
            "28026400734",
            "19059104716",
            "15012910972",
            "10129329373",
            "20078945932",
            "18073131800",
            "18110158080",
            "19048711890",
            "24057408215",
            "23077421475",
            "20129443600",
            "16093028221",
            "15126533427",
            "06046517928",
            "23115994336",
            "18060970021",
            "19041452504"]
    )
    fun validate_Valid_NorwegianSsn_ShouldReturnTrue(arg: String) {
        assertThat(arg.isValidNorwegianSsn()).isTrue()
    }

    @ParameterizedTest(name = "#{index} - Run test with args={0}")
    @ValueSource(
        strings = [
            "21126114164",
            "131031109",
            "301129139AA",
            "28026400732",
            "19059104711",
            "150129109721111",
            "1012932937",
            "8902179798"
        ]
    )
    fun validate_Invalid_NorwegianSsn_ShouldReturnFalse(arg: String) {
        assertThat(arg.isValidNorwegianSsn()).isFalse()
    }
}
