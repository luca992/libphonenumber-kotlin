package io.michaelrocks.libphonenumber.kotlin.metadata

import io.michaelrocks.libphonenumber.kotlin.MetadataLoader
import io.michaelrocks.libphonenumber.kotlin.applicationContext
import io.michaelrocks.libphonenumber.kotlin.io.InputStream
import io.michaelrocks.libphonenumber.kotlin.metadata.source.AssetsMetadataLoader

actual fun defaultMetadataLoader(): MetadataLoader {
    val assets = applicationContext?.assets
    if (assets == null) {
        println("Warning: Android application context is not provided. Skipping adding Kamel Components requiring Android application context.")
        return object : MetadataLoader {
            override fun loadMetadata(phoneMetadataResource: String): InputStream? {
                println("Warning: Android application context is not provided. Skipping loading metadata.")
                return null
            }
        }
    }

    return AssetsMetadataLoader(assets)
}