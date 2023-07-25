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
import io.michaelrocks.libphonenumber.kotlin.metadata.init.MetadataParser
import io.michaelrocks.libphonenumber.kotlin.metadata.source.BlockingMetadataBootstrappingGuard
import io.michaelrocks.libphonenumber.kotlin.metadata.source.FormattingMetadataSource
import io.michaelrocks.libphonenumber.kotlin.metadata.source.MapBackedMetadataContainer
import io.michaelrocks.libphonenumber.kotlin.metadata.source.MapBackedMetadataContainer.Companion.byCountryCallingCode
import io.michaelrocks.libphonenumber.kotlin.metadata.source.MetadataBootstrappingGuard

/**
 * Implementation of [FormattingMetadataSource] guarded by [MetadataBootstrappingGuard]
 *
 *
 * By default, a [BlockingMetadataBootstrappingGuard] will be used, but any custom
 * implementation can be injected.
 */
class FormattingMetadataSourceImpl(
    private val phoneMetadataFileNameProvider: PhoneMetadataFileNameProvider,
    private val bootstrappingGuard: MetadataBootstrappingGuard<MapBackedMetadataContainer<Int>>
) : FormattingMetadataSource {
    constructor(
        phoneMetadataFileNameProvider: PhoneMetadataFileNameProvider,
        metadataLoader: MetadataLoader?,
        metadataParser: MetadataParser?
    ) : this(
        phoneMetadataFileNameProvider,
        BlockingMetadataBootstrappingGuard<MapBackedMetadataContainer<Int>>(
            metadataLoader!!, metadataParser!!, byCountryCallingCode()
        )
    )

    override fun getFormattingMetadataForCountryCallingCode(countryCallingCode: Int): PhoneMetadata? {
        return bootstrappingGuard
            .getOrBootstrap(phoneMetadataFileNameProvider.getFor(countryCallingCode))
            ?.getMetadataBy(countryCallingCode)
    }
}
