rootProject.name = "libphonenumber-kotlin"

pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
    }
    plugins {
        // See https://jmfayard.github.io/refreshVersions
        id("de.fayard.refreshVersions") version "0.51.0"
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        google()
    }
}

include(":library")
include(":sample")
