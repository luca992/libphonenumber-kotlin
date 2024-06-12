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
package io.michaelrocks.libphonenumber.kotlin.metadata

import io.michaelrocks.libphonenumber.kotlin.MetadataLoader
import io.michaelrocks.libphonenumber.kotlin.metadata.init.MetadataParser.Companion.newLenientParser
import io.michaelrocks.libphonenumber.kotlin.metadata.source.*
import kotlin.jvm.JvmOverloads

/**
 * Provides metadata init and source dependencies when metadata is stored in multi-file mode and
 * loaded as a classpath resource.
 */
class DefaultMetadataDependenciesProvider @JvmOverloads constructor(metadataLoader: MetadataLoader) {
    val metadataParser = newLenientParser()
    val metadataLoader: MetadataLoader
    val phoneNumberMetadataFileNameProvider: PhoneMetadataResourceProvider = MultiFileModeResourceProvider(
        "PhoneNumberMetadataProto"
    )
    val phoneNumberMetadataSource: MetadataSource
    val shortNumberMetadataFileNameProvider: PhoneMetadataResourceProvider = MultiFileModeResourceProvider(
        "ShortNumberMetadataProto"
    )
    val shortNumberMetadataSource: RegionMetadataSource
    val alternateFormatsMetadataFileNameProvider: PhoneMetadataResourceProvider = MultiFileModeResourceProvider(
        "PhoneNumberAlternateFormatsProto"
    )
    val alternateFormatsMetadataSource: FormattingMetadataSource

    init {
        this.metadataLoader = metadataLoader
        phoneNumberMetadataSource = MetadataSourceImpl(
            phoneNumberMetadataFileNameProvider,
            metadataLoader,
            metadataParser
        )
        shortNumberMetadataSource = RegionMetadataSourceImpl(
            shortNumberMetadataFileNameProvider,
            metadataLoader,
            metadataParser
        )
        alternateFormatsMetadataSource = FormattingMetadataSourceImpl(
            alternateFormatsMetadataFileNameProvider,
            metadataLoader,
            metadataParser
        )
    }

    val carrierDataDirectory: String
        get() = "io/michaelrocks/libphonenumber/android/carrier/data/"
    val geocodingDataDirectory: String
        get() = "io/michaelrocks/libphonenumber/android/geocoding/data/"
}
