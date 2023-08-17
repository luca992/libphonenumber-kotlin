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

import io.michaelrocks.libphonenumber.kotlin.Phonemetadata.PhoneMetadata
import kotlin.jvm.JvmField
import kotlin.jvm.JvmStatic


/**
 * A [MetadataContainer] implementation backed by a [ConcurrentHashMap] with generic
 * keys.
 */
class MapBackedMetadataContainer<T> private constructor(@JvmField val keyProvider: KeyProvider<T>) :
    MetadataContainer {
    private val metadataMap: MutableMap<T, PhoneMetadata?> = mutableMapOf()

    fun getMetadataBy(key: T?): PhoneMetadata? {
        return if (key != null) metadataMap[key] else null
    }

    override fun accept(phoneMetadata: PhoneMetadata?) {
        metadataMap[keyProvider.getKeyOf(phoneMetadata!!)] = phoneMetadata
    }

    interface KeyProvider<T> {
        fun getKeyOf(phoneMetadata: PhoneMetadata): T
    }

    companion object {
        @JvmStatic
        fun byRegionCode(): MapBackedMetadataContainer<String> {
            return MapBackedMetadataContainer<String>(
                object : KeyProvider<String> {
                    override fun getKeyOf(phoneMetadata: PhoneMetadata): String {
                        return phoneMetadata.id!!
                    }
                }
            )
        }

        @JvmStatic
        fun byCountryCallingCode(): MapBackedMetadataContainer<Int> {
            return MapBackedMetadataContainer(
                object : KeyProvider<Int> {
                    override fun getKeyOf(phoneMetadata: PhoneMetadata): Int {
                        return phoneMetadata.countryCode
                    }
                })
        }
    }
}
