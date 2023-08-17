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
package io.michaelrocks.libphonenumber.kotlin.metadata.init

import co.touchlab.kermit.Logger
import io.michaelrocks.libphonenumber.kotlin.Phonemetadata
import io.michaelrocks.libphonenumber.kotlin.Phonemetadata.PhoneMetadata
import io.michaelrocks.libphonenumber.kotlin.io.InputStream
import io.michaelrocks.libphonenumber.kotlin.io.ObjectInputStream
import okio.IOException
import kotlin.jvm.JvmStatic

/**
 * Exposes single method for parsing [InputStream] content into [Collection] of [ ]
 */
class MetadataParser private constructor(private val strictMode: Boolean) {
    /**
     * Parses given [InputStream] into a [Collection] of [PhoneMetadata].
     *
     * @throws IllegalArgumentException if `source` is `null` and strict mode is on
     * @return parsed [PhoneMetadata], or empty [Collection] if `source` is `null` and lenient mode is on
     */
    fun parse(source: InputStream?): Collection<PhoneMetadata> {
        if (source == null) {
            return handleNullSource()
        }
        var ois: ObjectInputStream? = null
        return try {
            ois = ObjectInputStream(source)
            val phoneMetadataCollection = Phonemetadata.PhoneMetadataCollection()
            phoneMetadataCollection.readExternal(ois)
            val phoneMetadata = phoneMetadataCollection.metadataList
            // Sanity check; this should not happen if provided InputStream is valid
            check(!phoneMetadata.isEmpty()) { "Empty metadata" }
            phoneMetadataCollection.metadataList
        } catch (e: IOException) {
            throw IllegalStateException("Unable to parse metadata file", e)
        } finally {
            if (ois != null) {
                // This will close all underlying streams as well, including source.
                close(ois)
            } else {
                close(source)
            }
        }
    }

    private fun handleNullSource(): List<PhoneMetadata> {
        require(!strictMode) { "Source cannot be null" }
        return emptyList()
    }

    private fun close(inputStream: InputStream) {
        try {
            inputStream.close()
        } catch (e: IOException) {
            logger.w("Error closing input stream (ignored)", e)
        }
    }

    companion object {
        private val logger = Logger.withTag(MetadataParser::class.simpleName.toString())

        /**
         * Creates new instance in lenient mode, see [MetadataParser.parse] for more
         * info.
         */
        @JvmStatic
        fun newLenientParser(): MetadataParser {
            return MetadataParser(false)
        }

        /**
         * Creates new instance in strict mode, see [MetadataParser.parse] for more
         * info
         */
        fun newStrictParser(): MetadataParser {
            return MetadataParser(true)
        }
    }
}
