plugins {
    alias(libs.plugins.org.jetbrains.kotlin.multiplatform)
    alias(libs.plugins.dev.icerock.mobile.multiplatform.resources)
}


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
                implementation(libs.dev.icerock.moko.resources)
            }
        }
        val jvmMain by getting {
            dependsOn(commonMain)
        }
    }
}

multiplatformResources {
    multiplatformResourcesPackage = "io.michaelrocks.libphonenumber.test"
}
