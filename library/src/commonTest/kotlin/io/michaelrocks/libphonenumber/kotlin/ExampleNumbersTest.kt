/*
 * Copyright (C) 2009 The Libphonenumber Authors
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

import co.touchlab.kermit.Logger
import io.michaelrocks.libphonenumber.kotlin.PhoneNumberUtil.PhoneNumberType
import io.michaelrocks.libphonenumber.kotlin.Phonenumber.PhoneNumber
import io.michaelrocks.libphonenumber.kotlin.metadata.DefaultMetadataDependenciesProvider
import io.michaelrocks.libphonenumber.kotlin.metadata.defaultMetadataLoader
import kotlin.test.*

/**
 * Verifies all of the example numbers in the metadata are valid and of the correct type. If no
 * example number exists for a particular type, the test still passes since not all types are
 * relevant for all regions. Tests that check the XML schema will ensure that an exampleNumber
 * node is present for every phone number description.
 */
class ExampleNumbersTest {
    private val metadataDependenciesProvider = DefaultMetadataDependenciesProvider(defaultMetadataLoader)
    private val phoneNumberUtil = PhoneNumberUtil.createInstance(metadataDependenciesProvider.metadataLoader)
    private val shortNumberInfo: ShortNumberInfo = phoneNumberUtil.shortNumberInfo!!
    private val shortNumberMetadataSource = metadataDependenciesProvider.shortNumberMetadataSource
    private val invalidCases: MutableList<PhoneNumber?> = ArrayList()
    private val wrongTypeCases: MutableList<PhoneNumber> = ArrayList()
    private val shortNumberSupportedRegions: Set<String> = ShortNumbersRegionCodeSet.regionCodeSet

    /**
     * @param exampleNumberRequestedType type we are requesting an example number for
     * @param possibleExpectedTypes      acceptable types that this number should match, such as
     * FIXED_LINE and FIXED_LINE_OR_MOBILE for a fixed line example number.
     */
    private fun checkNumbersValidAndCorrectType(
        exampleNumberRequestedType: PhoneNumberType, possibleExpectedTypes: Set<PhoneNumberType>
    ) {
        for (regionCode in phoneNumberUtil.getSupportedRegions()) {
            val exampleNumber = phoneNumberUtil.getExampleNumberForType(regionCode, exampleNumberRequestedType)
            if (exampleNumber != null) {
                if (!phoneNumberUtil.isValidNumber(exampleNumber)) {
                    invalidCases.add(exampleNumber)
                    logger.e("Failed validation for $exampleNumber")
                } else {
                    // We know the number is valid, now we check the type.
                    val exampleNumberType = phoneNumberUtil.getNumberType(exampleNumber)
                    if (!possibleExpectedTypes.contains(exampleNumberType)) {
                        wrongTypeCases.add(exampleNumber)
                        logger.e(
                            "Wrong type for " + exampleNumber + ": got " + exampleNumberType
                        )
                        logger.w("Expected types: ")
                        for (type in possibleExpectedTypes) {
                            logger.w(type.toString())
                        }
                    }
                }
            }
        }
    }

    @Test
    fun testFixedLine() {
        val fixedLineTypes: Set<PhoneNumberType> = setOf(
            PhoneNumberType.FIXED_LINE, PhoneNumberType.FIXED_LINE_OR_MOBILE
        )
        checkNumbersValidAndCorrectType(PhoneNumberType.FIXED_LINE, fixedLineTypes)
        assertEquals(0, invalidCases.size)
        assertEquals(0, wrongTypeCases.size)
    }

    @Test
    fun testMobile() {
        val mobileTypes: Set<PhoneNumberType> = setOf(
            PhoneNumberType.MOBILE, PhoneNumberType.FIXED_LINE_OR_MOBILE
        )
        checkNumbersValidAndCorrectType(PhoneNumberType.MOBILE, mobileTypes)
        assertEquals(0, invalidCases.size)
        assertEquals(0, wrongTypeCases.size)
    }

    @Test
    fun testTollFree() {
        val tollFreeTypes: Set<PhoneNumberType> = setOf(PhoneNumberType.TOLL_FREE)
        checkNumbersValidAndCorrectType(PhoneNumberType.TOLL_FREE, tollFreeTypes)
        assertEquals(0, invalidCases.size)
        assertEquals(0, wrongTypeCases.size)
    }

    @Test
    fun testPremiumRate() {
        val premiumRateTypes: Set<PhoneNumberType> = setOf(PhoneNumberType.PREMIUM_RATE)
        checkNumbersValidAndCorrectType(PhoneNumberType.PREMIUM_RATE, premiumRateTypes)
        assertEquals(0, invalidCases.size)
        assertEquals(0, wrongTypeCases.size)
    }

    @Test
    fun testVoip() {
        val voipTypes: Set<PhoneNumberType> = setOf(PhoneNumberType.VOIP)
        checkNumbersValidAndCorrectType(PhoneNumberType.VOIP, voipTypes)
        assertEquals(0, invalidCases.size)
        assertEquals(0, wrongTypeCases.size)
    }

    @Test
    fun testPager() {
        val pagerTypes: Set<PhoneNumberType> = setOf(PhoneNumberType.PAGER)
        checkNumbersValidAndCorrectType(PhoneNumberType.PAGER, pagerTypes)
        assertEquals(0, invalidCases.size)
        assertEquals(0, wrongTypeCases.size)
    }

    @Test
    fun testUan() {
        val uanTypes: Set<PhoneNumberType> = setOf(PhoneNumberType.UAN)
        checkNumbersValidAndCorrectType(PhoneNumberType.UAN, uanTypes)
        assertEquals(0, invalidCases.size)
        assertEquals(0, wrongTypeCases.size)
    }

    @Test
    fun testVoicemail() {
        val voicemailTypes: Set<PhoneNumberType> = setOf(PhoneNumberType.VOICEMAIL)
        checkNumbersValidAndCorrectType(PhoneNumberType.VOICEMAIL, voicemailTypes)
        assertEquals(0, invalidCases.size)
        assertEquals(0, wrongTypeCases.size)
    }

    @Test
    fun testSharedCost() {
        val sharedCostTypes: Set<PhoneNumberType> = setOf(PhoneNumberType.SHARED_COST)
        checkNumbersValidAndCorrectType(PhoneNumberType.SHARED_COST, sharedCostTypes)
        assertEquals(0, invalidCases.size)
        assertEquals(0, wrongTypeCases.size)
    }

    @Test
    fun testCanBeInternationallyDialled() {
        for (regionCode in phoneNumberUtil.getSupportedRegions()) {
            var exampleNumber: PhoneNumber? = null
            val desc = phoneNumberUtil.getMetadataForRegion(regionCode)!!.noInternationalDialling
            try {
                if (desc!!.hasExampleNumber()) {
                    exampleNumber = phoneNumberUtil.parse(desc.exampleNumber, regionCode)
                }
            } catch (e: NumberParseException) {
                logger.e(e.toString())
            }
            if (exampleNumber != null && phoneNumberUtil.canBeInternationallyDialled(exampleNumber)) {
                wrongTypeCases.add(exampleNumber)
                logger.e(
                    "Number " + exampleNumber + " should not be internationally diallable"
                )
            }
        }
        assertEquals(0, wrongTypeCases.size)
    }

    @Test
    fun testGlobalNetworkNumbers() {
        for (callingCode in phoneNumberUtil.supportedGlobalNetworkCallingCodes) {
            val exampleNumber = phoneNumberUtil.getExampleNumberForNonGeoEntity(callingCode)
            assertNotNull(exampleNumber, "No example phone number for calling code $callingCode")
            if (!phoneNumberUtil.isValidNumber(exampleNumber)) {
                invalidCases.add(exampleNumber)
                logger.e("Failed validation for $exampleNumber")
            }
        }
        assertEquals(0, invalidCases.size)
    }

    @Test
    fun testEveryRegionHasAnExampleNumber() {
        for (regionCode in phoneNumberUtil.getSupportedRegions()) {
            val exampleNumber = phoneNumberUtil.getExampleNumber(regionCode)
            assertNotNull(exampleNumber, "No example number found for region $regionCode")
        }
    }

    @Test
    fun testEveryRegionHasAnInvalidExampleNumber() {
        for (regionCode in phoneNumberUtil.getSupportedRegions()) {
            val exampleNumber = phoneNumberUtil.getInvalidExampleNumber(regionCode)
            assertNotNull(exampleNumber, "No invalid example number found for region $regionCode")
        }
    }

    @Test
    fun testEveryTypeHasAnExampleNumber() {
        for (type in PhoneNumberType.entries.toTypedArray()) {
            if (type === PhoneNumberType.UNKNOWN) {
                continue
            }
            val exampleNumber = phoneNumberUtil.getExampleNumberForType(type)
            assertNotNull(exampleNumber, "No example number found for type $type")
        }
    }

    @Throws(Exception::class)
    @Test
    fun testShortNumbersValidAndCorrectCost() {
        val invalidStringCases: MutableList<String> = ArrayList()
        for (regionCode in shortNumberSupportedRegions) {
            var exampleShortNumber: String = shortNumberInfo.getExampleShortNumber(regionCode)
            if (!shortNumberInfo.isValidShortNumberForRegion(
                    phoneNumberUtil.parse(exampleShortNumber, regionCode), regionCode
                )
            ) {
                val invalidStringCase = ("region_code: " + regionCode + ", national_number: " + exampleShortNumber)
                invalidStringCases.add(invalidStringCase)
                logger.e("Failed validation for string $invalidStringCase")
            }
            var phoneNumber = phoneNumberUtil.parse(exampleShortNumber, regionCode)
            if (!shortNumberInfo.isValidShortNumber(phoneNumber)) {
                invalidCases.add(phoneNumber)
                logger.e("Failed validation for $phoneNumber")
            }
            for (cost in ShortNumberInfo.ShortNumberCost.values()) {
                exampleShortNumber = shortNumberInfo.getExampleShortNumberForCost(regionCode, cost)
                if (exampleShortNumber != "") {
                    phoneNumber = phoneNumberUtil.parse(exampleShortNumber, regionCode)
                    val exampleShortNumberCost: ShortNumberInfo.ShortNumberCost =
                        shortNumberInfo.getExpectedCostForRegion(phoneNumber, regionCode)
                    if (cost !== exampleShortNumberCost) {
                        wrongTypeCases.add(phoneNumber)
                        logger.e(
                            "Wrong cost for " + phoneNumber.toString() + ": got " + exampleShortNumberCost + ", expected: " + cost
                        )
                    }
                }
            }
        }
        assertEquals(0, invalidStringCases.size)
        assertEquals(0, invalidCases.size)
        assertEquals(0, wrongTypeCases.size)
    }

    @Throws(Exception::class)
    @Test
    fun testEmergency() {
        var wrongTypeCounter = 0
        for (regionCode in shortNumberSupportedRegions) {
            val desc = shortNumberMetadataSource.getMetadataForRegion(regionCode)!!.emergency
            if (desc!!.hasExampleNumber()) {
                val exampleNumber = desc.exampleNumber
                val phoneNumber = phoneNumberUtil.parse(exampleNumber, regionCode)
                if (!shortNumberInfo.isPossibleShortNumberForRegion(
                        phoneNumber, regionCode
                    ) || !shortNumberInfo.isEmergencyNumber(exampleNumber, regionCode)
                ) {
                    wrongTypeCounter++
                    logger.e("Emergency example number test failed for $regionCode")
                } else if (shortNumberInfo.getExpectedCostForRegion(
                        phoneNumber, regionCode
                    ) !== ShortNumberInfo.ShortNumberCost.TOLL_FREE
                ) {
                    wrongTypeCounter++
                    logger.w("Emergency example number not toll free for $regionCode")
                }
            }
        }
        assertEquals(0, wrongTypeCounter)
    }

    @Throws(Exception::class)
    @Test
    fun testCarrierSpecificShortNumbers() {
        var wrongTagCounter = 0
        for (regionCode in shortNumberSupportedRegions) {
            val desc = shortNumberMetadataSource.getMetadataForRegion(regionCode)!!.carrierSpecific
            if (desc!!.hasExampleNumber()) {
                val exampleNumber = desc.exampleNumber
                val carrierSpecificNumber = phoneNumberUtil.parse(exampleNumber, regionCode)
                if (!shortNumberInfo.isPossibleShortNumberForRegion(
                        carrierSpecificNumber, regionCode
                    ) || !shortNumberInfo.isCarrierSpecificForRegion(carrierSpecificNumber, regionCode)
                ) {
                    wrongTagCounter++
                    logger.e("Carrier-specific test failed for $regionCode")
                }
            }
        }
        assertEquals(0, wrongTagCounter)
    }

    @Throws(Exception::class)
    @Test
    fun testSmsServiceShortNumbers() {
        var wrongTagCounter = 0
        for (regionCode in shortNumberSupportedRegions) {
            val desc = shortNumberMetadataSource.getMetadataForRegion(regionCode)!!.smsServices
            if (desc!!.hasExampleNumber()) {
                val exampleNumber = desc.exampleNumber
                val smsServiceNumber = phoneNumberUtil.parse(exampleNumber, regionCode)
                if (!shortNumberInfo.isPossibleShortNumberForRegion(
                        smsServiceNumber, regionCode
                    ) || !shortNumberInfo.isSmsServiceForRegion(smsServiceNumber, regionCode)
                ) {
                    wrongTagCounter++
                    logger.e("SMS service test failed for $regionCode")
                }
            }
        }
        assertEquals(0, wrongTagCounter)
    }

    companion object {
        private val logger = Logger.withTag(ExampleNumbersTest::class.simpleName.toString())
    }
}
