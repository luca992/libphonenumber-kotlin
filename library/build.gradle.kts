plugins {
    alias(libs.plugins.org.jetbrains.kotlin.multiplatform)
    alias(libs.plugins.com.android.library)
    id("maven-publish")
    id("signing")
}

val artifactName = rootProject.name
group = rootProject.group
version = rootProject.version

kotlin {
    android()
    jvm()
    sourceSets {
        val commonMain by getting
        val commonTest by getting
        val jvmMain by getting {
            dependsOn(commonMain)
        }
        val androidMain by getting {
            dependsOn(commonMain)
        }
        val androidUnitTest by getting {
            dependsOn(commonTest)
            dependencies {
                implementation(libs.junit)
                implementation(libs.mockito.core)
            }
        }
    }
}

android {
    namespace = "io.michaelrocks.libphonenumber.android"
    compileSdk = 33

    defaultConfig {
        minSdk = 14

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
