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
package io.michaelrocks.libphonenumber.kotlin.internal

import io.michaelrocks.libphonenumber.kotlin.CountryCodeToRegionCodeMap.countryCodeToRegionCodeMap
import kotlin.jvm.JvmStatic

/**
 * Utility class for checking whether identifiers region code and country calling code belong
 * to geographical entities. For more information about geo vs. non-geo entities see [ ] and [ ]
 */
object GeoEntityUtility {
    /** Region code with a special meaning, used to mark non-geographical entities  */
    const val REGION_CODE_FOR_NON_GEO_ENTITIES = "001"

    /** Determines whether `regionCode` belongs to a geographical entity.  */
    @JvmStatic
    fun isGeoEntity(regionCode: String): Boolean {
        return regionCode != REGION_CODE_FOR_NON_GEO_ENTITIES
    }

    /**
     * Determines whether `countryCallingCode` belongs to a geographical entity.
     *
     *
     * A single country calling code could map to several different regions. It is considered that
     * `countryCallingCode` belongs to a geo entity if all of these regions are geo entities
     *
     *
     * Note that this method will not throw an exception even when the underlying mapping for the
     * `countryCallingCode` does not exist, instead it will return `false`
     */
    @JvmStatic
    fun isGeoEntity(countryCallingCode: Int): Boolean {
        val regionCodesForCountryCallingCode = countryCodeToRegionCodeMap[countryCallingCode]
        return (regionCodesForCountryCallingCode != null
                && !regionCodesForCountryCallingCode.contains(REGION_CODE_FOR_NON_GEO_ENTITIES))
    }
}
