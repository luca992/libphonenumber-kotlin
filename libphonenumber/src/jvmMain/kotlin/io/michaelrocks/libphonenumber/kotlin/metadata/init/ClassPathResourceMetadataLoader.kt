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

/**
 * A [MetadataLoader] implementation that reads phone number metadata files as classpath
 * resources.
 */
class ClassPathResourceMetadataLoader : MetadataLoader {
    override fun loadMetadata(phoneMetadataResource: AssetResource): InputStream? {
        return phoneMetadataResource.resourcesClassLoader.getResourceAsStream(phoneMetadataResource.filePath)
    }

    companion object {
        private val logger = Logger.withTag(
            ClassPathResourceMetadataLoader::class.simpleName.toString()
        )
    }
}
