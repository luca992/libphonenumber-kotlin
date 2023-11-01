package io.michaelrocks.libphonenumber.kotlin.metadata

import io.michaelrocks.libphonenumber.kotlin.MetadataLoader
import io.michaelrocks.libphonenumber.kotlin.metadata.init.MokoAssetResourceMetadataLoader

actual fun defaultMetadataLoader(): MetadataLoader {
    return MokoAssetResourceMetadataLoader()
}