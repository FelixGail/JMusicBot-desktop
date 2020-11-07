package net.bjoernpetersen.deskbot.rest.location

import com.github.zafarkhaja.semver.ParseException
import com.google.inject.AbstractModule
import com.google.inject.Inject
import com.google.inject.Scopes
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.Location
import net.bjoernpetersen.deskbot.rest.model.ImplementationInfo
import net.bjoernpetersen.deskbot.rest.model.VersionInfo
import java.io.IOException
import java.util.Properties
import java.io.IOException
import java.util.Properties
import net.bjoernpetersen.deskbot.impl.MainConfigEntries
import net.bjoernpetersen.musicbot.spi.version.ImplementationInfo
import net.bjoernpetersen.musicbot.spi.version.Version

private const val PROJECT_PAGE = "https://github.com/BjoernPetersen/MusicBot-desktop"
private const val PROJECT_NAME = "DeskBot"

object VersionConstraints {
    const val PATH = "/version"
}

class VersionImpl @Inject private constructor(
    private val mainConfigEntries: MainConfigEntries
) : Version {

    override val apiVersion by lazy { loadApiVersion() }
    override val botName by lazy { mainConfigEntries.instanceName.get()!! }
    override val implementation by lazy {
        ImplementationInfo(
            PROJECT_PAGE,
            PROJECT_NAME,
            loadImplementationVersion()
        )
    }

    companion object {
        private fun loadImplementationVersion() = try {
            val properties = Properties()
            Version::class.java
                .getResourceAsStream("/net/bjoernpetersen/deskbot/version.properties")
                .use { versionStream -> properties.load(versionStream) }
            properties.getProperty("version") ?: throw IllegalStateException("Version is missing")
        } catch (e: IOException) {
            throw IllegalStateException("Could not read version resource", e)
        } catch (e: ParseException) {
            throw IllegalStateException("Could not read version resource", e)
        }

        private fun loadApiVersion(): String = "0.15.3"
    }
}

class VersionModule() : AbstractModule() {

    @KtorExperimentalLocationsAPI
    override fun configure() {
        bind(Version::class.java).to(VersionImpl::class.java).`in`(Scopes.SINGLETON)
    }
}
