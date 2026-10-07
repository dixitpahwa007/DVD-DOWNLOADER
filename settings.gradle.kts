import androidx.media3.buildlogic.includeMedia3

pluginManagement {
    includeBuild("media/build-logic-settings")

    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("gradlebuild.media3-settings-logic")
}

includeMedia3(file("media"))
