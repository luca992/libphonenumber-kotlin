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
import org.jetbrains.compose.resources.MissingResourceException

/**
 * A blocking implementation of [MetadataBootstrappingGuard]. Can be used for both single-file
 * (bulk) and multi-file metadata
 *
 * @param <T> needs to extend [MetadataContainer]
</T> */
internal class BlockingMetadataBootstrappingGuard<T : MetadataContainer>(
    private val metadataLoader: MetadataLoader,
    private val metadataParser: MetadataParser,
    private val metadataContainer: T
) : MetadataBootstrappingGuard<T> {
    private val loadedFiles // identity map
            : MutableSet<String> = mutableSetOf()


    override fun getOrBootstrap(phoneMetadataResource: String?): T? {
        phoneMetadataResource ?: return null
        if (!loadedFiles.contains(phoneMetadataResource)) {
            bootstrapMetadata(phoneMetadataResource)
        }
        return metadataContainer
    }

    //    @Synchronized
    private fun bootstrapMetadata(phoneMetadataResource: String) {
        // Additional check is needed because multiple threads could pass the first check when calling
        // getOrBootstrap() at the same time for unloaded metadata file
        if (loadedFiles.contains(phoneMetadataResource)) {
            return
        }
        val phoneMetadata = read(phoneMetadataResource)
        for (metadata in phoneMetadata) {
            metadataContainer.accept(metadata)
        }
        loadedFiles.add(phoneMetadataResource)
    }

    private fun read(phoneMetadataResource: String): Collection<PhoneMetadata> {
        return try {
            val metadataStream = metadataLoader.loadMetadata(phoneMetadataResource)
            metadataParser.parse(metadataStream)
        } catch (e: IllegalArgumentException) {
            throw IllegalStateException("Failed to read file ${phoneMetadataResource}", e)
        } catch (e: IllegalStateException) {
            throw IllegalStateException("Failed to read file ${phoneMetadataResource}", e)
        }
    }
}
