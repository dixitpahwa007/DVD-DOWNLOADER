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
        maven {
            url = uri("https://jcenter.bintray.com/")
        }
    }
}

rootProject.name = "DVD-DOWNLOADER"

include(":app")

// Use the locally checked-out AndroidX Media3 1.5.1 source.
// This is required because media3-decoder-ffmpeg is not published
// as a normal Maven artifact.
val mediaDir = file("media")

if (mediaDir.exists()) {
    (gradle as ExtensionAware).extra["androidxMediaModulePrefix"] = "media3-"
    apply(from = file("media/core_settings.gradle"))
}
