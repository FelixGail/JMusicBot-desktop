package net.bjoernpetersen.deskbot.rest.feature

import io.ktor.application.Application
import io.ktor.application.ApplicationCall
import io.ktor.application.ApplicationFeature
import io.ktor.application.call
import io.ktor.response.ApplicationSendPipeline
import io.ktor.response.header
import io.ktor.util.AttributeKey
import io.ktor.util.pipeline.PipelineContext
import io.ktor.util.pipeline.PipelinePhase

/**
 * Add [AccessControlHeader]s to every response
 */
class AccessControl(private val configuration: Configuration) {

    /**
     * Configuration for the [AccessControl]. Add allowed origins via the [allowOrigin] function.
     */
    class Configuration {
        internal val origins = mutableListOf<String>()

        /**
         * Add an allowed origin
         *
         * @param origin Allowed origin as string
         */
        fun allowOrigin(origin: String) {
            origins.add(origin)
        }
    }

    private fun addOriginHeaders(context: PipelineContext<Any, ApplicationCall>) {
        context.apply {
            configuration.origins.forEach {
                call.response.header(Feature.AccessControlHeader, it)
            }
        }
    }

    /**
     * Installable Feature for [AccessControl]
     */
    companion object Feature : ApplicationFeature<Application, Configuration, AccessControl> {
        /**
         * Allow-Origin header key
         */
        const val AccessControlHeader = "Access-Control-Allow-Origin"

        override val key: AttributeKey<AccessControl> =
                AttributeKey<AccessControl>("AccessControlFeature")

        override fun install(pipeline: Application, configure: Configuration.() -> Unit):
                AccessControl {
            val configuration = Configuration().apply(configure)
            val feature = AccessControl(configuration)

            val accessControlPhase = PipelinePhase("accessControl")
            pipeline.sendPipeline.insertPhaseAfter(ApplicationSendPipeline.Render, accessControlPhase)
            pipeline.sendPipeline.intercept(accessControlPhase) {
                feature.addOriginHeaders(this)
            }

            return feature
        }
    }
}
