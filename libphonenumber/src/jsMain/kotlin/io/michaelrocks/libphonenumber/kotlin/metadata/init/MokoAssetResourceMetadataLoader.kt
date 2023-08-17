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
import dev.icerock.moko.resources.AssetResource
import io.michaelrocks.libphonenumber.kotlin.MetadataLoader
import io.michaelrocks.libphonenumber.kotlin.io.InputStream
import io.michaelrocks.libphonenumber.kotlin.io.OkioInputStream
import okio.Buffer
import okio.ByteString.Companion.decodeBase64

/**
 * A [MetadataLoader] implementation that reads phone number metadata files as classpath
 * resources.
 */

class MokoAssetResourceMetadataLoader : MetadataLoader {
    override fun loadMetadata(phoneMetadataResource: AssetResource): InputStream? {
        return try {
            val buffer = Buffer()
            // webpack is configured to bundle the metadata files as base64 strings by using asset/inline
            // So phoneMetadataResource.originalPath will actually be a base64 string of the file contents
            // in this mode vs asset/resource which would be the path to the file
            val base64Data = phoneMetadataResource.originalPath.decodeBase64()!!
            buffer.write(base64Data.toByteArray())
            OkioInputStream(buffer)
        } catch (t: Throwable) {
            logger.v("Failed to load metadata from $phoneMetadataResource.path", t)
            null
        }
    }

    companion object {
        private val logger = Logger.withTag(
            MokoAssetResourceMetadataLoader::class.simpleName.toString()
        )
    }
}
