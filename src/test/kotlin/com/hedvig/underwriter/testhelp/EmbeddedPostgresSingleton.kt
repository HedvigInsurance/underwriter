package com.hedvig.underwriter.testhelp

import com.opentable.db.postgres.embedded.EmbeddedPostgres

object EmbeddedPostgresSingleton {
    private var _embeddedPostgres: EmbeddedPostgres? = null
    val embeddedPostgres: EmbeddedPostgres
        get() {
            if (_embeddedPostgres == null) {
                _embeddedPostgres = EmbeddedPostgres.builder()
                    // https://github.com/zonkyio/embedded-postgres/issues/11
                    .setLocaleConfig("locale", "en_US")
                    .start()
            }

            return _embeddedPostgres!!
        }
}
