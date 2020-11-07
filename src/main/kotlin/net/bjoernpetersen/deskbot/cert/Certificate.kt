package net.bjoernpetersen.deskbot.cert

import java.security.KeyStore
import java.security.cert.X509Certificate
import java.time.Instant
import java.util.Date

data class Certificate(val passphrase: String) {
    val keystore: KeyStore = KeyStore.getInstance("JKS")

    fun getAlias(): String? {
        val aliases = keystore.aliases()
        if (!aliases.hasMoreElements()) return null

        aliases.iterator().forEach {
            if (keystore.getCertificate(it).type == "X.509") {
                return it
            }
        }

        return null
    }

    fun isValid(): Boolean {
        if (getAlias() != null && (keystore.getCertificate(getAlias()) as X509Certificate).notAfter.after(
                Date.from(
                        Instant.now()
                    )
            )
        ) {
            return true
        }
        return false
    }
}
