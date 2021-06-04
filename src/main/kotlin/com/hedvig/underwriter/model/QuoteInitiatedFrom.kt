package com.hedvig.underwriter.model

enum class QuoteInitiatedFrom {
    RAPIO,
    WEBONBOARDING,
    APP,
    IOS,
    ANDROID,
    HOPE,

    /**
     * The quote was created as part of the "self change" flow (moving flow) in the apps.
     */
    SELF_CHANGE
}
