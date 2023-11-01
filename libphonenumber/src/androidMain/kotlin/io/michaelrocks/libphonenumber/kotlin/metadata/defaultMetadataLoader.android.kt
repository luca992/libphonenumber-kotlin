package io.michaelrocks.libphonenumber.kotlin.metadata

import io.michaelrocks.libphonenumber.kotlin.MetadataLoader
import io.michaelrocks.libphonenumber.kotlin.applicationContext
import io.michaelrocks.libphonenumber.kotlin.metadata.source.AssetsMetadataLoader

actual fun defaultMetadataLoader(): MetadataLoader {
    return AssetsMetadataLoader(applicationContext.assets)
}