// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    id("com.google.gms.google-services") version "4.4.0" apply false
}

buildscript {
    // Repositories are now defined in settings.gradle.kts
}

// Repositories are now defined in settings.gradle.kts

// Optional: Add a task to help with version updates
tasks.register("getUpdates") {
    doLast {
        println("Check for dependency updates using './gradlew dependencyUpdates'")
    }
}