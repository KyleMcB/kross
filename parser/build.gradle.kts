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
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    implementation(project(":shell-state"))
    testImplementation(kotlin("test"))
    implementation(project(":executable"))

    implementation(libs.kotlinx.serialization.json)

    // Test engine for your environment
    testImplementation(kotlin("test-junit")) // For JUnit-based test runner/ Use the Kotlin JUnit 5 integration.
    // testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    //
    // // Use the JUnit 5 integration.
    // testImplementation(libs.junit.jupiter.engine)
    //
    // testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
tasks.test {
    testLogging {
        // Show standard output and error streams
        showStandardStreams = true

        // Optionally, customize log levels
        events("passed", "failed", "skipped") // Configure which test events to log
    }
}
