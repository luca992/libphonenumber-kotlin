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

import io.michaelrocks.libphonenumber.kotlin.MetadataLoader
import io.michaelrocks.libphonenumber.kotlin.Phonemetadata.PhoneMetadata
import io.michaelrocks.libphonenumber.kotlin.internal.GeoEntityUtility.isGeoEntity
import io.michaelrocks.libphonenumber.kotlin.metadata.init.MetadataParser
import io.michaelrocks.libphonenumber.kotlin.metadata.source.*
import io.michaelrocks.libphonenumber.kotlin.metadata.source.BlockingMetadataBootstrappingGuard

/**
 * Implementation of [RegionMetadataSource] guarded by [MetadataBootstrappingGuard]
 *
 *
 * By default, a [BlockingMetadataBootstrappingGuard] will be used, but any custom
 * implementation can be injected.
 */
class RegionMetadataSourceImpl(
    private val phoneMetadataFileNameProvider: PhoneMetadataFileNameProvider,
    private val bootstrappingGuard: MetadataBootstrappingGuard<MapBackedMetadataContainer<String>>
) : RegionMetadataSource {
    constructor(
        phoneMetadataFileNameProvider: PhoneMetadataFileNameProvider,
        metadataLoader: MetadataLoader,
        metadataParser: MetadataParser
    ) : this(
        phoneMetadataFileNameProvider,
        BlockingMetadataBootstrappingGuard<MapBackedMetadataContainer<String>>(
            metadataLoader, metadataParser, MapBackedMetadataContainer.byRegionCode()
        )
    )

    override fun getMetadataForRegion(regionCode: String?): PhoneMetadata? {
        require(isGeoEntity(regionCode!!)) { "$regionCode region code is a non-geo entity" }
        return bootstrappingGuard
            .getOrBootstrap(phoneMetadataFileNameProvider.getFor(regionCode))
            ?.getMetadataBy(regionCode)
    }
}
