///*
// * Copyright (C) 2009 The Libphonenumber Authors
// * Copyright (C) 2022 Michael Rozumyanskiy
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// * http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//package io.michaelrocks.libphonenumber.android
//
//import io.michaelrocks.libphonenumber.kotlin.NumberParseException
//import io.michaelrocks.libphonenumber.kotlin.PhoneNumberUtil
//import io.michaelrocks.libphonenumber.kotlin.PhoneNumberUtil.PhoneNumberType
//import io.michaelrocks.libphonenumber.kotlin.Phonenumber.PhoneNumber
//import io.michaelrocks.libphonenumber.kotlin.ShortNumberInfo
//import io.michaelrocks.libphonenumber.kotlin.ShortNumbersRegionCodeSet
//import io.michaelrocks.libphonenumber.kotlin.metadata.DefaultMetadataDependenciesProvider
//import junit.framework.TestCase
//import java.util.*
//import java.util.logging.Level
//import java.util.logging.Logger
//
///**
// * Verifies all of the example numbers in the metadata are valid and of the correct type. If no
// * example number exists for a particular type, the test still passes since not all types are
// * relevant for all regions. Tests that check the XML schema will ensure that an exampleNumber
// * node is present for every phone number description.
// */
//class ExampleNumbersTest : TestCase() {
//    private val metadataDependenciesProvider = DefaultMetadataDependenciesProvider(null)
//    private val phoneNumberUtil = PhoneNumberUtil.createInstance(metadataDependenciesProvider.metadataLoader)
//    private val shortNumberInfo: ShortNumberInfo = phoneNumberUtil.shortNumberInfo!!
//    private val shortNumberMetadataSource = metadataDependenciesProvider.shortNumberMetadataSource
//    private val invalidCases: MutableList<PhoneNumber?> = ArrayList()
//    private val wrongTypeCases: MutableList<PhoneNumber> = ArrayList()
//    private val shortNumberSupportedRegions: Set<String> = ShortNumbersRegionCodeSet.regionCodeSet
//
//    /**
//     * @param exampleNumberRequestedType type we are requesting an example number for
//     * @param possibleExpectedTypes      acceptable types that this number should match, such as
//     * FIXED_LINE and FIXED_LINE_OR_MOBILE for a fixed line example number.
//     */
//    private fun checkNumbersValidAndCorrectType(
//        exampleNumberRequestedType: PhoneNumberType,
//        possibleExpectedTypes: Set<PhoneNumberType>
//    ) {
//        for (regionCode in phoneNumberUtil.getSupportedRegions()) {
//            val exampleNumber = phoneNumberUtil.getExampleNumberForType(regionCode, exampleNumberRequestedType)
//            if (exampleNumber != null) {
//                if (!phoneNumberUtil.isValidNumber(exampleNumber)) {
//                    invalidCases.add(exampleNumber)
//                    logger.log(Level.SEVERE, "Failed validation for $exampleNumber")
//                } else {
//                    // We know the number is valid, now we check the type.
//                    val exampleNumberType = phoneNumberUtil.getNumberType(exampleNumber)
//                    if (!possibleExpectedTypes.contains(exampleNumberType)) {
//                        wrongTypeCases.add(exampleNumber)
//                        logger.log(
//                            Level.SEVERE, "Wrong type for "
//                                    + exampleNumber
//                                    + ": got " + exampleNumberType
//                        )
//                        logger.log(Level.WARNING, "Expected types: ")
//                        for (type in possibleExpectedTypes) {
//                            logger.log(Level.WARNING, type.toString())
//                        }
//                    }
//                }
//            }
//        }
//    }
//
//    fun testFixedLine() {
//        val fixedLineTypes: Set<PhoneNumberType> = EnumSet.of(
//            PhoneNumberType.FIXED_LINE,
//            PhoneNumberType.FIXED_LINE_OR_MOBILE
//        )
//        checkNumbersValidAndCorrectType(PhoneNumberType.FIXED_LINE, fixedLineTypes)
//        assertEquals(0, invalidCases.size)
//        assertEquals(0, wrongTypeCases.size)
//    }
//
//    fun testMobile() {
//        val mobileTypes: Set<PhoneNumberType> = EnumSet.of(
//            PhoneNumberType.MOBILE,
//            PhoneNumberType.FIXED_LINE_OR_MOBILE
//        )
//        checkNumbersValidAndCorrectType(PhoneNumberType.MOBILE, mobileTypes)
//        assertEquals(0, invalidCases.size)
//        assertEquals(0, wrongTypeCases.size)
//    }
//
//    fun testTollFree() {
//        val tollFreeTypes: Set<PhoneNumberType> = EnumSet.of(PhoneNumberType.TOLL_FREE)
//        checkNumbersValidAndCorrectType(PhoneNumberType.TOLL_FREE, tollFreeTypes)
//        assertEquals(0, invalidCases.size)
//        assertEquals(0, wrongTypeCases.size)
//    }
//
//    fun testPremiumRate() {
//        val premiumRateTypes: Set<PhoneNumberType> = EnumSet.of(PhoneNumberType.PREMIUM_RATE)
//        checkNumbersValidAndCorrectType(PhoneNumberType.PREMIUM_RATE, premiumRateTypes)
//        assertEquals(0, invalidCases.size)
//        assertEquals(0, wrongTypeCases.size)
//    }
//
//    fun testVoip() {
//        val voipTypes: Set<PhoneNumberType> = EnumSet.of(PhoneNumberType.VOIP)
//        checkNumbersValidAndCorrectType(PhoneNumberType.VOIP, voipTypes)
//        assertEquals(0, invalidCases.size)
//        assertEquals(0, wrongTypeCases.size)
//    }
//
//    fun testPager() {
//        val pagerTypes: Set<PhoneNumberType> = EnumSet.of(PhoneNumberType.PAGER)
//        checkNumbersValidAndCorrectType(PhoneNumberType.PAGER, pagerTypes)
//        assertEquals(0, invalidCases.size)
//        assertEquals(0, wrongTypeCases.size)
//    }
//
//    fun testUan() {
//        val uanTypes: Set<PhoneNumberType> = EnumSet.of(PhoneNumberType.UAN)
//        checkNumbersValidAndCorrectType(PhoneNumberType.UAN, uanTypes)
//        assertEquals(0, invalidCases.size)
//        assertEquals(0, wrongTypeCases.size)
//    }
//
//    fun testVoicemail() {
//        val voicemailTypes: Set<PhoneNumberType> = EnumSet.of(PhoneNumberType.VOICEMAIL)
//        checkNumbersValidAndCorrectType(PhoneNumberType.VOICEMAIL, voicemailTypes)
//        assertEquals(0, invalidCases.size)
//        assertEquals(0, wrongTypeCases.size)
//    }
//
//    fun testSharedCost() {
//        val sharedCostTypes: Set<PhoneNumberType> = EnumSet.of(PhoneNumberType.SHARED_COST)
//        checkNumbersValidAndCorrectType(PhoneNumberType.SHARED_COST, sharedCostTypes)
//        assertEquals(0, invalidCases.size)
//        assertEquals(0, wrongTypeCases.size)
//    }
//
//    fun testCanBeInternationallyDialled() {
//        for (regionCode in phoneNumberUtil.getSupportedRegions()) {
//            var exampleNumber: PhoneNumber? = null
//            val desc = phoneNumberUtil.getMetadataForRegion(regionCode)!!.noInternationalDialling
//            try {
//                if (desc!!.hasExampleNumber()) {
//                    exampleNumber = phoneNumberUtil.parse(desc.exampleNumber, regionCode)
//                }
//            } catch (e: NumberParseException) {
//                logger.log(Level.SEVERE, e.toString())
//            }
//            if (exampleNumber != null && phoneNumberUtil.canBeInternationallyDialled(exampleNumber)) {
//                wrongTypeCases.add(exampleNumber)
//                logger.log(
//                    Level.SEVERE, "Number " + exampleNumber
//                            + " should not be internationally diallable"
//                )
//            }
//        }
//        assertEquals(0, wrongTypeCases.size)
//    }
//
//    fun testGlobalNetworkNumbers() {
//        for (callingCode in phoneNumberUtil.supportedGlobalNetworkCallingCodes) {
//            val exampleNumber = phoneNumberUtil.getExampleNumberForNonGeoEntity(callingCode)
//            assertNotNull("No example phone number for calling code $callingCode", exampleNumber)
//            if (!phoneNumberUtil.isValidNumber(exampleNumber!!)) {
//                invalidCases.add(exampleNumber)
//                logger.log(Level.SEVERE, "Failed validation for $exampleNumber")
//            }
//        }
//        assertEquals(0, invalidCases.size)
//    }
//
//    fun testEveryRegionHasAnExampleNumber() {
//        for (regionCode in phoneNumberUtil.getSupportedRegions()) {
//            val exampleNumber = phoneNumberUtil.getExampleNumber(regionCode)
//            assertNotNull("No example number found for region $regionCode", exampleNumber)
//        }
//    }
//
//    fun testEveryRegionHasAnInvalidExampleNumber() {
//        for (regionCode in phoneNumberUtil.getSupportedRegions()) {
//            val exampleNumber = phoneNumberUtil.getInvalidExampleNumber(regionCode)
//            assertNotNull("No invalid example number found for region $regionCode", exampleNumber)
//        }
//    }
//
//    fun testEveryTypeHasAnExampleNumber() {
//        for (type in PhoneNumberType.entries.toTypedArray()) {
//            if (type === PhoneNumberType.UNKNOWN) {
//                continue
//            }
//            val exampleNumber = phoneNumberUtil.getExampleNumberForType(type)
//            assertNotNull("No example number found for type $type", exampleNumber)
//        }
//    }
//
//    @Throws(Exception::class)
//    fun testShortNumbersValidAndCorrectCost() {
//        val invalidStringCases: MutableList<String> = ArrayList()
//        for (regionCode in shortNumberSupportedRegions) {
//            var exampleShortNumber: String = shortNumberInfo.getExampleShortNumber(regionCode)
//            if (!shortNumberInfo.isValidShortNumberForRegion(
//                    phoneNumberUtil.parse(exampleShortNumber, regionCode), regionCode
//                )
//            ) {
//                val invalidStringCase = ("region_code: " + regionCode + ", national_number: "
//                        + exampleShortNumber)
//                invalidStringCases.add(invalidStringCase)
//                logger.log(Level.SEVERE, "Failed validation for string $invalidStringCase")
//            }
//            var phoneNumber = phoneNumberUtil.parse(exampleShortNumber, regionCode)
//            if (!shortNumberInfo.isValidShortNumber(phoneNumber)) {
//                invalidCases.add(phoneNumber)
//                logger.log(Level.SEVERE, "Failed validation for $phoneNumber")
//            }
//            for (cost in ShortNumberInfo.ShortNumberCost.values()) {
//                exampleShortNumber = shortNumberInfo.getExampleShortNumberForCost(regionCode, cost)
//                if (exampleShortNumber != "") {
//                    phoneNumber = phoneNumberUtil.parse(exampleShortNumber, regionCode)
//                    val exampleShortNumberCost: ShortNumberInfo.ShortNumberCost =
//                        shortNumberInfo.getExpectedCostForRegion(phoneNumber, regionCode)
//                    if (cost !== exampleShortNumberCost) {
//                        wrongTypeCases.add(phoneNumber)
//                        logger.log(
//                            Level.SEVERE, "Wrong cost for " + phoneNumber.toString()
//                                    + ": got " + exampleShortNumberCost
//                                    + ", expected: " + cost
//                        )
//                    }
//                }
//            }
//        }
//        assertEquals(0, invalidStringCases.size)
//        assertEquals(0, invalidCases.size)
//        assertEquals(0, wrongTypeCases.size)
//    }
//
//    @Throws(Exception::class)
//    fun testEmergency() {
//        var wrongTypeCounter = 0
//        for (regionCode in shortNumberSupportedRegions) {
//            val desc = shortNumberMetadataSource.getMetadataForRegion(regionCode)!!.emergency
//            if (desc!!.hasExampleNumber()) {
//                val exampleNumber = desc.exampleNumber
//                val phoneNumber = phoneNumberUtil.parse(exampleNumber, regionCode)
//                if (!shortNumberInfo.isPossibleShortNumberForRegion(phoneNumber, regionCode)
//                    || !shortNumberInfo.isEmergencyNumber(exampleNumber, regionCode)
//                ) {
//                    wrongTypeCounter++
//                    logger.log(Level.SEVERE, "Emergency example number test failed for $regionCode")
//                } else if (shortNumberInfo.getExpectedCostForRegion(phoneNumber, regionCode)
//                    !== ShortNumberInfo.ShortNumberCost.TOLL_FREE
//                ) {
//                    wrongTypeCounter++
//                    logger.log(Level.WARNING, "Emergency example number not toll free for $regionCode")
//                }
//            }
//        }
//        assertEquals(0, wrongTypeCounter)
//    }
//
//    @Throws(Exception::class)
//    fun testCarrierSpecificShortNumbers() {
//        var wrongTagCounter = 0
//        for (regionCode in shortNumberSupportedRegions) {
//            val desc = shortNumberMetadataSource.getMetadataForRegion(regionCode)!!.carrierSpecific
//            if (desc!!.hasExampleNumber()) {
//                val exampleNumber = desc.exampleNumber
//                val carrierSpecificNumber = phoneNumberUtil.parse(exampleNumber, regionCode)
//                if (!shortNumberInfo.isPossibleShortNumberForRegion(carrierSpecificNumber, regionCode)
//                    || !shortNumberInfo.isCarrierSpecificForRegion(carrierSpecificNumber, regionCode)
//                ) {
//                    wrongTagCounter++
//                    logger.log(Level.SEVERE, "Carrier-specific test failed for $regionCode")
//                }
//            }
//        }
//        assertEquals(0, wrongTagCounter)
//    }
//
//    @Throws(Exception::class)
//    fun testSmsServiceShortNumbers() {
//        var wrongTagCounter = 0
//        for (regionCode in shortNumberSupportedRegions) {
//            val desc = shortNumberMetadataSource.getMetadataForRegion(regionCode)!!.smsServices
//            if (desc!!.hasExampleNumber()) {
//                val exampleNumber = desc.exampleNumber
//                val smsServiceNumber = phoneNumberUtil.parse(exampleNumber, regionCode)
//                if (!shortNumberInfo.isPossibleShortNumberForRegion(smsServiceNumber, regionCode)
//                    || !shortNumberInfo.isSmsServiceForRegion(smsServiceNumber, regionCode)
//                ) {
//                    wrongTagCounter++
//                    logger.log(Level.SEVERE, "SMS service test failed for $regionCode")
//                }
//            }
//        }
//        assertEquals(0, wrongTagCounter)
//    }
//
//    companion object {
//        private val logger = Logger.getLogger(ExampleNumbersTest::class.java.getName())
//    }
//}
