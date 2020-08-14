package net.bjoernpetersen.deskbot.view

import kotlinx.coroutines.runBlocking
import net.bjoernpetersen.deskbot.impl.ConsolePrinterBrowserOpener
import net.bjoernpetersen.deskbot.initialization.ConsoleInitializationHandler
import net.bjoernpetersen.deskbot.lifecycle.Lifecyclist
import java.io.File
import kotlin.system.exitProcess

class DeskBotHeadless {

    companion object {
        fun start() {
            val cycle = Lifecyclist()
            cycle.create(File("plugins"))
            cycle.inject(ConsolePrinterBrowserOpener())
            runBlocking {
                cycle.run(ConsoleInitializationHandler()) {
                    if (it != null) {
                        exitProcess(1)
                    }
                    while (cycle.stage != Lifecyclist.Stage.Stopped) {
                        cycle.awaitStageChange()
                    }
                }
            }
        }

        fun stop() {
            DeskBotInfo.runningInstance?.apply {
                if (stage == Lifecyclist.Stage.Running) {
                    Thread { stop() }.start()
                }
            }
        }

        @Suppress("SpreadOperator")
        @JvmStatic
        fun main(args: Array<String>) {
            Runtime.getRuntime().addShutdownHook(Thread {
                stop()
            })
            start()
        }
    }
}
