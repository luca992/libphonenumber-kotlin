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
 * resources for both browser and Node.js environments.
 */
class ComposeResourceMetadataLoader : MetadataLoader {

    // Check if running in Node.js environment
    private val isNode: Boolean = js("typeof process !== 'undefined' && process.versions && process.versions.node")

    // Function to fetch file in browser environment using XMLHttpRequest
    private fun fetchFileBrowser(url: String): ByteArray? {
        val xhr = XMLHttpRequest()
        xhr.open("GET", url, false) // `false` makes it synchronous
        xhr.overrideMimeType("text/plain; charset=x-user-defined") // Interpret response as binary
        xhr.send()

        return if (xhr.status == 200.toShort()) {
            val responseText = xhr.responseText // Read response as text
            // Convert text response to ByteArray
            ByteArray(responseText.length) { i -> responseText[i].code.toByte() }
        } else {
            console.error("Failed to fetch file in browser. Status: ${xhr.status}")
            null
        }
    }

    // Function to read file in Node.js environment using fs module
    private fun readFileNode(path: String): ByteArray? {
        return try {
            // Use dynamic import to avoid webpack errors in browser
            val fs = if (isNode) js("require('fs')") else null
            if (fs == null) {
                console.error("Failed to load fs module in Node.js")
                return null
            }
            val buffer = fs.readFileSync(path)
            val uint8Array = js("new Uint8Array(buffer)")
            val length = uint8Array.length as Int
            ByteArray(length) { i -> (uint8Array[i] as Int).toByte() }
        } catch (e: Throwable) {
            console.error("Failed to read file in Node.js: ${e.message}")
            null
        }
    }

    // Function to resolve path for Node.js environment
    private fun resolveNodePath(resourcePath: String): String {
        val path = js("require('path')")
        val process = js("process")

        // Try to find the file in several possible locations
        val currentDir = process.cwd() as String
        return path.resolve(
            currentDir,
            "kotlin/composeResources/io.github.luca992.libphonenumber_kotlin.libphonenumber.generated.resources/$resourcePath"
        ) as String
    }

    override fun loadMetadata(phoneMetadataResource: String): InputStream? {
        return try {
            val buffer = Buffer()
            val resourcePath = "files/$phoneMetadataResource"

            val result = if (isNode) {
                // Node.js environment
                val nodePath = resolveNodePath(resourcePath)
                console.log("loadMetadata Node.js path: $nodePath")
                readFileNode(nodePath)
            } else {
                // Browser environment
                val browserPath = Res.getUri(resourcePath)
                console.log("loadMetadata browser path: $browserPath")
                fetchFileBrowser(browserPath)
            }

            return if (result != null) {
                buffer.write(result)
                OkioInputStream(buffer)
            } else {
                console.error("Failed to load file.")
                null
            }
        } catch (t: Throwable) {
            logger.v("Failed to load metadata from $phoneMetadataResource", t)
            null
        }
    }

    companion object {
        private val logger = Logger.withTag(
            ComposeResourceMetadataLoader::class.simpleName.toString()
        )
    }
}
