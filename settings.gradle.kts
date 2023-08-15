rootProject.name = "libphonenumber-kotlin"

pluginManagement {
    repositories {
        mavenCentral()
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
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}

plugins {
    id("de.fayard.refreshVersions")
}

include(":library")
include(":library-test-resources")
include(":sample")
