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
// AndroidX Media3 1.5.1 - local source build
// ============================================================

val mediaDir = file("media")

if (mediaDir.exists()) {

    // Required by Media3 1.5.1 build scripts.
    gradle.ext["androidxMediaSettingsDir"] = mediaDir.canonicalPath
    gradle.ext["androidxMediaModulePrefix"] = "media3-"

    // Include a Media3 module only if its directory exists.
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

    // ========================================================
    // Core
    // ========================================================

    includeMedia3Module("common", "common")
    includeMedia3Module("common-ktx", "common_ktx")
    includeMedia3Module("container", "container")

    // ========================================================
    // Session / ExoPlayer
    // ========================================================

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

    // ========================================================
    // UI
    // ========================================================

    includeMedia3Module("ui", "ui")
    includeMedia3Module("ui-leanback", "ui_leanback")

    // ========================================================
    // DataSource
    // ========================================================

    includeMedia3Module("database", "database")
    includeMedia3Module("datasource", "datasource")
    includeMedia3Module(
        "datasource-cronet",
        "datasource_cronet"
    )

    // datasource_httpengine is intentionally not included.
    // It is not present in the Media3 1.5.1 checkout.

    includeMedia3Module(
        "datasource-rtmp",
        "datasource_rtmp"
    )
    includeMedia3Module(
        "datasource-okhttp",
        "datasource_okhttp"
    )

    // ========================================================
    // Decoders
    // ========================================================

    includeMedia3Module("decoder", "decoder")
    includeMedia3Module(
        "decoder-av1",
        "decoder_av1"
    )

    // IMPORTANT:
    // This is the locally built FFmpeg decoder.
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
        "decoder_vp9"
    )

    // ========================================================
    // Extractor
    // ========================================================

    includeMedia3Module(
        "extractor",
        "extractor"
    )

    // ========================================================
    // Cast
    // ========================================================

    includeMedia3Module(
        "cast",
        "cast"
    )

    // ========================================================
    // Effects / Muxer / Transformer
    // ========================================================

    includeMedia3Module(
        "effect",
        "effect"
    )
    includeMedia3Module(
        "muxer",
        "muxer"
    )
    includeMedia3Module(
        "transformer",
        "transformer"
    )

    // ========================================================
    // Test utilities
    // ========================================================

    includeMedia3Module(
        "test-utils-robolectric",
        "test_utils_robolectric"
    )
    includeMedia3Module(
        "test-data",
        "test_data"
    )
    includeMedia3Module(
        "test-utils",
        "test_utils"
    )
}
