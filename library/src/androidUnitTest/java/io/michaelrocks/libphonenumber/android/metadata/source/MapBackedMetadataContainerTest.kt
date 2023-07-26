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

import io.michaelrocks.libphonenumber.kotlin.Phonemetadata.PhoneMetadata.Companion.newBuilder
import io.michaelrocks.libphonenumber.kotlin.metadata.source.MapBackedMetadataContainer.Companion.byCountryCallingCode
import io.michaelrocks.libphonenumber.kotlin.metadata.source.MapBackedMetadataContainer.Companion.byRegionCode
import junit.framework.TestCase

class MapBackedMetadataContainerTest : TestCase() {
    fun test_getMetadataBy_shouldReturnNullForNullRegionCode() {
        assertNull(byRegionCode().getMetadataBy(null))
    }

    fun test_getMetadataBy_shouldReturnNullForNonExistingRegionCode() {
        assertNull(byRegionCode().getMetadataBy(REGION_CODE))
    }

    fun test_getMetadataBy_shouldReturnMetadataForExistingRegionCode() {
        val metadataContainer = byRegionCode()
        metadataContainer.accept(PHONE_METADATA)
        assertSame(PHONE_METADATA, metadataContainer.getMetadataBy(REGION_CODE))
    }

    fun test_getMetadataBy_shouldReturnNullForNullCountryCode() {
        assertNull(byCountryCallingCode().getMetadataBy(null))
    }

    fun test_getMetadataBy_shouldReturnNullForNonExistingCountryCode() {
        assertNull(byCountryCallingCode().getMetadataBy(COUNTRY_CODE))
    }

    fun test_getMetadataBy_shouldReturnMetadataForExistingCountryCode() {
        val metadataContainer = byCountryCallingCode()
        metadataContainer.accept(PHONE_METADATA)
        assertSame(PHONE_METADATA, metadataContainer.getMetadataBy(COUNTRY_CODE))
    }

    companion object {
        private const val REGION_CODE = "US"
        private const val COUNTRY_CODE = 41
        private val PHONE_METADATA = newBuilder().setId(REGION_CODE).setCountryCode(COUNTRY_CODE)
    }
}
