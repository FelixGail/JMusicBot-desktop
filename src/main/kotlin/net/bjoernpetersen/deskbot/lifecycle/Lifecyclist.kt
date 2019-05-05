package net.bjoernpetersen.deskbot.lifecycle

import com.google.inject.Guice
import com.google.inject.Injector
import com.google.inject.Module
import io.vertx.core.Vertx
import io.vertx.kotlin.core.closeAwait
import javafx.application.Platform
import javafx.concurrent.Task
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import net.bjoernpetersen.deskbot.fximpl.FxInitStateWriter
import net.bjoernpetersen.deskbot.impl.Broadcaster
import net.bjoernpetersen.deskbot.impl.FileConfigStorage
import net.bjoernpetersen.deskbot.impl.FileStorageImpl
import net.bjoernpetersen.deskbot.impl.MainConfigEntries
import net.bjoernpetersen.deskbot.impl.SongPlayedNotifierModule
import net.bjoernpetersen.deskbot.rest.RestModule
import net.bjoernpetersen.deskbot.rest.RestServer
import net.bjoernpetersen.deskbot.view.DeskBot
import net.bjoernpetersen.deskbot.view.get
import net.bjoernpetersen.deskbot.view.show
import net.bjoernpetersen.musicbot.api.auth.BotUser
import net.bjoernpetersen.musicbot.api.auth.DefaultPermissions
import net.bjoernpetersen.musicbot.api.config.ConfigManager
import net.bjoernpetersen.musicbot.api.config.MainConfigScope
import net.bjoernpetersen.musicbot.api.config.PluginConfigScope
import net.bjoernpetersen.musicbot.api.module.BrowserOpenerModule
import net.bjoernpetersen.musicbot.api.module.ConfigModule
import net.bjoernpetersen.musicbot.api.module.DefaultPlayerModule
import net.bjoernpetersen.musicbot.api.module.DefaultQueueModule
import net.bjoernpetersen.musicbot.api.module.DefaultResourceCacheModule
import net.bjoernpetersen.musicbot.api.module.DefaultSongLoaderModule
import net.bjoernpetersen.musicbot.api.module.DefaultUserDatabaseModule
import net.bjoernpetersen.musicbot.api.module.FileStorageModule
import net.bjoernpetersen.musicbot.api.module.InstanceStopper
import net.bjoernpetersen.musicbot.api.module.PluginClassLoaderModule
import net.bjoernpetersen.musicbot.api.module.PluginModule
import net.bjoernpetersen.musicbot.api.player.QueueEntry
import net.bjoernpetersen.musicbot.api.player.Song
import net.bjoernpetersen.musicbot.api.plugin.PluginLoader
import net.bjoernpetersen.musicbot.api.plugin.management.DefaultDependencyManager
import net.bjoernpetersen.musicbot.api.plugin.management.PluginFinder
import net.bjoernpetersen.musicbot.api.plugin.management.findDependencies
import net.bjoernpetersen.musicbot.spi.player.Player
import net.bjoernpetersen.musicbot.spi.player.QueueChangeListener
import net.bjoernpetersen.musicbot.spi.player.SongQueue
import net.bjoernpetersen.musicbot.spi.plugin.NoSuchSongException
import net.bjoernpetersen.musicbot.spi.plugin.Plugin
import net.bjoernpetersen.musicbot.spi.plugin.PluginLookup
import net.bjoernpetersen.musicbot.spi.plugin.Provider
import net.bjoernpetersen.musicbot.spi.plugin.Suggester
import net.bjoernpetersen.musicbot.spi.plugin.category
import net.bjoernpetersen.musicbot.spi.plugin.management.DependencyManager
import net.bjoernpetersen.musicbot.spi.util.BrowserOpener
import org.controlsfx.control.TaskProgressView
import java.io.File
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import javax.inject.Inject
import kotlin.concurrent.thread
import kotlin.concurrent.withLock
import kotlin.coroutines.CoroutineContext

class Lifecyclist : CoroutineScope {
    private val logger = KotlinLogging.logger {}

    private lateinit var job: Job
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default + job

    var stage: Stage = Stage.New
        private set

    // Created stage vars
    private lateinit var configManager: ConfigManager
    private lateinit var classLoader: ClassLoader
    private lateinit var dependencyManager: DependencyManager

    // Injected stage vars
    private lateinit var pluginFinder: PluginFinder
    private lateinit var injector: Injector
    private lateinit var mainConfig: MainConfigEntries

    // Run stage vars
    private lateinit var broadcaster: Broadcaster

    private fun <T> stagedBlock(stage: Stage, exact: Boolean = true, action: () -> T): T {
        if (exact) {
            if (this.stage != stage) throw IllegalStateException()
        } else {
            if (this.stage < stage) throw IllegalStateException()
        }
        return action()
    }

    private suspend fun <T> staged(
        stage: Stage,
        exact: Boolean = true,
        action: suspend () -> T
    ): T {
        if (exact) {
            if (this.stage != stage) throw IllegalStateException()
        } else {
            if (this.stage < stage) throw IllegalStateException()
        }
        return action()
    }

    fun getConfigManager() = stagedBlock(Stage.Created, false) { configManager }
    fun getPluginClassLoader() = stagedBlock(Stage.Created, false) { classLoader }

    fun getDependencyManager() = stagedBlock(
        Stage.Created, false
    ) { dependencyManager }

    fun getPluginFinder() = stagedBlock(Stage.Injected, false) { pluginFinder }
    fun getInjector() = stagedBlock(Stage.Injected, false) { injector }
    fun getMainConfig() = stagedBlock(Stage.Injected, false) { mainConfig }

    private fun createConfig() {
        configManager = ConfigManager(
            FileConfigStorage(configDir),
            FileConfigStorage(secretDir),
            FileConfigStorage(stateDir)
        )
    }

    private fun createPlugins(pluginDir: File) {
        val loader = PluginLoader(pluginDir)
        dependencyManager = DefaultDependencyManager(configManager[MainConfigScope].state, loader)
        classLoader = loader.loader
    }

    fun create(pluginDir: File): DependencyManager = stagedBlock(Stage.New) {
        job = Job()
        createConfig()
        createPlugins(pluginDir)

        stage = Stage.Created
        dependencyManager
    }

    private fun modules(browserOpener: BrowserOpener, suggester: Suggester?): List<Module> = listOf(
        ConfigModule(configManager),
        DefaultPlayerModule(suggester),
        DefaultQueueModule(),
        DefaultSongLoaderModule(),
        DefaultUserDatabaseModule("jdbc:sqlite:UserDatabase.db"),
        PluginClassLoaderModule(classLoader),
        PluginModule(pluginFinder),
        BrowserOpenerModule(browserOpener),
        SongPlayedNotifierModule(),
        RestModule(),
        DefaultResourceCacheModule(),
        FileStorageModule(FileStorageImpl::class)
    )

    fun inject(browserOpener: BrowserOpener) = stagedBlock(Stage.Created) {
        pluginFinder = dependencyManager.finish()

        mainConfig = MainConfigEntries(configManager, pluginFinder, classLoader)

        val suggester = mainConfig.defaultSuggester.get()
        logger.info { "Default suggester: ${suggester?.name}" }

        injector = Guice.createInjector(modules(browserOpener, suggester))

        pluginFinder.allPlugins().forEach {
            injector.injectMembers(it)
        }

        pluginFinder.allPlugins().forEach {
            val configs = configManager[PluginConfigScope(it::class)]
            it.createConfigEntries(configs.plain)
            it.createSecretEntries(configs.secrets)
            it.createStateEntries(configs.state)
        }

        stage = Stage.Injected
    }

    suspend fun run(result: (Throwable?) -> Unit) = staged(Stage.Injected) {
        // TODO rollback in case of failure
        coroutineScope {
            Initializer(pluginFinder).start {
                if (it != null) {
                    logger.error(it) { "Could not initialize!" }
                    result(it)
                    return@start
                }
                DefaultPermissions.defaultPermissions = mainConfig.defaultPermissions.get()!!

                injector.getInstance(Player::class.java).start()

                val vertx = injector.getInstance(Vertx::class.java)
                val rest = injector.getInstance(RestServer::class.java)
                vertx.deployVerticle(rest)

                broadcaster = Broadcaster().apply { start() }

                GlobalScope.launch {
                    val dumper = injector.getInstance(QueueDumper::class.java)
                    dumper.restoreQueue()
                    injector.getInstance(SongQueue::class.java)
                        .addListener(object : QueueChangeListener {
                            override fun onAdd(entry: QueueEntry) {
                                dumper.dumpQueue()
                            }

                            override fun onMove(entry: QueueEntry, fromIndex: Int, toIndex: Int) {
                                dumper.dumpQueue()
                            }

                            override fun onRemove(entry: QueueEntry) {
                                dumper.dumpQueue()
                            }
                        })
                }

                DeskBot.runningInstance = this@Lifecyclist
                stage = Stage.Running
                result(null)
            }
        }
    }

    fun stop() = stagedBlock(Stage.Running) {
        try {
            broadcaster.close()
        } catch (e: Exception) {
            logger.error(e) { "Could not close broadcaster" }
        }

        runBlocking {
            coroutineScope {
                withContext(coroutineContext) {
                    val stopper = InstanceStopper(injector).apply {
                        register(Vertx::class.java) { vertx ->
                            vertx.closeAwait()
                        }
                    }
                    stopper.stop()
                    stage = Stage.Stopped
                }
            }

            job.cancel()
        }
    }

    enum class Stage {
        Stopped, New, Created, Injected, Running
    }

    private companion object {
        val stateDir = File("state");
        val configDir = File("config")
        val secretDir = File(configDir, "secret")
    }
}

private class Initializer(private val finder: PluginFinder) {

    private val logger = KotlinLogging.logger {}

    fun start(result: (Throwable?) -> Unit) {
        val view = TaskProgressView<Task<*>>()
        val tasks = view.tasks

        val lock: Lock = ReentrantLock()
        val done = lock.newCondition()!!
        val finished: MutableSet<Plugin> = HashSet(64)
        val errors: MutableList<Throwable> = ArrayList()

        val res = DeskBot.resources
        val window = view.show(modal = true, title = res["window.initialization"])

        val parentTask = object : Task<Unit>() {
            override fun call() {
                updateTitle(res["task.parent.title"])
                updateMessage(res["task.parent.description"])

                // TODO timeout/cancel
                val todo = finder.run {
                    genericPlugins.size + playbackFactories.size + providers.size + suggesters.size
                }.toLong()
                updateProgress(0, todo)
                lock.withLock {
                    var finishedCount = finished.size.toLong()
                    while (finishedCount != todo) {
                        updateProgress(finishedCount, todo)
                        done.await()
                        finishedCount = finished.size.toLong()
                    }
                }

                val exception = if (errors.isEmpty()) null
                else errors.fold(Exception("One or more initializations failed")) { e, t ->
                    e.apply { addSuppressed(t) }
                }

                Platform.runLater {
                    window.close()
                    result(exception)
                }
            }
        }
        thread(name = "InitializationParent", isDaemon = true) { parentTask.run() }
        tasks.add(parentTask)

        finder.allPlugins().forEach { plugin ->
            val task = object : Task<Unit>() {
                val writer = FxInitStateWriter(::updateMessage)

                override fun call() {
                    updateTitle(
                        res["task.plugin.title"].format(plugin.category.simpleName, plugin.name)
                    )
                    plugin.findDependencies()
                        .asSequence()
                        .map { finder[it]!! }
                        .forEach {
                            if (it !in finished) {
                                val type = it.category.simpleName
                                lock.withLock {
                                    while (it !in finished) {
                                        writer.state(
                                            res["task.plugin.waiting"].format(type, it.name)
                                        )
                                        done.await()
                                    }
                                }
                            }
                        }
                    writer.state("Starting...")

                    runBlocking {
                        try {
                            plugin.initialize(writer)
                        } catch (e: Throwable) {
                            logger.error(e) { "Could not initialize $plugin" }
                            throw e
                        }
                    }

                    lock.withLock {
                        finished.add(plugin)
                        done.signalAll()
                    }
                }
            }
            tasks.add(task)
            thread(isDaemon = true, name = "Init${plugin.category.simpleName}${plugin.name}") {
                task.run()
            }
        }
    }
}

private class QueueDumper @Inject private constructor(
    private val queue: SongQueue,
    private val player: Player,
    private val pluginLookup: PluginLookup
) {

    private val logger = KotlinLogging.logger {}

    private fun Song.toDumpString() = "${provider.id}|$id\n"

    fun dumpQueue() {
        logger.info { "Dumping queue" }
        val file = File("queue.dump")
        file.bufferedWriter().use { writer ->
            val currentEntry = player.state.entry
            // Assert that there is a current song and that it wasn't suggested
            if (currentEntry != null && currentEntry.user != null) {
                logger.debug { "Dumping current song first" }
                writer.write(currentEntry.song.toDumpString())
            } else {
                logger.debug { "Not dumping current song. State: ${player.state}" }
            }

            queue.toList().asSequence()
                .map { it.song }
                .map { it.toDumpString() }
                .forEach { writer.write(it) }
        }
    }

    suspend fun restoreQueue() {
        val file = File("queue.dump")
        if (!file.isFile) return

        logger.info("Restoring queue")
        withContext(Dispatchers.IO) {
            file.bufferedReader().useLines { lines ->
                val songs = lines
                    .map { it.split('|') }
                    .filter { it.size == 2 }
                    .map { pluginLookup.lookup<Provider>(it[0]) to it[1] }
                    .map {
                        async {
                            try {
                                it.first?.lookup(it.second)
                            } catch (e: NoSuchSongException) {
                                null
                            }
                        }
                    }
                    .toList()

                withContext(Dispatchers.IO) {
                    songs.forEach {
                        val song = it.await()
                        if (song != null) {
                            queue.insert(QueueEntry(song, BotUser))
                        }
                    }
                }
            }

        }
    }
}