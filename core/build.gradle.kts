// Pure Kotlin/JVM module: domain types and logic with no Android dependency,
// so it can be unit-tested on any machine with a JDK.
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.junit)
}

tasks.test {
    // Seed files live outside the module; tests read them from the repository.
    systemProperty("agentsetu.seedDir", rootProject.file("data/seed").absolutePath)
    inputs.dir(rootProject.file("data/seed"))
}
