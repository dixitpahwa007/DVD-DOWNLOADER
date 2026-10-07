import org.gradle.api.initialization.resolve.RepositoriesMode

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)

    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "DVD-DOWNLOADER"

include(":app")

val mediaDir = file("media")

if (mediaDir.exists()) {

    // Media3 1.5.1 expects these properties on Gradle's
    // extra properties extension.
    gradle.extra["androidxMediaSettingsDir"] = mediaDir.canonicalPath
    gradle.extra["androidxMediaModulePrefix"] = "media3-"

    fun includeMedia3Module(
        name: String,
        directory: String
    ) {
        val projectPath = ":media3-lib-$name"
        val projectDirectory = File(
            mediaDir,
            "libraries/$directory"
        )

        if (projectDirectory.isDirectory) {
            include(projectPath)
            project(projectPath).projectDir = projectDirectory
        }
    }

    includeMedia3Module("common", "common")
    includeMedia3Module("common-ktx", "common_ktx")

    includeMedia3Module("container", "container")
    includeMedia3Module("session", "session")

    includeMedia3Module("exoplayer", "exoplayer")
    includeMedia3Module("exoplayer-dash", "exoplayer_dash")
    includeMedia3Module("exoplayer-hls", "exoplayer_hls")
    includeMedia3Module("exoplayer-rtsp", "exoplayer_rtsp")
    includeMedia3Module(
        "exoplayer-smoothstreaming",
        "exoplayer_smoothstreaming"
    )
    includeMedia3Module("exoplayer-ima", "exoplayer_ima")
    includeMedia3Module(
        "exoplayer-workmanager",
        "exoplayer_workmanager"
    )

    includeMedia3Module("ui", "ui")
    includeMedia3Module("ui-leanback", "ui_leanback")

    include
