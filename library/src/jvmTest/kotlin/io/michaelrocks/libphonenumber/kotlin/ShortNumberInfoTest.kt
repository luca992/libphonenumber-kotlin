/*
 * Copyright (C) 2013 The Libphonenumber Authors
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

import io.michaelrocks.libphonenumber.kotlin.RegionCode
import io.michaelrocks.libphonenumber.kotlin.TestMetadataTestCase
import io.michaelrocks.libphonenumber.kotlin.PhoneNumberUtil.Companion.createInstance
import io.michaelrocks.libphonenumber.kotlin.Phonenumber.PhoneNumber
import io.michaelrocks.libphonenumber.kotlin.metadata.init.ClassPathResourceMetadataLoader
import junit.framework.TestCase

/**
 * Unit tests for ShortNumberInfo.java
 *
 * @author Shaopeng Jia
 */
class ShortNumberInfoTest : TestMetadataTestCase() {
    override val metadataLoader: MetadataLoader
        get() = ClassPathResourceMetadataLoader()

    private val shortInfo = createInstance(ClassPathResourceMetadataLoader()).shortNumberInfo
    fun testIsPossibleShortNumber() {
        val possibleNumber = PhoneNumber()
        possibleNumber.setCountryCode(33).setNationalNumber(123456L)
        TestCase.assertTrue(shortInfo!!.isPossibleShortNumber(possibleNumber))
        TestCase.assertTrue(
            shortInfo.isPossibleShortNumberForRegion(parse("123456", RegionCode.FR), RegionCode.FR)
        )
        val impossibleNumber = PhoneNumber()
        impossibleNumber.setCountryCode(33).setNationalNumber(9L)
        TestCase.assertFalse(shortInfo.isPossibleShortNumber(impossibleNumber))

        // Note that GB and GG share the country calling code 44, and that this number is possible but
        // not valid.
        TestCase.assertTrue(
            shortInfo.isPossibleShortNumber(
                PhoneNumber().setCountryCode(44).setNationalNumber(11001L)
            )
        )
    }

    fun testIsValidShortNumber() {
        TestCase.assertTrue(
            shortInfo!!.isValidShortNumber(
                PhoneNumber().setCountryCode(33).setNationalNumber(1010L)
            )
        )
        TestCase.assertTrue(shortInfo.isValidShortNumberForRegion(parse("1010", RegionCode.FR), RegionCode.FR))
        TestCase.assertFalse(
            shortInfo.isValidShortNumber(
                PhoneNumber().setCountryCode(33).setNationalNumber(123456L)
            )
        )
        TestCase.assertFalse(
            shortInfo.isValidShortNumberForRegion(parse("123456", RegionCode.FR), RegionCode.FR)
        )

        // Note that GB and GG share the country calling code 44.
        TestCase.assertTrue(
            shortInfo.isValidShortNumber(
                PhoneNumber().setCountryCode(44).setNationalNumber(18001L)
            )
        )
    }

    fun testIsCarrierSpecific() {
        val carrierSpecificNumber = PhoneNumber()
        carrierSpecificNumber.setCountryCode(1).setNationalNumber(33669L)
        TestCase.assertTrue(shortInfo!!.isCarrierSpecific(carrierSpecificNumber))
        TestCase.assertTrue(
            shortInfo.isCarrierSpecificForRegion(parse("33669", RegionCode.US), RegionCode.US)
        )
        val notCarrierSpecificNumber = PhoneNumber()
        notCarrierSpecificNumber.setCountryCode(1).setNationalNumber(911L)
        TestCase.assertFalse(shortInfo.isCarrierSpecific(notCarrierSpecificNumber))
        TestCase.assertFalse(
            shortInfo.isCarrierSpecificForRegion(parse("911", RegionCode.US), RegionCode.US)
        )
        val carrierSpecificNumberForSomeRegion = PhoneNumber()
        carrierSpecificNumberForSomeRegion.setCountryCode(1).setNationalNumber(211L)
        TestCase.assertTrue(shortInfo.isCarrierSpecific(carrierSpecificNumberForSomeRegion))
        TestCase.assertTrue(
            shortInfo.isCarrierSpecificForRegion(carrierSpecificNumberForSomeRegion, RegionCode.US)
        )
        TestCase.assertFalse(
            shortInfo.isCarrierSpecificForRegion(carrierSpecificNumberForSomeRegion, RegionCode.BB)
        )
    }

    fun testIsSmsService() {
        val smsServiceNumberForSomeRegion = PhoneNumber()
        smsServiceNumberForSomeRegion.setCountryCode(1).setNationalNumber(21234L)
        TestCase.assertTrue(shortInfo!!.isSmsServiceForRegion(smsServiceNumberForSomeRegion, RegionCode.US))
        TestCase.assertFalse(shortInfo.isSmsServiceForRegion(smsServiceNumberForSomeRegion, RegionCode.BB))
    }

    fun testGetExpectedCost() {
        val premiumRateExample = shortInfo!!.getExampleShortNumberForCost(
            RegionCode.FR,
            ShortNumberInfo.ShortNumberCost.PREMIUM_RATE
        )
        TestCase.assertEquals(
            ShortNumberInfo.ShortNumberCost.PREMIUM_RATE, shortInfo.getExpectedCostForRegion(
                parse(premiumRateExample, RegionCode.FR), RegionCode.FR
            )
        )
        val premiumRateNumber = PhoneNumber()
        premiumRateNumber.setCountryCode(33).setNationalNumber(premiumRateExample.toInt().toLong())
        TestCase.assertEquals(
            ShortNumberInfo.ShortNumberCost.PREMIUM_RATE,
            shortInfo.getExpectedCost(premiumRateNumber)
        )
        val standardRateExample = shortInfo.getExampleShortNumberForCost(
            RegionCode.FR,
            ShortNumberInfo.ShortNumberCost.STANDARD_RATE
        )
        TestCase.assertEquals(
            ShortNumberInfo.ShortNumberCost.STANDARD_RATE, shortInfo.getExpectedCostForRegion(
                parse(standardRateExample, RegionCode.FR), RegionCode.FR
            )
        )
        val standardRateNumber = PhoneNumber()
        standardRateNumber.setCountryCode(33).setNationalNumber(standardRateExample.toInt().toLong())
        TestCase.assertEquals(
            ShortNumberInfo.ShortNumberCost.STANDARD_RATE,
            shortInfo.getExpectedCost(standardRateNumber)
        )
        val tollFreeExample = shortInfo.getExampleShortNumberForCost(
            RegionCode.FR,
            ShortNumberInfo.ShortNumberCost.TOLL_FREE
        )
        TestCase.assertEquals(
            ShortNumberInfo.ShortNumberCost.TOLL_FREE,
            shortInfo.getExpectedCostForRegion(parse(tollFreeExample, RegionCode.FR), RegionCode.FR)
        )
        val tollFreeNumber = PhoneNumber()
        tollFreeNumber.setCountryCode(33).setNationalNumber(tollFreeExample.toInt().toLong())
        TestCase.assertEquals(
            ShortNumberInfo.ShortNumberCost.TOLL_FREE,
            shortInfo.getExpectedCost(tollFreeNumber)
        )
        TestCase.assertEquals(
            ShortNumberInfo.ShortNumberCost.UNKNOWN_COST,
            shortInfo.getExpectedCostForRegion(parse("12345", RegionCode.FR), RegionCode.FR)
        )
        val unknownCostNumber = PhoneNumber()
        unknownCostNumber.setCountryCode(33).setNationalNumber(12345L)
        TestCase.assertEquals(
            ShortNumberInfo.ShortNumberCost.UNKNOWN_COST,
            shortInfo.getExpectedCost(unknownCostNumber)
        )

        // Test that an invalid number may nevertheless have a cost other than UNKNOWN_COST.
        TestCase.assertFalse(
            shortInfo.isValidShortNumberForRegion(parse("116123", RegionCode.FR), RegionCode.FR)
        )
        TestCase.assertEquals(
            ShortNumberInfo.ShortNumberCost.TOLL_FREE,
            shortInfo.getExpectedCostForRegion(parse("116123", RegionCode.FR), RegionCode.FR)
        )
        val invalidNumber = PhoneNumber()
        invalidNumber.setCountryCode(33).setNationalNumber(116123L)
        TestCase.assertFalse(shortInfo.isValidShortNumber(invalidNumber))
        TestCase.assertEquals(
            ShortNumberInfo.ShortNumberCost.TOLL_FREE,
            shortInfo.getExpectedCost(invalidNumber)
        )

        // Test a nonexistent country code.
        TestCase.assertEquals(
            ShortNumberInfo.ShortNumberCost.UNKNOWN_COST,
            shortInfo.getExpectedCostForRegion(parse("911", RegionCode.US), RegionCode.ZZ)
        )
        unknownCostNumber.clear()
        unknownCostNumber.setCountryCode(123).setNationalNumber(911L)
        TestCase.assertEquals(
            ShortNumberInfo.ShortNumberCost.UNKNOWN_COST,
            shortInfo.getExpectedCost(unknownCostNumber)
        )
    }

    fun testGetExpectedCostForSharedCountryCallingCode() {
        // Test some numbers which have different costs in countries sharing the same country calling
        // code. In Australia, 1234 is premium-rate, 1194 is standard-rate, and 733 is toll-free. These
        // are not known to be valid numbers in the Christmas Islands.
        val ambiguousPremiumRateString = "1234"
        val ambiguousPremiumRateNumber = PhoneNumber().setCountryCode(61).setNationalNumber(1234L)
        val ambiguousStandardRateString = "1194"
        val ambiguousStandardRateNumber = PhoneNumber().setCountryCode(61).setNationalNumber(1194L)
        val ambiguousTollFreeString = "733"
        val ambiguousTollFreeNumber = PhoneNumber().setCountryCode(61).setNationalNumber(733L)
        TestCase.assertTrue(shortInfo!!.isValidShortNumber(ambiguousPremiumRateNumber))
        TestCase.assertTrue(shortInfo.isValidShortNumber(ambiguousStandardRateNumber))
        TestCase.assertTrue(shortInfo.isValidShortNumber(ambiguousTollFreeNumber))
        TestCase.assertTrue(
            shortInfo.isValidShortNumberForRegion(
                parse(ambiguousPremiumRateString, RegionCode.AU), RegionCode.AU
            )
        )
        TestCase.assertEquals(
            ShortNumberInfo.ShortNumberCost.PREMIUM_RATE, shortInfo.getExpectedCostForRegion(
                parse(ambiguousPremiumRateString, RegionCode.AU), RegionCode.AU
            )
        )
        TestCase.assertFalse(
            shortInfo.isValidShortNumberForRegion(
                parse(ambiguousPremiumRateString, RegionCode.CX), RegionCode.CX
            )
        )
        TestCase.assertEquals(
            ShortNumberInfo.ShortNumberCost.UNKNOWN_COST, shortInfo.getExpectedCostForRegion(
                parse(ambiguousPremiumRateString, RegionCode.CX), RegionCode.CX
            )
        )
        // PREMIUM_RATE takes precedence over UNKNOWN_COST.
        TestCase.assertEquals(
            ShortNumberInfo.ShortNumberCost.PREMIUM_RATE,
            shortInfo.getExpectedCost(ambiguousPremiumRateNumber)
        )
        TestCase.assertTrue(
            shortInfo.isValidShortNumberForRegion(
                parse(ambiguousStandardRateString, RegionCode.AU), RegionCode.AU
            )
        )
        TestCase.assertEquals(
            ShortNumberInfo.ShortNumberCost.STANDARD_RATE, shortInfo.getExpectedCostForRegion(
                parse(ambiguousStandardRateString, RegionCode.AU), RegionCode.AU
            )
        )
        TestCase.assertFalse(
            shortInfo.isValidShortNumberForRegion(
                parse(ambiguousStandardRateString, RegionCode.CX), RegionCode.CX
            )
        )
        TestCase.assertEquals(
            ShortNumberInfo.ShortNumberCost.UNKNOWN_COST, shortInfo.getExpectedCostForRegion(
                parse(ambiguousStandardRateString, RegionCode.CX), RegionCode.CX
            )
        )
        TestCase.assertEquals(
            ShortNumberInfo.ShortNumberCost.UNKNOWN_COST,
            shortInfo.getExpectedCost(ambiguousStandardRateNumber)
        )
        TestCase.assertTrue(
            shortInfo.isValidShortNumberForRegion(
                parse(ambiguousTollFreeString, RegionCode.AU),
                RegionCode.AU
            )
        )
        TestCase.assertEquals(
            ShortNumberInfo.ShortNumberCost.TOLL_FREE, shortInfo.getExpectedCostForRegion(
                parse(ambiguousTollFreeString, RegionCode.AU), RegionCode.AU
            )
        )
        TestCase.assertFalse(
            shortInfo.isValidShortNumberForRegion(
                parse(ambiguousTollFreeString, RegionCode.CX),
                RegionCode.CX
            )
        )
        TestCase.assertEquals(
            ShortNumberInfo.ShortNumberCost.UNKNOWN_COST, shortInfo.getExpectedCostForRegion(
                parse(ambiguousTollFreeString, RegionCode.CX), RegionCode.CX
            )
        )
        TestCase.assertEquals(
            ShortNumberInfo.ShortNumberCost.UNKNOWN_COST,
            shortInfo.getExpectedCost(ambiguousTollFreeNumber)
        )
    }

    fun testExampleShortNumberPresence() {
        TestCase.assertFalse(shortInfo!!.getExampleShortNumber(RegionCode.AD).isEmpty())
        TestCase.assertFalse(shortInfo.getExampleShortNumber(RegionCode.FR).isEmpty())
        TestCase.assertTrue(shortInfo.getExampleShortNumber(RegionCode.UN001).isEmpty())
        TestCase.assertTrue(shortInfo.getExampleShortNumber(null).isEmpty())
    }

    fun testConnectsToEmergencyNumber_US() {
        TestCase.assertTrue(shortInfo!!.connectsToEmergencyNumber("911", RegionCode.US))
        TestCase.assertTrue(shortInfo.connectsToEmergencyNumber("112", RegionCode.US))
        TestCase.assertFalse(shortInfo.connectsToEmergencyNumber("999", RegionCode.US))
    }

    fun testConnectsToEmergencyNumberLongNumber_US() {
        TestCase.assertTrue(shortInfo!!.connectsToEmergencyNumber("9116666666", RegionCode.US))
        TestCase.assertTrue(shortInfo.connectsToEmergencyNumber("1126666666", RegionCode.US))
        TestCase.assertFalse(shortInfo.connectsToEmergencyNumber("9996666666", RegionCode.US))
    }

    fun testConnectsToEmergencyNumberWithFormatting_US() {
        TestCase.assertTrue(shortInfo!!.connectsToEmergencyNumber("9-1-1", RegionCode.US))
        TestCase.assertTrue(shortInfo.connectsToEmergencyNumber("1-1-2", RegionCode.US))
        TestCase.assertFalse(shortInfo.connectsToEmergencyNumber("9-9-9", RegionCode.US))
    }

    fun testConnectsToEmergencyNumberWithPlusSign_US() {
        TestCase.assertFalse(shortInfo!!.connectsToEmergencyNumber("+911", RegionCode.US))
        TestCase.assertFalse(shortInfo.connectsToEmergencyNumber("\uFF0B911", RegionCode.US))
        TestCase.assertFalse(shortInfo.connectsToEmergencyNumber(" +911", RegionCode.US))
        TestCase.assertFalse(shortInfo.connectsToEmergencyNumber("+112", RegionCode.US))
        TestCase.assertFalse(shortInfo.connectsToEmergencyNumber("+999", RegionCode.US))
    }

    fun testConnectsToEmergencyNumber_BR() {
        TestCase.assertTrue(shortInfo!!.connectsToEmergencyNumber("911", RegionCode.BR))
        TestCase.assertTrue(shortInfo.connectsToEmergencyNumber("190", RegionCode.BR))
        TestCase.assertFalse(shortInfo.connectsToEmergencyNumber("999", RegionCode.BR))
    }

    fun testConnectsToEmergencyNumberLongNumber_BR() {
        // Brazilian emergency numbers don't work when additional digits are appended.
        TestCase.assertFalse(shortInfo!!.connectsToEmergencyNumber("9111", RegionCode.BR))
        TestCase.assertFalse(shortInfo.connectsToEmergencyNumber("1900", RegionCode.BR))
        TestCase.assertFalse(shortInfo.connectsToEmergencyNumber("9996", RegionCode.BR))
    }

    fun testConnectsToEmergencyNumber_CL() {
        TestCase.assertTrue(shortInfo!!.connectsToEmergencyNumber("131", RegionCode.CL))
        TestCase.assertTrue(shortInfo.connectsToEmergencyNumber("133", RegionCode.CL))
    }

    fun testConnectsToEmergencyNumberLongNumber_CL() {
        // Chilean emergency numbers don't work when additional digits are appended.
        TestCase.assertFalse(shortInfo!!.connectsToEmergencyNumber("1313", RegionCode.CL))
        TestCase.assertFalse(shortInfo.connectsToEmergencyNumber("1330", RegionCode.CL))
    }

    fun testConnectsToEmergencyNumber_AO() {
        // Angola doesn't have any metadata for emergency numbers in the test metadata.
        TestCase.assertFalse(shortInfo!!.connectsToEmergencyNumber("911", RegionCode.AO))
        TestCase.assertFalse(shortInfo.connectsToEmergencyNumber("222123456", RegionCode.AO))
        TestCase.assertFalse(shortInfo.connectsToEmergencyNumber("923123456", RegionCode.AO))
    }

    fun testConnectsToEmergencyNumber_ZW() {
        // Zimbabwe doesn't have any metadata in the test metadata.
        TestCase.assertFalse(shortInfo!!.connectsToEmergencyNumber("911", RegionCode.ZW))
        TestCase.assertFalse(shortInfo.connectsToEmergencyNumber("01312345", RegionCode.ZW))
        TestCase.assertFalse(shortInfo.connectsToEmergencyNumber("0711234567", RegionCode.ZW))
    }

    fun testIsEmergencyNumber_US() {
        TestCase.assertTrue(shortInfo!!.isEmergencyNumber("911", RegionCode.US))
        TestCase.assertTrue(shortInfo.isEmergencyNumber("112", RegionCode.US))
        TestCase.assertFalse(shortInfo.isEmergencyNumber("999", RegionCode.US))
    }

    fun testIsEmergencyNumberLongNumber_US() {
        TestCase.assertFalse(shortInfo!!.isEmergencyNumber("9116666666", RegionCode.US))
        TestCase.assertFalse(shortInfo.isEmergencyNumber("1126666666", RegionCode.US))
        TestCase.assertFalse(shortInfo.isEmergencyNumber("9996666666", RegionCode.US))
    }

    fun testIsEmergencyNumberWithFormatting_US() {
        TestCase.assertTrue(shortInfo!!.isEmergencyNumber("9-1-1", RegionCode.US))
        TestCase.assertTrue(shortInfo.isEmergencyNumber("*911", RegionCode.US))
        TestCase.assertTrue(shortInfo.isEmergencyNumber("1-1-2", RegionCode.US))
        TestCase.assertTrue(shortInfo.isEmergencyNumber("*112", RegionCode.US))
        TestCase.assertFalse(shortInfo.isEmergencyNumber("9-9-9", RegionCode.US))
        TestCase.assertFalse(shortInfo.isEmergencyNumber("*999", RegionCode.US))
    }

    fun testIsEmergencyNumberWithPlusSign_US() {
        TestCase.assertFalse(shortInfo!!.isEmergencyNumber("+911", RegionCode.US))
        TestCase.assertFalse(shortInfo.isEmergencyNumber("\uFF0B911", RegionCode.US))
        TestCase.assertFalse(shortInfo.isEmergencyNumber(" +911", RegionCode.US))
        TestCase.assertFalse(shortInfo.isEmergencyNumber("+112", RegionCode.US))
        TestCase.assertFalse(shortInfo.isEmergencyNumber("+999", RegionCode.US))
    }

    fun testIsEmergencyNumber_BR() {
        TestCase.assertTrue(shortInfo!!.isEmergencyNumber("911", RegionCode.BR))
        TestCase.assertTrue(shortInfo.isEmergencyNumber("190", RegionCode.BR))
        TestCase.assertFalse(shortInfo.isEmergencyNumber("999", RegionCode.BR))
    }

    fun testIsEmergencyNumberLongNumber_BR() {
        TestCase.assertFalse(shortInfo!!.isEmergencyNumber("9111", RegionCode.BR))
        TestCase.assertFalse(shortInfo.isEmergencyNumber("1900", RegionCode.BR))
        TestCase.assertFalse(shortInfo.isEmergencyNumber("9996", RegionCode.BR))
    }

    fun testIsEmergencyNumber_AO() {
        // Angola doesn't have any metadata for emergency numbers in the test metadata.
        TestCase.assertFalse(shortInfo!!.isEmergencyNumber("911", RegionCode.AO))
        TestCase.assertFalse(shortInfo.isEmergencyNumber("222123456", RegionCode.AO))
        TestCase.assertFalse(shortInfo.isEmergencyNumber("923123456", RegionCode.AO))
    }

    fun testIsEmergencyNumber_ZW() {
        // Zimbabwe doesn't have any metadata in the test metadata.
        TestCase.assertFalse(shortInfo!!.isEmergencyNumber("911", RegionCode.ZW))
        TestCase.assertFalse(shortInfo.isEmergencyNumber("01312345", RegionCode.ZW))
        TestCase.assertFalse(shortInfo.isEmergencyNumber("0711234567", RegionCode.ZW))
    }

    fun testEmergencyNumberForSharedCountryCallingCode() {
        // Test the emergency number 112, which is valid in both Australia and the Christmas Islands.
        TestCase.assertTrue(shortInfo!!.isEmergencyNumber("112", RegionCode.AU))
        TestCase.assertTrue(shortInfo.isValidShortNumberForRegion(parse("112", RegionCode.AU), RegionCode.AU))
        TestCase.assertEquals(
            ShortNumberInfo.ShortNumberCost.TOLL_FREE,
            shortInfo.getExpectedCostForRegion(parse("112", RegionCode.AU), RegionCode.AU)
        )
        TestCase.assertTrue(shortInfo.isEmergencyNumber("112", RegionCode.CX))
        TestCase.assertTrue(shortInfo.isValidShortNumberForRegion(parse("112", RegionCode.CX), RegionCode.CX))
        TestCase.assertEquals(
            ShortNumberInfo.ShortNumberCost.TOLL_FREE,
            shortInfo.getExpectedCostForRegion(parse("112", RegionCode.CX), RegionCode.CX)
        )
        val sharedEmergencyNumber = PhoneNumber().setCountryCode(61).setNationalNumber(112L)
        TestCase.assertTrue(shortInfo.isValidShortNumber(sharedEmergencyNumber))
        TestCase.assertEquals(
            ShortNumberInfo.ShortNumberCost.TOLL_FREE,
            shortInfo.getExpectedCost(sharedEmergencyNumber)
        )
    }

    fun testOverlappingNANPANumber() {
        // 211 is an emergency number in Barbados, while it is a toll-free information line in Canada
        // and the USA.
        TestCase.assertTrue(shortInfo!!.isEmergencyNumber("211", RegionCode.BB))
        TestCase.assertEquals(
            ShortNumberInfo.ShortNumberCost.TOLL_FREE,
            shortInfo.getExpectedCostForRegion(parse("211", RegionCode.BB), RegionCode.BB)
        )
        TestCase.assertFalse(shortInfo.isEmergencyNumber("211", RegionCode.US))
        TestCase.assertEquals(
            ShortNumberInfo.ShortNumberCost.UNKNOWN_COST,
            shortInfo.getExpectedCostForRegion(parse("211", RegionCode.US), RegionCode.US)
        )
        TestCase.assertFalse(shortInfo.isEmergencyNumber("211", RegionCode.CA))
        TestCase.assertEquals(
            ShortNumberInfo.ShortNumberCost.TOLL_FREE,
            shortInfo.getExpectedCostForRegion(parse("211", RegionCode.CA), RegionCode.CA)
        )
    }

    fun testCountryCallingCodeIsNotIgnored() {
        // +46 is the country calling code for Sweden (SE), and 40404 is a valid short number in the US.
        TestCase.assertFalse(
            shortInfo!!.isPossibleShortNumberForRegion(
                parse("+4640404", RegionCode.SE), RegionCode.US
            )
        )
        TestCase.assertFalse(
            shortInfo.isValidShortNumberForRegion(
                parse("+4640404", RegionCode.SE), RegionCode.US
            )
        )
        TestCase.assertEquals(
            ShortNumberInfo.ShortNumberCost.UNKNOWN_COST,
            shortInfo.getExpectedCostForRegion(
                parse("+4640404", RegionCode.SE), RegionCode.US
            )
        )
    }

    private fun parse(number: String, regionCode: String): PhoneNumber {
        return try {
            phoneUtil.parse(number, regionCode)
        } catch (e: NumberParseException) {
            throw AssertionError(
                "Test input data should always parse correctly: $number ($regionCode)", e
            )
        }
    }

}
