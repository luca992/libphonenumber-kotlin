import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.compose.experimental.dsl.IOSDevices

plugins {
    alias(libs.plugins.org.jetbrains.kotlin.multiplatform)
    alias(libs.plugins.org.jetbrains.compose)
    alias(libs.plugins.com.android.application)
}

version = "1.0-SNAPSHOT"

kotlin {
    androidTarget()
    jvm("desktop")
    js(IR) {
        browser()
        binaries.executable()
    }
    macosX64 {
        binaries {
            executable {
                entryPoint = "main"
                freeCompilerArgs += listOf(
                    "-linker-option", "-framework", "-linker-option", "Metal"
                )
            }
        }
    }
    macosArm64 {
        binaries {
            executable {
                entryPoint = "main"
                freeCompilerArgs += listOf(
                    "-linker-option", "-framework", "-linker-option", "Metal"
                )
            }
        }
    }
    iosX64("uikitX64") {
        binaries {
            executable {
                entryPoint = "main"
                freeCompilerArgs += listOf(
                    "-linker-option", "-framework", "-linker-option", "Metal",
                    "-linker-option", "-framework", "-linker-option", "CoreText",
                    "-linker-option", "-framework", "-linker-option", "CoreGraphics"
                )
            }
        }
    }
    iosArm64("uikitArm64") {
        binaries {
            executable {
                entryPoint = "main"
                freeCompilerArgs += listOf(
                    "-linker-option", "-framework", "-linker-option", "Metal",
                    "-linker-option", "-framework", "-linker-option", "CoreText",
                    "-linker-option", "-framework", "-linker-option", "CoreGraphics"
                )
            }
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.ui)
                implementation(compose.foundation)
                implementation(compose.material)
                implementation(compose.materialIconsExtended)
                implementation(compose.runtime)
                implementation(compose.animation)
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }

        val androidMain by getting {
            dependsOn(commonMain)
        }

        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
            }
        }

        val jsMain by getting {
            dependsOn(commonMain)
            dependencies {
            }
        }

        val nativeMain by creating {
            dependsOn(commonMain)
        }
        val macosMain by creating {
            dependsOn(nativeMain)
        }
        val macosX64Main by getting {
            dependsOn(macosMain)
        }
        val macosArm64Main by getting {
            dependsOn(macosMain)
        }
        val uikitMain by creating {
            dependsOn(nativeMain)
        }
        val uikitX64Main by getting {
            dependsOn(uikitMain)
        }
        val uikitArm64Main by getting {
            dependsOn(uikitMain)
        }
    }
}

compose.desktop {
    application {
        mainClass = "MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "compose-sample"
            packageVersion = "1.0.0"
        }
    }
}


compose.experimental {
    web.application {}
    uikit.application {
        bundleIdPrefix = "io.eqoty.secretk"
        projectName = "Eqoty Admin Console"
        deployConfigurations {
            simulator("IPhone8") {
                //Usage: ./gradlew iosDeployIPhone8Debug
                device = IOSDevices.IPHONE_8
            }
            simulator("IPad") {
                //Usage: ./gradlew iosDeployIPadDebug
                device = IOSDevices.IPAD_MINI_6th_Gen
            }
            connectedDevice("Device") {
                //First need specify your teamId here, or in local.properties (compose.ios.teamId=***)
                //teamId="***"
                //Usage: ./gradlew iosDeployDeviceRelease
            }
        }
    }
}



compose.desktop.nativeApplication {
    targets(kotlin.targets.getByName("macosArm64"))
    distributions {
        targetFormats(TargetFormat.Dmg)
        packageName = "Eqoty Admin Console"
        packageVersion = "1.0.0"
    }
}


android {
    compileSdk = 33

    defaultConfig {
        minSdk = 24
        targetSdk = 33
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    sourceSets {
        named("main") {
            manifest.srcFile("src/androidMain/AndroidManifest.xml")
            res.srcDirs("src/androidMain/res", "src/commonMain/resources")
        }
    }
    namespace = "io.eqoty.secretk.sample"
}
