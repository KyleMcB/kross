plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    `java-library`
}

repositories {
    mavenCentral()
}
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(8)
    }
}
dependencies {
    implementation("com.soywiz.korlibs.luak:luak:3.4.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    implementation(project(":shell-state"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    testImplementation(kotlin("test"))

    implementation(libs.kotlinx.serialization.json)

    // Test engine for your environment
    testImplementation(kotlin("test-junit")) // For JUnit-based test runner/ Use the Kotlin JUnit 5 integration.
}
tasks.test {
    testLogging {
        // Show standard output and error streams
        showStandardStreams = true

        // Optionally, customize log levels
        events("passed", "failed", "skipped") // Configure which test events to log
    }
}