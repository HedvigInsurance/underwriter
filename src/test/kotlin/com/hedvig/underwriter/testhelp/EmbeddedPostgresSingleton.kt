package com.hedvig.underwriter.testhelp

import com.opentable.db.postgres.embedded.EmbeddedPostgres
import org.apache.commons.lang.SystemUtils

object EmbeddedPostgresSingleton {
    private var _embeddedPostgres: EmbeddedPostgres? = null
    val embeddedPostgres: EmbeddedPostgres
        get() {
            if (_embeddedPostgres == null) {
                val builder = EmbeddedPostgres.builder()
                if (SystemUtils.IS_OS_MAC) {
                    // https://github.com/zonkyio/embedded-postgres/issues/11
                    builder.setLocaleConfig("locale", "en_US")
                }
                _embeddedPostgres = builder.start()
            }

            return _embeddedPostgres!!
        }
}
