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
// AndroidX Media3 1.5.1 - Local Source Build
// ============================================================
//
// Media3 1.5.1 uses core_settings.gradle for local builds.
// We reproduce its library includes here because the 1.5.1
// source tree references datasource_httpengine, while that
// directory is not present in our checkout.
//
// IMPORTANT:
// media3-decoder-ffmpeg is included locally so that the
// FFmpeg decoder built by the GitHub Actions workflow is used.
// ============================================================

val mediaDir = file("media")

if (mediaDir.exists()) {

    // Media3 expects this property when its modules are used
    // from another Gradle build.
    gradle.ext["androidxMediaSettingsDir"] =
        mediaDir.canonicalPath

    // Prefix used by Media3's local-module setup.
    val mediaPrefix = ":media3-"

    // --------------------------------------------------------
    // Common
    // --------------------------------------------------------

    include("${mediaPrefix}lib-common")
    project("${mediaPrefix}lib-common").projectDir =
        File(mediaDir, "libraries/common")

    include("${mediaPrefix}lib-common-ktx")
    project("${mediaPrefix}lib-common-ktx").projectDir =
        File(mediaDir, "libraries/common_ktx")

    include("${mediaPrefix}lib-container")
    project("${mediaPrefix}lib-container").projectDir =
        File(mediaDir, "libraries/container")

    // --------------------------------------------------------
    // Session / ExoPlayer
    // --------------------------------------------------------

    include("${mediaPrefix}lib-session")
    project("${mediaPrefix}lib-session").projectDir =
        File(mediaDir, "libraries/session")

    include("${mediaPrefix}lib-exoplayer")
    project("${mediaPrefix}lib-exoplayer").projectDir =
        File(mediaDir, "libraries/exoplayer")

    include("${mediaPrefix}lib-exoplayer-dash")
    project("${mediaPrefix}lib-exoplayer-dash").projectDir =
        File(mediaDir, "libraries/exoplayer_dash")

    include("${mediaPrefix}lib-exoplayer-hls")
    project("${mediaPrefix}lib-exoplayer-hls").projectDir =
        File(mediaDir, "libraries/exoplayer_hls")

    include("${mediaPrefix}lib-exoplayer-rtsp")
    project("${mediaPrefix}lib-exoplayer-rtsp").projectDir =
        File(mediaDir, "libraries/exoplayer_rtsp")

    include("${mediaPrefix}lib-exoplayer-smoothstreaming")
    project("${mediaPrefix}lib-exoplayer-smoothstreaming").projectDir =
        File(mediaDir, "libraries/exoplayer_smoothstreaming")

    include("${mediaPrefix}lib-exoplayer-ima")
    project("${mediaPrefix}lib-exoplayer-ima").projectDir =
        File(mediaDir, "libraries/exoplayer_ima")

    include("${mediaPrefix}lib-exoplayer-workmanager")
    project("${mediaPrefix}lib-exoplayer-workmanager").projectDir =
        File(mediaDir, "libraries/exoplayer_workmanager")

    // --------------------------------------------------------
    // UI
    // --------------------------------------------------------

    include("${mediaPrefix}lib-ui")
    project("${mediaPrefix}lib-ui").projectDir =
        File(mediaDir, "libraries/ui")

    include("${mediaPrefix}lib-ui-leanback")
    project("${mediaPrefix}lib-ui-leanback").projectDir =
        File(mediaDir, "libraries/ui_leanback")

    // --------------------------------------------------------
    // DataSource
    // --------------------------------------------------------

    include("${mediaPrefix}lib-database")
    project("${mediaPrefix}lib-database").projectDir =
        File(mediaDir, "libraries/database")

    include("${mediaPrefix}lib-datasource")
    project("${mediaPrefix}lib-datasource").projectDir =
        File(mediaDir, "libraries/datasource")

    include("${mediaPrefix}lib-datasource-cronet")
    project("${mediaPrefix}lib-datasource-cronet").projectDir =
        File(mediaDir, "libraries/datasource_cronet")

    // NOTE:
    // datasource_httpengine is intentionally NOT included.
    //
    // Media3 1.5.1 core_settings.gradle references it, but
    // the directory does not exist in our checkout.

    include("${mediaPrefix}lib-datasource-rtmp")
    project("${mediaPrefix}lib-datasource-rtmp").projectDir =
