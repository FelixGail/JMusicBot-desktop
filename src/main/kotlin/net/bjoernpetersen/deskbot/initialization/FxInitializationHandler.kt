package net.bjoernpetersen.deskbot.initialization

import javafx.application.Platform
import javafx.concurrent.Task
import javafx.stage.Stage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.bjoernpetersen.deskbot.fximpl.FxInitStateWriter
import net.bjoernpetersen.deskbot.view.DeskBotInfo
import net.bjoernpetersen.deskbot.view.get
import net.bjoernpetersen.deskbot.view.show
import net.bjoernpetersen.musicbot.spi.plugin.management.InitStateWriter
import org.controlsfx.control.TaskProgressView
import kotlin.concurrent.thread

class FxInitializationHandler : InitializationHandler() {
    private val progressView = TaskProgressView<Task<*>>()
    val res = DeskBotInfo.resources
    lateinit var window: Stage

    override suspend fun runTasks(tasks: List<Pair<String, (writer: InitStateWriter) -> Unit>>) {
        tasks.forEach { (name, task) ->
            val fxTask = object : Task<Unit>() {
                val writer = FxInitStateWriter(::updateMessage)

                override fun call() {
                    updateTitle(
                        res["task.initialization.title"].format(name)
                    )
                    task(writer)
                }
            }
            progressView.tasks.add(fxTask)
            thread(isDaemon = true, name = "Init$name") {
                fxTask.run()
            }
        }
    }

    override suspend fun initialize() {
        GlobalScope.launch {
            withContext(Dispatchers.JavaFx) {
                window = progressView.show(wait = false, modal = true, title = res["window.initialization"])
            }
        }
        start()
    }

    override fun teardown() {
        Platform.runLater {
            window.close()
        }
    }
}
