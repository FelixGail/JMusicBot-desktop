package net.bjoernpetersen.deskbot.cert

import java.security.KeyStore
import java.security.cert.X509Certificate
import java.time.Instant
import java.util.Date

data class Certificate(val passphrase: String) {
    val keystore: KeyStore = KeyStore.getInstance("JKS")

    var alias: String? = null
        get() {
            if(field != null) {
                return field
            }
            val aliases = keystore.aliases()
            if (!aliases.hasMoreElements()) return null

            aliases.iterator().forEach {
                if(keystore.getCertificate(it).type == "X.509") {
                    field = alias
                    return alias
                }
            }

            return null
        }

    fun isValid(): Boolean {
        if(alias != null && (keystore.getCertificate(alias) as X509Certificate).notAfter.after(Date.from(Instant.now()))) {
            return true
        }
        return false
    }
}