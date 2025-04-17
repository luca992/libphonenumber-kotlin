/*
 * Copyright (C) 2012 The Libphonenumber Authors
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
package io.michaelrocks.libphonenumber.kotlin

import io.michaelrocks.libphonenumber.kotlin.metadata.DefaultMetadataDependenciesProvider
import io.michaelrocks.libphonenumber.kotlin.metadata.source.MetadataSourceImpl
import io.michaelrocks.libphonenumber.kotlin.metadata.source.MultiFileModeResourceProvider

/**
 * Root class for PhoneNumberUtil tests that depend on the test metadata file.
 *
 *
 * Note since tests that extend this class do not use the normal metadata file, they should not be
 * used for regression test purposes.
 *
 *
 * Ideally the `phoneUtil` field (which uses test metadata) would be the only way that tests
 * need to interact with a PhoneNumberUtil instance. However as some static methods in the library
 * invoke "getInstance()" internally, we must also inject the test instance as the PhoneNumberUtil
 * singleton. This means it is unsafe to run tests derived from this class in parallel with each
 * other or at the same time as other tests which might require the singleton instance.
 *
 * @author Shaopeng Jia
 */
abstract class TestMetadataTestCase {


    abstract val metadataLoader: MetadataLoader
    val metadataDependenciesProvider = DefaultMetadataDependenciesProvider(metadataLoader)

    /**
     * An instance of PhoneNumberUtil that uses test metadata.
     */
    protected val phoneUtil: PhoneNumberUtil = PhoneNumberUtil(
        MetadataSourceImpl(
            MultiFileModeResourceProvider(TEST_METADATA_FILE_PREFIX),
            metadataDependenciesProvider.metadataLoader,
            metadataDependenciesProvider.metadataParser
        ), metadataDependenciesProvider, CountryCodeToRegionCodeMapForTesting.countryCodeToRegionCodeMap
    )

    companion object {
        private const val TEST_METADATA_FILE_PREFIX =
            "PhoneNumberMetadataProtoForTesting"
    }
}