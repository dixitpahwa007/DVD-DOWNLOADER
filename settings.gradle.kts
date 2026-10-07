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

// ============================================================
// Local AndroidX Media3 1.5.1
// ============================================================

val mediaDir = file("media")

if (mediaDir.exists()) {

    // Media3 1.5.1 build scripts expect these Gradle properties.
    gradle.ext["androidxMediaSettingsDir"] = mediaDir.canonicalPath
    gradle.ext["androidxMediaModulePrefix"] = "media3-"

    // --------------------------------------------------------
    // Helper for including Media3 modules only when their
    // source directory actually exists in Media3 1.5.1.
    // --------------------------------------------------------

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

    // --------------------------------------------------------
    // Core
    // --------------------------------------------------------

    includeMedia3Module(
        "common",
        "common"
    )

    includeMedia3Module(
        "common-ktx",
        "common_ktx"
    )

    includeMedia3Module(
        "container",
        "container"
    )

    // --------------------------------------------------------
    // Session / ExoPlayer
    // --------------------------------------------------------

    includeMedia3Module(
        "session",
        "session"
    )

    includeMedia3Module(
        "exoplayer",
        "exoplayer"
    )

    includeMedia3Module(
        "exoplayer-dash",
        "exoplayer_dash"
    )

    includeMedia3Module(
        "exoplayer-hls",
        "exoplayer_hls"
    )

    includeMedia3Module(
        "exoplayer-rtsp",
        "exoplayer_rtsp"
    )

    includeMedia3Module(
        "exoplayer-smoothstreaming",
        "exoplayer_smoothstreaming"
    )

    includeMedia3Module(
        "exoplayer-ima",
        "exoplayer_ima"
    )

    includeMedia3Module(
        "exoplayer-workmanager",
        "exoplayer_workmanager"
    )

    // --------------------------------------------------------
    // UI
    // --------------------------------------------------------

    includeMedia3Module(
        "ui",
        "ui"
    )

    includeMedia3Module(
        "ui-leanback",
        "ui_leanback"
    )

    // --------------------------------------------------------
    // DataSource
    // --------------------------------------------------------

    includeMedia3Module(
        "database",
        "database"
    )

    includeMedia3Module(
        "datasource",
        "datasource"
    )

    includeMedia3Module(
        "datasource-cronet",
        "datasource_cronet"
    )

    // Media3 1.5.1 checkout does not contain:
    // libraries/datasource_httpengine
    //
    // Therefore it is intentionally NOT included.

    includeMedia3Module(
        "datasource-rtmp",
        "datasource_rtmp"
    )

    includeMedia3Module(
        "datasource-okhttp",
        "datasource_okhttp"
    )

    // --------------------------------------------------------
    // Decoders
    // --------------------------------------------------------

    includeMedia3Module(
        "decoder",
        "decoder"
    )

    includeMedia3Module(
        "decoder-av1",
        "decoder_av1"
    )

    // IMPORTANT:
    // This is the locally-built Media3 FFmpeg decoder.
    includeMedia3Module(
        "decoder-ffmpeg",
        "decoder_ffmpeg"
    )

    includeMedia3Module(
        "decoder-flac",
        "decoder_flac"
    )

    includeMedia3Module(
        "decoder-iamf",
        "decoder_iamf"
    )

    includeMedia3Module(
        "decoder-opus",
        "decoder_opus"
    )

    includeMedia3Module(
        "decoder-vp9",
        "decoder_vp
