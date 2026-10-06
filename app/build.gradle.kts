import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val releaseSigning = Properties().apply {
    rootProject.file("keystore.properties").takeIf { it.isFile }?.inputStream()?.use(::load)
}
val localConfig = Properties().apply {
    rootProject.file("local.properties").takeIf { it.isFile }?.inputStream()?.use(::load)
}
val lastFmApiKey = providers.environmentVariable("LASTFM_API_KEY").orNull
    ?: providers.gradleProperty("LASTFM_API_KEY").orNull
    ?: localConfig.getProperty("LASTFM_API_KEY", "")
val escapedLastFmApiKey = lastFmApiKey.replace("\\", "\\\\").replace("\"", "\\\"")
fun configValue(name: String): String = (
    providers.environmentVariable(name).orNull
        ?: providers.gradleProperty(name).orNull
        ?: localConfig.getProperty(name, "")
    ).replace("\\", "\\\\").replace("\"", "\\\"")

android {
    namespace = "com.vibearc.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.vibearc.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 18
        versionName = "1.4.0"
        buildConfigField("String", "LASTFM_API_KEY", "\"$escapedLastFmApiKey\"")
        buildConfigField("String", "LASTFM_SIGNER_URL", "\"${configValue("LASTFM_SIGNER_URL")}\"")
        buildConfigField("String", "LASTFM_SIGNER_TOKEN", "\"${configValue("LASTFM_SIGNER_TOKEN")}\"")
    }

    signingConfigs {
        providers.gradleProperty("testKeystore").orNull?.let { path ->
            getByName("debug").storeFile = rootProject.file(path)
        }
        if (releaseSigning.isNotEmpty()) create("release") {
            storeFile = rootProject.file(releaseSigning.getProperty("storeFile"))
            storePassword = releaseSigning.getProperty("storePassword")
            keyAlias = releaseSigning.getProperty("keyAlias")
            keyPassword = releaseSigning.getProperty("keyPassword")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
        create("downloadTest") {
            initWith(getByName("release"))
            applicationIdSuffix = ".downloadtest"
            versionNameSuffix = "-fallback"
            matchingFallbacks += "release"
        }
        create("performance") {
            initWith(getByName("release"))
            isDebuggable = false
            isMinifyEnabled = true
            // The Windows sandbox denies ZipFS access in the optional resource shrinker.
            // Keep code optimization; retaining unused resources only affects APK size.
            isShrinkResources = false
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += "release"
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs_nio:2.1.5")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.media3:media3-exoplayer:1.2.1")
    implementation("androidx.media3:media3-session:1.2.1")
    implementation("com.github.teamnewpipe:NewPipeExtractor:v0.26.5")
    implementation("com.github.TeamNewPipe:nanojson:e9d656ddb49a412a5a0a5d5ef20ca7ef09549996")

    testImplementation("junit:junit:4.13.2")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
