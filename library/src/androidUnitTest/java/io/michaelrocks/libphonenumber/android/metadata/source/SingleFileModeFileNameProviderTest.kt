/*
 * Copyright (C) 2022 The Libphonenumber Authors
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
package io.michaelrocks.libphonenumber.android.metadata.source

import io.michaelrocks.libphonenumber.kotlin.metadata.source.PhoneMetadataFileNameProvider
import io.michaelrocks.libphonenumber.kotlin.metadata.source.SingleFileModeFileNameProvider
import junit.framework.TestCase

class SingleFileModeFileNameProviderTest : TestCase() {
    private val metadataFileNameProvider: PhoneMetadataFileNameProvider = SingleFileModeFileNameProvider("some/file")
    fun test_getFor_shouldReturnTheFileNameBase() {
        val metadataFileName = metadataFileNameProvider.getFor("key1")
        assertEquals("some/file", metadataFileName)
    }
}
