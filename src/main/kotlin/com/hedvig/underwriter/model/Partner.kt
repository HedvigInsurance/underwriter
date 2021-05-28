package com.hedvig.underwriter.model


enum class Partner(val partnerName: String, val campaignCode: String? = null) {
    HEDVIG("HEDVIG"),
    INSPLANET("INSPLANET", "f89051cb43"),
    COMPRICER("COMPRICER", "8a2fcb2a11"),
    INSURLEY("INSURLEY", "eib7soo9va"),
    KEYSOLUTIONS("KEYSOLUTIONS", "l8wyjvFfx5"),
    AVY("AVY"),
    SPIFF("SPIFF"),
    TJENSTETORGET("TJENSTETORGET"),
    FORSIKRINGSPORTALEN("FORSIKRINGSPORTALEN"),
    SAMLINO("SAMLINO"),
    FINDFORSIKRING("FINDFORSIKRING"),
    FORSIKRINGSGUIDEN("FORSIKRINGSGUIDEN"),
    KEY_HOLE("KEY_HOLE"),
    COPENHAGEN_SALES("COPENHAGEN_SALES"),
    CHARLIE("CHARLIE"),
    FLYTTSMART("FLYTTSMART"),
    PAMIND("PAMIND"),
    MECENAT("MECENAT"),
    HAPPENS("HAPPENS"),
    HEMAVI("HEMAVI"),
    SAMBLA("SAMBLA"),
    PEACH("PEACH"),
    MMG("MMG"),
}

class PartnerConverter : Converter<Partner,String>
