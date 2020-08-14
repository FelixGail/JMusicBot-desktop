import com.diffplug.spotless.LineEnding
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("com.diffplug.gradle.spotless") version Plugin.SPOTLESS
    id("io.gitlab.arturbosch.detekt") version Plugin.DETEKT

    id("com.github.ben-manes.versions") version Plugin.VERSIONS

    kotlin("jvm") version Plugin.KOTLIN
    application
    idea
}

group = "com.github.bjoernpetersen"
version = "0.19.0"

repositories {
    mavenLocal {
        mavenContent {
            snapshotsOnly()
        }
    }
    jcenter()
    maven("https://oss.sonatype.org/content/repositories/snapshots") {
        mavenContent {
            snapshotsOnly()
        }
    }
}

application {
    applicationName = "musicbot-desktop"
    mainClassName = "net.bjoernpetersen.deskbot.view.DeskBot"
}

tasks.named<CreateStartScripts>("startScripts") {
    applicationName = "musicbot-desktop"
}
val headlessStartScripts by tasks.register("headlessStartScripts", CreateStartScripts::class) {
    applicationName = "musicbot-headless"
    outputDir = file("build/scripts") // By putting these scripts here, they will be picked up automatically by the installDist task
    mainClassName = "net.bjoernpetersen.deskbot.view.DeskBotHeadless"
    classpath = project.tasks.getAt(JavaPlugin.JAR_TASK_NAME).outputs.files.plus(project.configurations.getByName(JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME)) // I took this from ApplicationPlugin.java:129
}
tasks.named("installDist") {
    dependsOn(headlessStartScripts)
}

idea {
    module {
        isDownloadJavadoc = true
    }
}

spotless {
    kotlin {
        ktlint()
        lineEndings = LineEnding.UNIX
        endWithNewline()
    }
    kotlinGradle {
        ktlint()
        lineEndings = LineEnding.UNIX
        endWithNewline()
    }
    format("markdown") {
        target("**/*.md")
        lineEndings = LineEnding.UNIX
        endWithNewline()
    }
}

detekt {
    toolVersion = Plugin.DETEKT
    config = files("$rootDir/buildConfig/detekt.yml")
    buildUponDefaultConfig = true
}

configurations.all {
    resolutionStrategy.cacheChangingModulesFor(1, TimeUnit.MINUTES)
}

tasks {
    withType<KotlinCompile> {
        kotlinOptions {
            jvmTarget = "1.8"
            freeCompilerArgs = listOf(
                "-Xopt-in=kotlin.RequiresOptIn",
                "-Xnew-inference"
            )
        }
    }

    "processResources"(ProcessResources::class) {
        filesMatching("**/version.properties") {
            filter {
                it.replace("%APP_VERSION%", version.toString())
            }
        }
    }

    withType<Test> {
        useJUnitPlatform()
    }

    withType<Jar> {
        from(project.projectDir) {
            include("LICENSE")
        }
    }

    dependencyUpdates {
        rejectVersionIf {
            isUnstable(candidate.version, currentVersion)
        }
    }
}

dependencies {
    runtimeOnly(
        group = "org.slf4j",
        name = "slf4j-simple",
        version = Lib.SLF4J
    )
    runtimeOnly(group = "org.xerial", name = "sqlite-jdbc", version = Lib.SQLITE)
    implementation(
        group = "com.github.bjoernpetersen",
        name = "musicbot",
        version = Lib.MUSICBOT
    ) {
        isChanging = Lib.MUSICBOT.contains("SNAPSHOT")
    }

    implementation(
        group = "org.jetbrains.kotlinx",
        name = "kotlinx-coroutines-core",
        version = Lib.KOTLIN_COROUTINES
    )

    implementation(
        group = "org.jetbrains.kotlinx",
        name = "kotlinx-coroutines-javafx",
        version = Lib.KOTLIN_COROUTINES
    )

    // Ktor
    implementation(
        group = "io.ktor",
        name = "ktor-client-okhttp",
        version = Lib.KTOR
    )

    implementation(
        group = "io.ktor",
        name = "ktor-server-cio",
        version = Lib.KTOR
    )
    implementation(
        group = "io.ktor",
        name = "ktor-locations",
        version = Lib.KTOR
    )
    implementation(
        group = "io.ktor",
        name = "ktor-jackson",
        version = Lib.KTOR
    )

    // JavaFX
    implementation("org.controlsfx:controlsfx:${Lib.CONTROLS_FX}")

    implementation(
        group = "com.fasterxml.jackson.core",
        name = "jackson-databind",
        version = Lib.JACKSON
    )
    implementation(
        group = "com.fasterxml.jackson.module",
        name = "jackson-module-kotlin",
        version = Lib.JACKSON
    )
    implementation(
        group = "com.fasterxml.jackson.dataformat",
        name = "jackson-dataformat-yaml",
        version = Lib.JACKSON
    )

    testRuntimeOnly(
        group = "org.junit.jupiter",
        name = "junit-jupiter-engine",
        version = Lib.JUNIT
    )
    testImplementation(
        group = "org.junit.jupiter",
        name = "junit-jupiter-api",
        version = Lib.JUNIT
    )
    testImplementation(
        group = "name.falgout.jeffrey.testing.junit5",
        name = "guice-extension",
        version = Lib.JUNIT_GUICE
    )
    testImplementation(group = "io.mockk", name = "mockk", version = Lib.MOCK_K)
    testImplementation(group = "org.assertj", name = "assertj-core", version = Lib.ASSERT_J)
}
