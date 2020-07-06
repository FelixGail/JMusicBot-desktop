package net.bjoernpetersen.deskbot.cert

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.url
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import net.bjoernpetersen.musicbot.api.config.ConfigManager
import net.bjoernpetersen.musicbot.api.config.GenericConfigScope
import net.bjoernpetersen.musicbot.api.config.listSerializer
import net.bjoernpetersen.musicbot.api.config.serialization
import net.bjoernpetersen.musicbot.api.config.serialized
import net.bjoernpetersen.musicbot.api.config.string
import net.bjoernpetersen.musicbot.spi.plugin.management.InitStateWriter
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.URL
import java.nio.file.Path
import java.util.Base64
import javax.inject.Inject

private val certificateSerializer = serialization<Pair<String, String>> {
    deserialize { it.split('|').let { (a, b) -> a to b } }
    serialize { "${it.first}|${it.second}" }
}

class CertificateHandler @Inject() private constructor(
    configManager: ConfigManager
) {

    private val logger = KotlinLogging.logger {}
    lateinit var certificate: Certificate

    val key by configManager[GenericConfigScope(CertificateHandler::class)].secrets.string {
        description = ""
        check { null }
    }

    /**
     * Maps InetAddress to domains
     */
    val domains by configManager[GenericConfigScope(CertificateHandler::class)].plain
        .serialized<Map<String, String>> {
            description = ""
            check { null }
            serialization {
                deserialize {
                    certificateSerializer.listSerializer()
                        .deserialize(it)
                        .fold(mutableMapOf(), { acc, pair -> acc[pair.first] = pair.second; return@fold acc })
                }

                serialize {
                    certificateSerializer.listSerializer()
                        .serialize(it.map { entry -> entry.key to entry.value })
                }
            }
        }

    fun loadCertificate(path: Path, passphrase: String): Certificate {
        val cert = Certificate(passphrase)
        cert.keystore.load(path.toFile().inputStream(), passphrase.toCharArray())
        return cert
    }

    @KtorExperimentalAPI
    suspend fun retrieveCertificate(url: String, addresses: List<String>): Certificate {
        return withContext(Dispatchers.IO) {
            val client = HttpClient(OkHttp) {
                install(JsonFeature) {
                    serializer = JacksonSerializer()
                }
            }
            val response = client.post<InitialResponse> {
                url(url)
                contentType(ContentType.Application.Json)
                body = InitialRequest(addresses)
            }
            domains.set(response.domains)
            val certificate = Certificate(response.token)
            assert(response.keyFormat == "jks")

            var done = false
            while (!done) {
                val certResponse = client.get<CertificateResponse> {
                    url(url)
                    contentType(ContentType.Application.Json)
                    body = CertificateRequest(certificate.passphrase)
                }
                if (certResponse.hasCertificate) {
                    assert(certResponse.jks != null)
                    val decodedStream = Base64.getDecoder().decode(certResponse.jks!!.jks).inputStream()
                    certificate.keystore.load(
                        decodedStream,
                        certificate.passphrase.toCharArray()
                    )
                    done = true;
                }
                delay(10000L)
            }
            return@withContext certificate
        }
    }

    @KtorExperimentalAPI
    suspend fun acquireCertificate(path: Path, url: String, writer: InitStateWriter) {
        writer.state("Try loading certificate from disk")
        val file = path.toFile()
        if (file.exists() && !key.get().isNullOrEmpty() &&
            (domains.get() == null || findAdresses().containsAll(domains.get()!!.keys))) {
            try {
                val cert = loadCertificate(path, key.get()!!)
                if (cert.isValid()) {
                    certificate = cert
                    return
                }
            } catch (exc: Exception) {
                logger.error(exc) {"Unable to load cert from disk"}
                //Loading failed - do nothing
            }
        }
        writer.state("Requesting new certificate ")
        val cert = retrieveCertificate(url, findAdresses())
        assert(cert.isValid())
        if (file.exists()) {
            file.delete()
        }
        withContext(Dispatchers.IO) {
            val stream = file.outputStream()
            cert.keystore.store(stream, cert.passphrase.toCharArray())
            stream.close()
        }
        certificate = cert
        key.set(cert.passphrase)
    }

    private fun findAdresses(): List<String> {
        return NetworkInterface.getNetworkInterfaces().asSequence()
            .filter {
                !it.isLoopback &&
                        !it.isVirtual &&
                        it.isUp
            }.flatMap {
                it.inetAddresses.toList().filter { a ->
                    !a.isLoopbackAddress &&
                            !a.isMulticastAddress &&
                            a is Inet4Address
                }.asSequence()
            }.map { i -> i.hostAddress }.toList()
    }

}

data class InitialRequest(val ips: List<String>, val keyFormat: String = "jks")
data class InitialResponse(val domains: Map<String, String>, val token: String, val keyFormat: String)
data class CertificateRequest(val token: String)
data class CertificateResponse(val hasCertificate: Boolean, val pem: Pem?, val p12: P12?, val jks: Jks?)
data class Pem(val crt: String, val key: String)
data class P12(val p12: String)
data class Jks(val jks: String)
