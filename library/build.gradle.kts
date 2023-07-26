plugins {
    alias(libs.plugins.org.jetbrains.kotlin.multiplatform)
    alias(libs.plugins.com.android.library)
    id("maven-publish")
    id("signing")
}

val artifactName = rootProject.name
group = rootProject.group
version = rootProject.version

object Targets {
    val iosTargets = arrayOf("iosArm64", "iosX64", "iosSimulatorArm64")
    val tvosTargets = arrayOf("tvosArm64", "tvosX64", "tvosSimulatorArm64")
    val watchosTargets = arrayOf(
        "watchosArm32", "watchosArm64", "watchosX64", "watchosSimulatorArm64", "watchosDeviceArm64"
    )
    val macosTargets = arrayOf("macosX64", "macosArm64")
    val darwinTargets = iosTargets + tvosTargets + watchosTargets + macosTargets
    val linuxTargets = arrayOf("linuxX64", "linuxArm64")
    val mingwTargets = arrayOf("mingwX64")
    val androidTargets = arrayOf(
        "androidNativeArm32", "androidNativeArm64", "androidNativeX86", "androidNativeX64",
    )
    val nativeTargets = linuxTargets + darwinTargets + mingwTargets
}

kotlin {
    androidTarget()
    jvm()
    js(IR){
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
            }
        }
        val commonTest by getting
        val jvmMain by getting {
            dependsOn(commonMain)
        }
        val androidMain by getting {
            dependsOn(jvmMain)
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
            dependsOn(commonTest)
            dependencies {
                implementation(libs.junit)
                implementation(libs.mockito.core)
            }
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
        named("main") {
            java.srcDirs("src/androidMain/java")
        }
        named("test") {
            java.srcDirs("src/androidUnitTest/java")
        }
    }

//    variantFilter { variant ->
//        if (variant.buildType.name != "release") {
//            variant.setIgnore(true)
//        }
//    }

    lint {
        abortOnError = false
    }
//
//  publishing {
//    singleVariant("release") {
//      withSourcesJar()
//      withJavadocJar()
//    }
//  }

    // https://kotlinlang.org/docs/gradle-configure-project.html#gradle-java-toolchains-support
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

/*
afterEvaluate {
  publishing {
    publications {
      release(MavenPublication) {
        from components.release
        artifactId artifactName
        pom {
          name = 'libphonenumber-android'
          description = 'An Android port of Google\'s libphonenumber.'
          inceptionYear = '2016'
          url = 'https://github.com/michaelrocks/libphonenumber-android'
          packaging = 'aar'

          licenses {
            license {
              name = 'The Apache License, Version 2.0'
              url = 'http://www.apache.org/licenses/LICENSE-2.0.txt'
              distribution = 'repo'
            }
          }
          developers {
            developer {
              id = 'MichaelRocks'
              name = 'Michael Rozumyanskiy'
              email = 'michael.rozumyanskiy@gmail.com'
            }
          }
          scm {
            connection = 'scm:git:git://github.com/michaelrocks/libphonenumber-android.git'
            developerConnection = 'scm:git:ssh://git@github.com/michaelrocks/libphonenumber-android.git'
            url = 'https://github.com/michaelrocks/libphonenumber-android'
          }
        }
      }
    }

    repositories {
      if (project.hasProperty('mavenCentralRepositoryUsername') && project.hasProperty('mavenCentralRepositoryPassword')) {
        maven {
          name 'Sonatype'
          url 'https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/'
          credentials {
            username mavenCentralRepositoryUsername
            password mavenCentralRepositoryPassword
          }
        }
      }
    }
  }

  signing {
    sign publishing.publications.release
  }
}
*/
