package com.hedvig.underwriter.graphql.type

data class CurrentInsurer(
    val id: String,
    val displayName: String,
    val switchable: Boolean
) {
    companion object {
        private val swedishInsurerMap = mapOf(
            "if" to Pair("If", false),
            "Folksam" to Pair("Folksam", true),
            "Trygg-Hansa" to Pair("Trygg-Hansa", true),
            "Länsförsäkringar" to Pair("Länsförsäkringar", false),
            "Moderna" to Pair("Moderna", true),
            "Gjensidige" to Pair("Gjensidige", false),
            "Vardia" to Pair("Vardia", false),
            "Tre Kronor" to Pair("Tre Kronor", true),
            "ICA" to Pair("Ica", true),
            "Dina Försäkringar" to Pair("Dina Försäkringar", false),
            "other" to Pair("Other", false)
        )
        private val norwegianInsurerMap = mapOf(
            "If NO" to Pair("If", true),
            "Fremtind" to Pair("Fremtind", true),
            "Gjensidige NO" to Pair("Gjensidige", true),
            "Tryg" to Pair("Tryg", true),
            "Eika" to Pair("Eika", true),
            "Frende" to Pair("Frende", true),
            "Storebrand" to Pair("Storebrand", true),
            "Codan" to Pair("Codan", true)
        )

        private val allInsurers = swedishInsurerMap + norwegianInsurerMap

        fun create(id: String) = allInsurers[id]?.let {
            CurrentInsurer(id, it.first, it.second)
        } ?: throw IllegalArgumentException("Unknown id($id) when creating CurrentInsurer")
    }
}
