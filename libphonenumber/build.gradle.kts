@file:OptIn(ExperimentalWasmDsl::class)

import com.vanniktech.maven.publish.SonatypeHost
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSetTree

plugins {
    alias(libs.plugins.org.jetbrains.kotlin.multiplatform)
    alias(libs.plugins.com.android.library)
    alias(libs.plugins.dev.mokkery)
    alias(libs.plugins.com.vanniktech.maven.publish)
    alias(libs.plugins.org.jetbrains.compose)
    alias(libs.plugins.compose.compiler)
}

group = project.property("GROUP") as String
version = project.property("VERSION_NAME") as String

kotlin {
    androidTarget {
        publishLibraryVariants("release")
        instrumentedTestVariant.sourceSetTree.set(KotlinSourceSetTree.test)
    }
    jvm()
    js(IR) {
        browser()
        nodejs()
    }
    wasmJs {
        browser()
    }
    iosX64();iosArm64();iosSimulatorArm64()
    macosX64();macosArm64()
    applyDefaultHierarchyTemplate()
    sourceSets {
        all {
            languageSettings.optIn("kotlin.ExperimentalStdlibApi")
            languageSettings.optIn("org.jetbrains.compose.resources.ExperimentalResourceApi")
        }
        val commonMain by getting {
            dependencies {
                implementation(libs.com.squareup.okio)
                implementation(libs.co.touchlab.kermit)
                implementation(compose.runtime)
                implementation(compose.components.resources)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(compose.components.resources)
                implementation(kotlin("test"))
            }
        }
        val jvmCommonMain by creating {
            dependsOn(commonMain)
        }
        val jvmCommonTest by creating {
            dependsOn(commonTest)
        }
        val jvmMain by getting {
            dependsOn(jvmCommonMain)
        }
        val jvmTest by getting {
            dependsOn(jvmCommonTest)
        }
        val androidMain by getting {
            dependsOn(jvmCommonMain)
            dependencies {
                implementation(libs.androidx.startup.runtime)
            }
        }
        val nonJvmMain by creating {
            dependsOn(commonMain)
        }
        val nonJvmTest by creating {
            dependsOn(commonTest)
        }
        val jsMain by getting {
            dependsOn(nonJvmMain)
        }
        val androidInstrumentedTest by getting {
            dependsOn(jvmCommonTest)
            dependencies {
                implementation(libs.androidx.runner)
            }
        }
        val nativeMain by getting {
            dependsOn(nonJvmMain)
        }
        val wasmJsMain by getting {
            dependsOn(nonJvmMain)
            dependencies {
                implementation(libs.kotlinx.browser)
            }
        }
        val wasmJsTest by getting {
            dependsOn(nonJvmTest)
        }
    }
}

compose.resources {
    publicResClass = true
    generateResClass = always
}

android {
    namespace = "io.michaelrocks.libphonenumber.kotlin"
    compileSdk = 35

    defaultConfig {
        minSdk = 23
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

//    androidComponents.beforeVariants { variantBuilder ->
//        if (variantBuilder.name != "release") {
//            variantBuilder.enable = false
//        }
//    }

    lint {
        abortOnError = false
    }

    // https://kotlinlang.org/docs/gradle-configure-project.html#gradle-java-toolchains-support
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

plugins.withId("com.vanniktech.maven.publish") {
    mavenPublishing {
        publishToMavenCentral(SonatypeHost.S01)
        signAllPublications()
    }
}