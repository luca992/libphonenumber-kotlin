import com.vanniktech.maven.publish.SonatypeHost

plugins {
    alias(libs.plugins.org.jetbrains.kotlin.multiplatform)
    alias(libs.plugins.com.android.library)
    alias(libs.plugins.io.mockative)
    alias(libs.plugins.com.vanniktech.maven.publish)
    alias(libs.plugins.org.jetbrains.compose)
    alias(libs.plugins.compose.compiler)
}

group = project.property("GROUP") as String
version = project.property("VERSION_NAME") as String

object Targets {
    // limited by moko resources https://github.com/icerockdev/moko-resources/issues/73
    val iosTargets = arrayOf("iosArm64", "iosX64", "iosSimulatorArm64")
    val tvosTargets = emptyArray<String>() // arrayOf("tvosArm64", "tvosX64", "tvosSimulatorArm64")
    val watchosTargets =
        arrayOf<String>(/*"watchosArm32", "watchosArm64", "watchosX64", "watchosSimulatorArm64", "watchosDeviceArm64"*/)
    val macosTargets = arrayOf("macosX64", "macosArm64")
    val darwinTargets = iosTargets + tvosTargets + watchosTargets + macosTargets

    val linuxTargets = emptyArray<String>() // arrayOf("linuxX64", "linuxArm64")
    val mingwTargets = emptyArray<String>() // arrayOf("mingwX64")
    val androidTargets =
        arrayOf<String>(/*"androidNativeArm32", "androidNativeArm64", "androidNativeX86", "androidNativeX64"*/)
    val nativeTargets = linuxTargets + darwinTargets + mingwTargets + androidTargets
}

kotlin {
    androidTarget {
        publishLibraryVariants("release")
    }
    jvm()
    js(IR) {
        browser()
        nodejs()
    }
//    wasm{
//        browser()
//        nodejs()
//        d8()
//    }
    for (target in Targets.nativeTargets) {
        targets.add(presets.getByName(target).createTarget(target))
    }
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
                // need to make a separate module to import the resources because moko resources doesn't commonTest
                // resources yet: https://github.com/icerockdev/moko-resources/issues/193
                implementation(project(":library-test-resources"))
                implementation(kotlin("test"))
            }
        }
        val jvmCommonMain by creating {
            dependsOn(commonMain)
        }
        val jvmCommonTest by creating {
            dependsOn(commonTest)
            dependencies {
                implementation(kotlin("test-junit"))
            }
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
        val androidUnitTest by getting {}
        val nativeMain by creating {
            dependsOn(nonJvmMain)
        }
        val nativeTest by creating {
            dependsOn(nonJvmTest)
        }
        Targets.nativeTargets.forEach { target ->
            getByName("${target}Main") {
                dependsOn(nativeMain)
            }
            getByName("${target}Test") {
                dependsOn(nativeTest)
            }
        }
    }
}

compose.resources {
    publicResClass = true
}

android {
    namespace = "io.michaelrocks.libphonenumber.kotlin"
    compileSdk = 34

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

// https://youtrack.jetbrains.com/issue/KT-46466
val dependsOnTasks = mutableListOf<String>()
tasks.withType<AbstractPublishToMaven>().configureEach {
    dependsOnTasks.add(this.name.replace("publish", "sign").replaceAfter("Publication", ""))
    dependsOn(dependsOnTasks)
}

plugins.withId("com.vanniktech.maven.publish") {
    mavenPublishing {
        publishToMavenCentral(SonatypeHost.S01)
        signAllPublications()
    }
}

apply(from = "$rootDir/gradle/pack-library-test-resources.gradle.kts")
