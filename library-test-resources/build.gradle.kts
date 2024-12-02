plugins {
    alias(libs.plugins.org.jetbrains.kotlin.multiplatform)
    alias(libs.plugins.org.jetbrains.compose)
    alias(libs.plugins.compose.compiler)
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
                implementation(compose.runtime)
                implementation(compose.components.resources)
            }
        }
    }
}
