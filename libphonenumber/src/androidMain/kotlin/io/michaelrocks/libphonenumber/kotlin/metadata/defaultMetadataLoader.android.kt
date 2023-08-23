package io.michaelrocks.libphonenumber.kotlin.metadata

import io.michaelrocks.libphonenumber.kotlin.MetadataLoader
import io.michaelrocks.libphonenumber.kotlin.metadata.source.AssetsMetadataLoader

actual fun defaultMetadataLoader(context: Any?): MetadataLoader {
    return AssetsMetadataLoader((context as android.content.Context).assets)
}