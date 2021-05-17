package com.hedvig.underwriter.service

interface GdprService {
    fun clean(requestedDryRun: Boolean?, requestedDays: Long?)
}
