package net.bjoernpetersen.deskbot.initialization

import net.bjoernpetersen.musicbot.spi.plugin.management.InitStateWriter

/**
 * A Task that will be scheduled for initialization
 *
 * @param identity An unambiguous identity for this task
 * @param dependencies An unambiguous list of task identities this task depends on
 */
abstract class InitializationTask(
    val identity: Int,
    val name: String,
    val dependencies: Collection<Int>?
) {
    var state: TaskState = TaskState.WAITING

    abstract fun call(writer: InitStateWriter)
}

enum class TaskState {
    WAITING,
    RUNNING,
    FAILED
}
