/*
 * Copyright (C) 2014 The Libphonenumber Authors
 * Copyright (C) 2022 Michael Rozumyanskiy
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.michaelrocks.libphonenumber.kotlin

import io.michaelrocks.libphonenumber.kotlin.io.InputStream


/**
 * Interface for clients to specify a customized phone metadata loader, useful for Android apps to
 * load Android resources since the library loads Java resources by default, e.g. with
 * [
 * AssetManager](http://developer.android.com/reference/android/content/res/AssetManager.html). Note that implementation owners have the responsibility to ensure this is
 * thread-safe.
 */
interface MetadataLoader {
    /**
     * Returns an input stream corresponding to the metadata to load. This method may be called
     * concurrently so implementations must be thread-safe.
     *
     * @param phoneMetadataResourceUri the path relative to the composeResources directory
     * @return  the input stream for the metadata file. The library will close this stream
     * after it is done. Return null in case the metadata file could not be found
     */
    fun loadMetadata(phoneMetadataResourceUri: String): InputStream?
}
