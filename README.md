[![Build](https://github.com/luca992/libphonenumber-kotlin/actions/workflows/build.yml/badge.svg)](https://github.com/luca992/libphonenumber-kotlin/actions/workflows/build.yml)
[![Android Arsenal](https://img.shields.io/badge/Android%20Arsenal-libphonenumber--kotlin-brightgreen.svg?style=flat)](http://android-arsenal.com/details/1/3676)

libphonenumber-kotlin
======================
Kotlin Multiplatform port of [MichaelRocks/libphonenumber-android](https://github.com/MichaelRocks/libphonenumber-android)

Why?
----
Ported to Kotlin Multiplatform to be used in common code for Android and iOS, macOS, and web!

Check out the Compose Multiplatform [sample](sample) 


https://github.com/luca992/libphonenumber-kotlin/assets/4157455/e3fc61a3-e2bb-4993-a6c5-c04f713dcd54


### Note: 
While the sample apps run on all targets, **_this project is experimental_** and is a work in progress. Only the JVM target has all tests passing, native and js have failing tests that need to be addressed.


#### Any help/PRs are appreciated!

TODO: Update the rest of the readme.

___

Google's libphonenumber is a great library but it has to major flaws when used on Android:
 1. ~~It adds about [7k methods][2] to a final dex.~~ Not anymore, since 7.7.0.
 2. Internally the library uses `Class.getResourceAsStream()` method,
 which is [very slow on Android][3].

The goal of this library is to fix these two issues.

Setup
--------
Gradle:
```groovy
repositories {
    mavenCentral()
    // For snapshots
    maven("https://s01.oss.sonatype.org/content/repositories/snapshots/")
}

dependencies {
    implementation("io.github.luca992.libphonenumber-kotlin:libphonenumber:0.1.5")
}
```

For DexGuard users
------------------
If your project is obfuscated with DexGuard you may need to add the following line to the
DexGuard configuration:
```
-keepresourcefiles assets/io/michaelrocks/libphonenumber/android/**
```

API differences
---------------
This library is not fully compatible with the original `libphonenumber`.
 1. Every `libphonenumber` class is repackaged to 
 `io.michaelrocks.libphonenumber.android`.
 2. `PhoneNumberUtil` doesn't contain a `getInstance()` method so you
 have to create an instance of this class with one of 
 `PhoneNumberUtil.createInstance()` methods and store it somewhere.
 3. `PhoneNumberUtil` now has a `createInstance(Context)` method, which
 is a default way to obtain an instance of this class on Android.

License
=======
    Copyright 2022 Michael Rozumyanskiy

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

 [1]: https://github.com/googlei18n/libphonenumber
 [2]: http://www.methodscount.com/?lib=com.googlecode.libphonenumber%3Alibphonenumber%3A8.13.17
 [3]: http://blog.nimbledroid.com/2016/04/06/slow-ClassLoader.getResourceAsStream.html
