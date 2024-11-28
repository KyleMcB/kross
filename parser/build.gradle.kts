plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-library`
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))

    // Test engine for your environment
    testImplementation(kotlin("test-junit")) // For JUnit-based test runner/ Use the Kotlin JUnit 5 integration.
    // testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    //
    // // Use the JUnit 5 integration.
    // testImplementation(libs.junit.jupiter.engine)
    //
    // testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
