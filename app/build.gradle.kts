plugins {
id("com.android.application")
id("org.jetbrains.kotlin.android")
}

android {
namespace = "com.dixit.video.downloader"
compileSdk = 36

defaultConfig {
    applicationId = "com.dixit.video.downloader"
    minSdk = 26
    targetSdk = 36
    versionCode = 12
    versionName = "6.2.0"
}

buildTypes {
    debug {
        applicationIdSuffix = ".debug"
        versionNameSuffix = "-debug"
    }

    release {
        isMinifyEnabled = false
    }
}

compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlinOptions {
    jvmTarget = "17"
}

}

dependencies {
implementation("androidx.core:core-ktx:1.15.0")
implementation("androidx.appcompat:appcompat:1.7.0")
implementation("com.google.android.material:material:1.12.0")
implementation("androidx.activity:activity-ktx:1.10.0")
implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
implementation("androidx.recyclerview:recyclerview:1.3.2")
implementation("androidx.documentfile:documentfile:1.0.1")

implementation("androidx.media3:media3-exoplayer:1.5.1")
implementation("androidx.media3:media3-ui:1.5.1")
implementation("androidx.media3:media3-common:1.5.1")
implementation("androidx.media3:media3-session:1.5.1")
implementation("androidx.media3:media3-transformer:1.5.1")
implementation("androidx.media3:media3-effect:1.5.1")
implementation("androidx.media3:media3-exoplayer-hls:1.5.1")
implementation("androidx.media3:media3-exoplayer-dash:1.5.1")

implementation(project(":media3-lib-decoder-ffmpeg"))

// MIT-licensed Android MP3 encoder used for true local MP3 conversion.
implementation("io.auxo.ame:ame-lite:0.1")

}
