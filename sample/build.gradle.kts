import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.compose.desktop.application.tasks.AbstractNativeMacApplicationPackageAppDirTask
import org.jetbrains.kotlin.gradle.plugin.mpp.AbstractExecutable
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBinary
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeLink
import org.jetbrains.kotlin.library.impl.KotlinLibraryLayoutImpl
import java.io.File
import java.io.FileFilter
import org.jetbrains.kotlin.konan.file.File as KonanFile

plugins {
    alias(libs.plugins.org.jetbrains.kotlin.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.org.jetbrains.compose)
    alias(libs.plugins.com.android.application)
    alias(libs.plugins.dev.icerock.mobile.multiplatform.resources)
    kotlin("native.cocoapods")
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
    iosX64() {
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
    iosArm64() {
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
    iosSimulatorArm64() {
        // TODO: remove after 1.5 release
        binaries.forEach {
            it.freeCompilerArgs += listOf(
                "-linker-option", "-framework", "-linker-option", "Metal",
                "-linker-option", "-framework", "-linker-option", "CoreText",
                "-linker-option", "-framework", "-linker-option", "CoreGraphics",
            )
        }
    }

    cocoapods {
        summary = "Shared code for the sample"
        homepage = "https://github.com/luca992/libphonenumber-kotlin"
        ios.deploymentTarget = "14.1"
        podfile = project.file("../sampleIosApp/Podfile")
        framework {
            baseName = "shared"
            isStatic = true
        }
        extraSpecAttributes["resources"] = "['src/commonMain/resources/**', 'src/iosMain/resources/**']"
    }


    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.ui)
                implementation(compose.foundation)
                implementation(compose.material)
                implementation(compose.runtime)
                implementation(libs.dev.icerock.moko.resources)
                api(project(":libphonenumber"))
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }

        val androidMain by getting {
            dependsOn(commonMain)
            dependencies {
                implementation(libs.androidx.appcompat)
                implementation(libs.androidx.activity.compose)
            }
        }

        val desktopMain by getting {
            dependsOn(commonMain)
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
        val iosMain by creating {
            dependsOn(nativeMain)
        }
        val iosSimulatorArm64Main by getting {
            dependsOn(iosMain)
        }
        val iosX64Main by getting {
            dependsOn(iosMain)
        }
        val iosArm64Main by getting {
            dependsOn(iosMain)
        }
    }
}

compose.desktop {
    application {
        mainClass = "MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "libphonenumber sample"
            packageVersion = "1.0.0"
        }
    }
}


compose.experimental {
    web.application {}
}



compose.desktop.nativeApplication {
    targets(kotlin.targets.getByName("macosArm64"))
    distributions {
        targetFormats(TargetFormat.Dmg)
        packageName = "libphonenumber sample"
        packageVersion = "1.0.0"
    }
}


android {
    namespace = "io.luca992.libphonenumber.sample"
    compileSdk = 34

    defaultConfig {
        minSdk = 24
        targetSdk = 34
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
    // https://kotlinlang.org/docs/gradle-configure-project.html#gradle-java-toolchains-support
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

multiplatformResources {
    resourcesPackage = "io.michaelrocks.libphonenumber.sample"
}

//// todo: Remove when resolved: https://github.com/icerockdev/moko-resources/issues/372
//tasks.withType<KotlinNativeLink>()
//    .matching { linkTask -> linkTask.binary is AbstractExecutable }
//    .configureEach {
//        val task: KotlinNativeLink = this
//
//        doLast {
//            val binary: NativeBinary = task.binary
//            val outputDir: File = task.outputFile.get().parentFile
//            task.libraries
//                .filter { library -> library.extension == "klib" }
//                .filter(File::exists)
//                .forEach { inputFile ->
//                    val klibKonan = KonanFile(inputFile.path)
//                    val klib = KotlinLibraryLayoutImpl(
//                        klib = klibKonan,
//                        component = "default"
//                    )
//                    val layout = klib.extractingToTemp
//
//                    // extracting bundles
//                    layout
//                        .resourcesDir
//                        .absolutePath
//                        .let(::File)
//                        .listFiles(FileFilter { it.extension == "bundle" })
//                        // copying bundles to app
//                        ?.forEach { bundleFile ->
//                            logger.info("${bundleFile.absolutePath} copying to $outputDir")
//                            bundleFile.copyRecursively(
//                                target = File(outputDir, bundleFile.name),
//                                overwrite = true
//                            )
//                        }
//                }
//        }
//    }
//
//tasks.withType<AbstractNativeMacApplicationPackageAppDirTask> {
//    val task: AbstractNativeMacApplicationPackageAppDirTask = this
//
//    doLast {
//        val execFile: File = task.executable.get().asFile
//        val execDir: File = execFile.parentFile
//        val destDir: File = task.destinationDir.asFile.get()
//        val bundleID: String = task.bundleID.get()
//
//        val outputDir = File(destDir, "$bundleID.app/Contents/Resources")
//        outputDir.mkdirs()
//
//        execDir.listFiles().orEmpty()
//            .filter { it.extension == "bundle" }
//            .forEach { bundleFile ->
//                logger.info("${bundleFile.absolutePath} copying to $outputDir")
//                bundleFile.copyRecursively(
//                    target = File(outputDir, bundleFile.name),
//                    overwrite = true
//                )
//            }
//    }
//}

apply(from = "$rootDir/gradle/pack-library-resources.gradle.kts")
