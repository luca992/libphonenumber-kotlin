package io.michaelrocks.libphonenumber.kotlin.metadata

import io.michaelrocks.libphonenumber.kotlin.MetadataLoader
import io.michaelrocks.libphonenumber.kotlin.metadata.init.ClassPathResourceMetadataLoader

actual val defaultMetadataLoader: MetadataLoader
    get() = ClassPathResourceMetadataLoader()