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

import io.michaelrocks.libphonenumber.kotlin.Phonemetadata.PhoneMetadata
import io.michaelrocks.libphonenumber.kotlin.internal.GeoEntityUtility
import io.michaelrocks.libphonenumber.kotlin.internal.GeoEntityUtility.isGeoEntity
import io.michaelrocks.libphonenumber.kotlin.metadata.source.MapBackedMetadataContainer
import io.michaelrocks.libphonenumber.kotlin.metadata.source.MapBackedMetadataContainer.Companion.byCountryCallingCode
import io.michaelrocks.libphonenumber.kotlin.metadata.source.MapBackedMetadataContainer.Companion.byRegionCode
import io.michaelrocks.libphonenumber.kotlin.metadata.source.MetadataContainer

/**
 * Implementation of [MetadataContainer] which is a composition of different [ ]s. It adds items to a single simpler container at a time depending on
 * the content of [PhoneMetadata].
 */
class CompositeMetadataContainer : MetadataContainer {
    private val metadataByCountryCode = byCountryCallingCode()
    private val metadataByRegionCode = byRegionCode()

    /**
     * Intended to be called for geographical regions only. For non-geographical entities, use [ ][CompositeMetadataContainer.getMetadataBy]
     */
    fun getMetadataBy(regionCode: String?): PhoneMetadata? {
        return metadataByRegionCode.getMetadataBy(regionCode)
    }

    /**
     * Intended to be called for non-geographical entities only, such as 800 (country code assigned to
     * the Universal International Freephone Service). For geographical regions, use [ ][CompositeMetadataContainer.getMetadataBy]
     */
    fun getMetadataBy(countryCallingCode: Int): PhoneMetadata? {
        return metadataByCountryCode.getMetadataBy(countryCallingCode)
    }

    /**
     * If the metadata belongs to a specific geographical region (it has a region code other than
     * [GeoEntityUtility.REGION_CODE_FOR_NON_GEO_ENTITIES]), it will be added to a [ ] which stores metadata by region code. Otherwise, it will be added
     * to a [MapBackedMetadataContainer] which stores metadata by country calling code. This
     * means that [CompositeMetadataContainer.getMetadataBy] will not work for country
     * calling codes such as 41 (country calling code for Switzerland), only for country calling codes
     * such as 800 (country code assigned to the Universal International Freephone Service)
     */
    override fun accept(phoneMetadata: PhoneMetadata?) {
        val regionCode = metadataByRegionCode.keyProvider.getKeyOf(phoneMetadata!!)
        if (isGeoEntity(regionCode)) {
            metadataByRegionCode.accept(phoneMetadata)
        } else {
            metadataByCountryCode.accept(phoneMetadata)
        }
    }
}
