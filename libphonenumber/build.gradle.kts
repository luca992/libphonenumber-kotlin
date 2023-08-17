import com.vanniktech.maven.publish.SonatypeHost

plugins {
    alias(libs.plugins.org.jetbrains.kotlin.multiplatform)
    alias(libs.plugins.com.android.library)
    alias(libs.plugins.dev.icerock.mobile.multiplatform.resources)
    alias(libs.plugins.org.kodein.mock.mockmp)
    alias(libs.plugins.com.vanniktech.maven.publish)
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
    androidTarget()
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
        }
        val commonMain by getting {
            dependencies {
                implementation(libs.com.squareup.okio)
                implementation(libs.co.touchlab.kermit)
                implementation(libs.dev.icerock.moko.resources)
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
        }
        val jvmMain by getting {
            dependsOn(jvmCommonMain)
        }
        val jvmTest by getting {
            dependsOn(jvmCommonTest)
        }
        val androidMain by getting {
            dependsOn(jvmCommonMain)
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
        val androidUnitTest by getting {
            dependsOn(jvmCommonTest)
        }
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

android {
    namespace = "io.michaelrocks.libphonenumber.android"
    compileSdk = 33

    defaultConfig {
        minSdk = 19

//    versionCode = 1
//    versionName = version
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    sourceSets {
        named("test") {
            java.srcDirs("src/androidUnitTest/java")
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

    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
    }

    // https://kotlinlang.org/docs/gradle-configure-project.html#gradle-java-toolchains-support
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

multiplatformResources {
    multiplatformResourcesPackage = "io.michaelrocks.libphonenumber"
}

mockmp {
    usesHelper = true
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

// not sure why only native has a gradle dependency order issue when publishing
// possibly related to https://github.com/icerockdev/moko-resources/issues/535
tasks.getByName("iosX64SourcesJar").dependsOn("generateMRiosX64Main")
tasks.getByName("iosArm64SourcesJar").dependsOn("generateMRiosArm64Main")
tasks.getByName("iosSimulatorArm64SourcesJar").dependsOn("generateMRiosSimulatorArm64Main")
tasks.getByName("macosArm64SourcesJar").dependsOn("generateMRmacosArm64Main")
tasks.getByName("macosX64SourcesJar").dependsOn("generateMRmacosX64Main")
