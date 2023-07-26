package io.michaelrocks.libphonenumber.kotlin.metadata

import io.michaelrocks.libphonenumber.kotlin.metadata.init.ClassPathResourceMetadataLoader

actual val instance: DefaultMetadataDependenciesProvider
    get() = DefaultMetadataDependenciesProvider(ClassPathResourceMetadataLoader())