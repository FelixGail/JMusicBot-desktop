package net.bjoernpetersen.deskbot.initialization

import mu.KotlinLogging
import net.bjoernpetersen.deskbot.fximpl.FxInitStateWriter
import net.bjoernpetersen.musicbot.spi.plugin.management.InitStateWriter
import kotlin.concurrent.thread

class ConsoleInitializationHandler : InitializationHandler() {
    private val logger = KotlinLogging.logger { }

    override suspend fun runTasks(tasks: List<Pair<String, (writer: InitStateWriter) -> Unit>>) {
        tasks.forEach { (name, task) ->
            val writer = FxInitStateWriter { string -> logger.info { "$name: $string" } }

            thread(isDaemon = true, name = "Init$name") {
                task(writer)
            }
        }
    }

    override suspend fun initialize() {
        start()
    }

    override fun teardown() {
    }
}
