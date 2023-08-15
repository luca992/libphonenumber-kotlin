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
package io.michaelrocks.libphonenumber.android.internal

import io.michaelrocks.libphonenumber.kotlin.internal.GeoEntityUtility.isGeoEntity
import junit.framework.TestCase

class GeoEntityUtilityTest : TestCase() {
    fun test_isGeoEntity_shouldReturnTrueForCountryRegionCode() {
        assertTrue(isGeoEntity("DE"))
    }

    fun test_isGeoEntity_shouldReturnFalseForWorldRegionCode() {
        assertFalse(isGeoEntity("001"))
    }

    fun test_isGeoEntity_shouldReturnTrueForCountryCallingCode() {
        assertTrue(isGeoEntity(41))
    }

    fun test_isGeoEntity_shouldReturnFalseForInternationalSharedCostServiceCallingCode() {
        assertFalse(isGeoEntity(808))
    }

    fun test_isGeoEntity_shouldReturnFalseForNonExistingCountryCallingCode() {
        assertFalse(isGeoEntity(111111111))
    }
}
