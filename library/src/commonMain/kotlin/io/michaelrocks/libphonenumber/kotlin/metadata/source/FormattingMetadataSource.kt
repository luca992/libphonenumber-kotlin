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

/** A source of formatting phone metadata.  */
interface FormattingMetadataSource {
    /**
     * Returns formatting phone metadata for provided country calling code.
     *
     *
     * This method is similar to the one in [ ][NonGeographicalEntityMetadataSource.getMetadataForNonGeographicalRegion], except that it
     * will not fail for geographical regions, it can be used for both geo- and non-geo entities.
     *
     *
     * In case the provided `countryCallingCode` maps to several different regions, only one
     * would contain formatting metadata.
     *
     * @return the phone metadata for provided `countryCallingCode`, or null if there is none.
     */
    fun getFormattingMetadataForCountryCallingCode(countryCallingCode: Int): PhoneMetadata?
}
