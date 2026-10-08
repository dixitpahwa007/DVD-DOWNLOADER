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

        maven {
            url = uri("https://artifactory.appodeal.com/appodeal-public/")
        }
    }
}

rootProject.name = "DVD-DOWNLOADER"

include(":app")

