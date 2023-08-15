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
package io.michaelrocks.libphonenumber.kotlin.metadata.source

import io.michaelrocks.libphonenumber.kotlin.Phonemetadata.PhoneMetadata.Companion.newBuilder
import io.michaelrocks.libphonenumber.kotlin.internal.GeoEntityUtility
import io.michaelrocks.libphonenumber.kotlin.metadata.source.CompositeMetadataContainer
import junit.framework.TestCase

class CompositeMetadataContainerTest : TestCase() {
    private var metadataContainer: CompositeMetadataContainer? = null
    public override fun setUp() {
        metadataContainer = CompositeMetadataContainer()
    }

    fun test_getMetadataBy_shouldReturnNullForNonExistingRegionCode() {
        assertNull(metadataContainer!!.getMetadataBy(REGION_CODE))
    }

    fun test_getMetadataBy_shouldReturnMetadataForExistingRegionCode() {
        metadataContainer!!.accept(PHONE_METADATA_WITH_REGION_CODE)
        assertSame(PHONE_METADATA_WITH_REGION_CODE, metadataContainer!!.getMetadataBy(REGION_CODE))
    }

    fun test_getMetadataBy_shouldReturnNullForNonExistingCountryCode() {
        assertNull(metadataContainer!!.getMetadataBy(COUNTRY_CODE))
    }

    fun test_getMetadataBy_shouldReturnMetadataForExistingCountryCode() {
        metadataContainer!!.accept(PHONE_METADATA_WITH_COUNTRY_CODE)
        assertSame(PHONE_METADATA_WITH_COUNTRY_CODE, metadataContainer!!.getMetadataBy(COUNTRY_CODE))
    }

    fun test_getMetadataBy_shouldReturnNullForExistingCountryCodeOfGeoRegion() {
        metadataContainer!!.accept(PHONE_METADATA_WITH_REGION_CODE)
        assertNull(metadataContainer!!.getMetadataBy(COUNTRY_CODE))
    }

    companion object {
        private const val REGION_CODE = "US"
        private const val COUNTRY_CODE = 1
        private val PHONE_METADATA_WITH_REGION_CODE = newBuilder().setId(REGION_CODE).setCountryCode(COUNTRY_CODE)
        private val PHONE_METADATA_WITH_COUNTRY_CODE = newBuilder()
            .setId(GeoEntityUtility.REGION_CODE_FOR_NON_GEO_ENTITIES)
            .setCountryCode(COUNTRY_CODE)
    }
}
