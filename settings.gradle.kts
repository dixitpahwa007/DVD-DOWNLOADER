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

    // Media3 1.5.1 expects these Gradle properties.
    extra["androidxMediaSettingsDir"] = mediaDir.canonicalPath
    extra["androidxMediaModulePrefix"] = "media3-"

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
    includeMedia3Module("exoplayer-smoothstreaming", "exoplayer_smoothstreaming")
    includeMedia3Module("exoplayer-ima", "exoplayer_ima")
    includeMedia3Module("exoplayer-workmanager", "exoplayer_workmanager")

    includeMedia3Module("ui", "ui")
    includeMedia3Module("ui-leanback", "ui_leanback")

    includeMedia3Module("database", "database")

    includeMedia3Module("datasource", "datasource")
    includeMedia3Module("datasource-cronet", "datasource_cronet")
    includeMedia3Module("datasource-rtmp", "datasource_rtmp")
    includeMedia3Module("datasource-okhttp", "datasource_okhttp")

    includeMedia3Module("decoder", "decoder")
    includeMedia3Module("decoder-av1", "decoder_av1")

    // FFmpeg decoder is built locally by the GitHub Actions workflow.
    includeMedia3Module("decoder-ffmpeg", "decoder_ffmpeg")

    includeMedia3Module("decoder-flac", "decoder_flac")
    includeMedia3Module("decoder-iamf", "decoder_iamf")
    includeMedia3Module("decoder-opus", "decoder_opus")
    includeMedia3Module("decoder-vp9", "decoder_vp9")

    includeMedia3Module("extractor", "extractor")

    includeMedia3Module("effect", "effect")
    includeMedia3Module("muxer", "muxer")
    includeMedia3Module("transformer", "transformer")

    // Media3 1.5.1 refers to this project as :media3-test-utils.
    val testUtilsDirectory = File(
        mediaDir,
        "libraries/test_utils"
    )

    if (testUtilsDirectory.isDirectory) {
        include(":media3-test-utils")
        project(":media3-test-utils").projectDir = testUtilsDirectory
    }

    // Cast is intentionally not included because the app does not use
    // androidx.media3:media3-cast.
}
