import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

// Release signing is read from keystore.properties (git-ignored). Without it, release builds are unsigned.
val keystoreProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

android {
    namespace = "app.agentsetu"
    compileSdk = 35

    defaultConfig {
        // Final package name (decided 29-09-2026). Never change it after the first release:
        // Android treats a different package name as a different app, and users would lose data.
        applicationId = "in.agentsetu.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 5
        versionName = "1.1.1"

        // Only ARM phones: every Android 8+ phone this app targets is ARM. Leaving out the x86
        // copies of the encryption library (for emulators) saves about 10 MB.
        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a")
        }

        // Where the app reads release/version.json for the update check. Override with
        // -Pagentsetu.updateUrl=... (e.g. a GitHub Pages address if this repository is private).
        val updateUrl = (project.findProperty("agentsetu.updateUrl") as String?)
            ?: "https://raw.githubusercontent.com/rahulranjan-dev-py/Agent-Setu/main/release/version.json"
        buildConfigField("String", "UPDATE_URL", "\"$updateUrl\"")
    }

    signingConfigs {
        // Fixed, PUBLIC test key (committed on purpose) so every test build from GitHub can be
        // installed over the previous one. It signs only the ".debug" test app; it can never sign
        // or update the real app, which uses the owner's private release key.
        getByName("debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
        if (keystoreProps.isNotEmpty()) {
            create("release") {
                storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfigs.findByName("release")?.let { signingConfig = it }
        }
    }

    // Bundled sample rate tables come straight from data/seed, the single source of truth.
    sourceSets["main"].assets.srcDir(rootProject.file("data/seed"))

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    // Store native libraries compressed inside the APK (about 5 MB smaller to download and share on
    // WhatsApp); Android unpacks them once at install time.
    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(project(":core"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.biometric)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.sqlite)
    implementation(libs.sqlcipher.android)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
}
