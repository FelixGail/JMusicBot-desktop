package net.bjoernpetersen.deskbot.fximpl

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import mu.KotlinLogging
import net.bjoernpetersen.musicbot.api.plugin.category
import net.bjoernpetersen.musicbot.spi.plugin.Plugin
import net.bjoernpetersen.musicbot.spi.plugin.management.InitStateWriter
import kotlin.coroutines.CoroutineContext

class FxInitStateWriter(private val updateMessage: (String) -> Unit) : InitStateWriter,
    CoroutineScope {

    private val logger = KotlinLogging.logger { }

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main

    override fun state(state: String) {
        GlobalScope.launch {
            updateMessage(state)
        }
    }

    override fun warning(warning: String) {
        GlobalScope.launch {
            updateMessage("WARNING: $warning")
            logger.warn { warning }
        }
    }
}

private fun Plugin.describe(): String {
    return "${category.simpleName} $name"
}
