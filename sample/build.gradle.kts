plugins {
    kotlin("android")
    id("com.android.application")
}

android {
    namespace = "io.michaelrocks.libphonenumber.android.sample"
    compileSdk = 33

    defaultConfig {
        minSdk = 14
        targetSdk = 33

        applicationId = "io.michaelrocks.libphonenumber.android.sample"

        versionCode = 1
        versionName = "8.13.17"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        getByName("debug") {
//            matchingFallbacks = listOf("release")
        }

        getByName("release") {
            isMinifyEnabled = true
        }
    }

    lint {
        abortOnError = false
    }
}

dependencies {
    implementation(project(":library"))
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.androidx.rules)
    androidTestImplementation(libs.androidx.runner)
    androidTestImplementation(libs.androidx.junit)
}
