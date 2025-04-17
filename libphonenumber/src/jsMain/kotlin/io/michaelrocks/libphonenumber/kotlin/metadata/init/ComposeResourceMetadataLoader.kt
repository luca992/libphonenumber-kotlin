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
import io.github.luca992.libphonenumber_kotlin.libphonenumber.generated.resources.Res
import io.michaelrocks.libphonenumber.kotlin.MetadataLoader
import io.michaelrocks.libphonenumber.kotlin.io.InputStream
import io.michaelrocks.libphonenumber.kotlin.io.OkioInputStream
import okio.Buffer
import org.w3c.xhr.XMLHttpRequest

/**
 * A [MetadataLoader] implementation that reads phone number metadata files as classpath
 * resources.
 */

class ComposeResourceMetadataLoader : MetadataLoader {

    fun fetchFileSynchronously(url: String): ByteArray? {
        val xhr = XMLHttpRequest()
        xhr.open("GET", url, false) // `false` makes it synchronous
        xhr.overrideMimeType("text/plain; charset=x-user-defined") // Interpret response as binary
        xhr.send()

        return if (xhr.status == 200.toShort()) {
            val responseText = xhr.responseText // Read response as text
            // Convert text response to ByteArray
            responseText.encodeToByteArray().map { it.toInt() and 0xFF }
            ByteArray(responseText.length) { i -> responseText[i].code.toByte() }
        } else {
            console.error("Failed to fetch file. Status: ${xhr.status}")
            null
        }
    }


    override fun loadMetadata(phoneMetadataResource: String): InputStream? {
        return try {
            val buffer = Buffer()
            val path = Res.getUri("files/$phoneMetadataResource")
            console.log("loadMetadata path: $path")
            val result = fetchFileSynchronously(path)
            return if (result != null) {
                buffer.write(result)
                OkioInputStream(buffer)
            } else {
                println("Failed to fetch file.")
                null
            }
        } catch (t: Throwable) {
            logger.v("Failed to load metadata from $phoneMetadataResource.path", t)
            null
        }
    }

    companion object {
        private val logger = Logger.withTag(
            ComposeResourceMetadataLoader::class.simpleName.toString()
        )
    }
}
