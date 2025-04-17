/*
 * Copyright (C) 2017 Michael Rozumyanskiy
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
package io.michaelrocks.libphonenumber.kotlin.metadata.source;

import android.content.res.AssetManager
import io.github.luca992.libphonenumber_kotlin.libphonenumber.generated.resources.Res
import io.michaelrocks.libphonenumber.kotlin.MetadataLoader
import io.michaelrocks.libphonenumber.kotlin.io.InputStream
import io.michaelrocks.libphonenumber.kotlin.io.JavaInputStream
import okio.IOException
import org.jetbrains.compose.resources.MissingResourceException

class AssetsMetadataLoader(private val assetManager: AssetManager) : MetadataLoader {
    override fun loadMetadata(phoneMetadataResource: String): InputStream? {
        return try {
            val path = Res.getUri("files/$phoneMetadataResource").removePrefix("file:///android_asset/")
            println(path)
            JavaInputStream(assetManager.open(path))
        } catch (exception: IOException) {
            exception.printStackTrace()
            null
        } catch (exception: MissingResourceException) {
            null
        }
    }
}
