package net.bjoernpetersen.deskbot.initialization

import mu.KotlinLogging
import net.bjoernpetersen.musicbot.spi.plugin.management.InitStateWriter
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

abstract class InitializationHandler {
    private val done = mutableListOf<Int>()
    private val tasks = mutableListOf<InitializationTask>()
    private val doneLock = ReentrantLock()
    private val doneCondition = doneLock.newCondition()
    private val logger = KotlinLogging.logger { }

    fun addTask(task: InitializationTask) {
        tasks.add(task)
    }

    fun addAll(tasks: Collection<InitializationTask>) {
        this.tasks.addAll(tasks)
    }

    private fun getInitializableRunnables(): List<Pair<String, (writer: InitStateWriter) -> Unit>> {
        return tasks
            .filter {
                doneLock.withLock {
                    it.state == TaskState.WAITING && done.containsAll(it.dependencies.orEmpty())
                }
            }
            .map {
                Pair(it.name, { writer ->
                    val run = doneLock.withLock {
                        return@withLock if (it.state == TaskState.WAITING) {
                            it.state = TaskState.RUNNING
                            true
                        } else {
                            false
                        }
                    }
                    if (run) {
                        @Suppress("TooGenericExceptionCaught")
                        try {
                            logger.info { "Initializing ${it.name}" }
                            it.call(writer)
                            logger.info { "Finished Initialization of ${it.name}" }
                            doneLock.withLock {
                                done.add(it.identity)
                            }
                        } catch (e: Throwable) {
                            logger.error(e) { "Error during initialization of ${it.name}" }
                            it.state = TaskState.FAILED
                            tasks
                                .filter { depTask ->
                                    depTask.dependencies?.contains(it.identity) ?: false
                                }
                                .forEach { depTask ->
                                    depTask.state = TaskState.FAILED
                                    logger.error { "Subsequent task ${depTask.name} failed because of ${it.name}" }
                                }
                        } finally {
                            doneLock.withLock {
                                doneCondition.signalAll()
                            }
                        }
                    }
                })
            }
    }

    protected abstract suspend fun runTasks(tasks: List<Pair<String, (writer: InitStateWriter) -> Unit>>)

    abstract suspend fun initialize()
    protected abstract fun teardown()

    protected suspend fun start() {
        val allIdentities = tasks.fold(mutableListOf<Int>()) { acc, initializationTask ->
            if (acc.contains(initializationTask.identity)) {
                throw IllegalArgumentException("List of initialization tasks contains identity ${initializationTask.identity} twice!")
            }
            acc.add(initializationTask.identity)
            acc
        }
        tasks.find {
            !allIdentities.containsAll(it.dependencies.orEmpty())
        }.also {
            if (it != null) {
                throw IllegalArgumentException("Cannot initialize. ${it.name} has unresolvable initialization dependencies")
            }
        }
        while (tasks.any { it.state == TaskState.WAITING }) {
            runTasks(getInitializableRunnables())
            doneLock.withLock {
                doneCondition.await()
            }
        }
        teardown()
    }
}
