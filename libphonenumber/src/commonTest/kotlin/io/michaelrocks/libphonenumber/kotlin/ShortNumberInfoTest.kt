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

import io.michaelrocks.libphonenumber.kotlin.PhoneNumberUtil.Companion.createInstance
import io.michaelrocks.libphonenumber.kotlin.Phonenumber.PhoneNumber
import io.michaelrocks.libphonenumber.kotlin.metadata.defaultMetadataLoader
import io.michaelrocks.libphonenumber.kotlin.utils.RegionCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for ShortNumberInfo.java
 *
 * @author Shaopeng Jia
 */
class ShortNumberInfoTest : TestMetadataTestCase() {
    override val metadataLoader: MetadataLoader
        get() = defaultMetadataLoader()

    private val shortInfo = createInstance(metadataLoader).shortNumberInfo

    @Test
    fun testIsPossibleShortNumber() {
        val possibleNumber = PhoneNumber()
        possibleNumber.setCountryCode(33).setNationalNumber(123456L)
        assertTrue(shortInfo!!.isPossibleShortNumber(possibleNumber))
        assertTrue(
            shortInfo.isPossibleShortNumberForRegion(parse("123456", RegionCode.FR), RegionCode.FR)
        )
        val impossibleNumber = PhoneNumber()
        impossibleNumber.setCountryCode(33).setNationalNumber(9L)
        assertFalse(shortInfo.isPossibleShortNumber(impossibleNumber))

        // Note that GB and GG share the country calling code 44, and that this number is possible but
        // not valid.
        assertTrue(
            shortInfo.isPossibleShortNumber(
                PhoneNumber().setCountryCode(44).setNationalNumber(11001L)
            )
        )
    }

    @Test
    fun testIsValidShortNumber() {
        assertTrue(
            shortInfo!!.isValidShortNumber(
                PhoneNumber().setCountryCode(33).setNationalNumber(1010L)
            )
        )
        assertTrue(shortInfo.isValidShortNumberForRegion(parse("1010", RegionCode.FR), RegionCode.FR))
        assertFalse(
            shortInfo.isValidShortNumber(
                PhoneNumber().setCountryCode(33).setNationalNumber(123456L)
            )
        )
        assertFalse(
            shortInfo.isValidShortNumberForRegion(parse("123456", RegionCode.FR), RegionCode.FR)
        )

        // Note that GB and GG share the country calling code 44.
        assertTrue(
            shortInfo.isValidShortNumber(
                PhoneNumber().setCountryCode(44).setNationalNumber(18001L)
            )
        )
    }

    @Test
    fun testIsCarrierSpecific() {
        val carrierSpecificNumber = PhoneNumber()
        carrierSpecificNumber.setCountryCode(1).setNationalNumber(33669L)
        assertTrue(shortInfo!!.isCarrierSpecific(carrierSpecificNumber))
        assertTrue(
            shortInfo.isCarrierSpecificForRegion(parse("33669", RegionCode.US), RegionCode.US)
        )
        val notCarrierSpecificNumber = PhoneNumber()
        notCarrierSpecificNumber.setCountryCode(1).setNationalNumber(911L)
        assertFalse(shortInfo.isCarrierSpecific(notCarrierSpecificNumber))
        assertFalse(
            shortInfo.isCarrierSpecificForRegion(parse("911", RegionCode.US), RegionCode.US)
        )
        val carrierSpecificNumberForSomeRegion = PhoneNumber()
        carrierSpecificNumberForSomeRegion.setCountryCode(1).setNationalNumber(211L)
        assertTrue(shortInfo.isCarrierSpecific(carrierSpecificNumberForSomeRegion))
        assertTrue(
            shortInfo.isCarrierSpecificForRegion(carrierSpecificNumberForSomeRegion, RegionCode.US)
        )
        assertFalse(
            shortInfo.isCarrierSpecificForRegion(carrierSpecificNumberForSomeRegion, RegionCode.BB)
        )
    }

    @Test
    fun testIsSmsService() {
        val smsServiceNumberForSomeRegion = PhoneNumber()
        smsServiceNumberForSomeRegion.setCountryCode(1).setNationalNumber(21234L)
        assertTrue(shortInfo!!.isSmsServiceForRegion(smsServiceNumberForSomeRegion, RegionCode.US))
        assertFalse(shortInfo.isSmsServiceForRegion(smsServiceNumberForSomeRegion, RegionCode.BB))
    }

    @Test
    fun testGetExpectedCost() {
        val premiumRateExample = shortInfo!!.getExampleShortNumberForCost(
            RegionCode.FR, ShortNumberInfo.ShortNumberCost.PREMIUM_RATE
        )
        assertEquals(
            ShortNumberInfo.ShortNumberCost.PREMIUM_RATE, shortInfo.getExpectedCostForRegion(
                parse(premiumRateExample, RegionCode.FR), RegionCode.FR
            )
        )
        val premiumRateNumber = PhoneNumber()
        premiumRateNumber.setCountryCode(33).setNationalNumber(premiumRateExample.toInt().toLong())
        assertEquals(
            ShortNumberInfo.ShortNumberCost.PREMIUM_RATE, shortInfo.getExpectedCost(premiumRateNumber)
        )
        val standardRateExample = shortInfo.getExampleShortNumberForCost(
            RegionCode.FR, ShortNumberInfo.ShortNumberCost.STANDARD_RATE
        )
        assertEquals(
            ShortNumberInfo.ShortNumberCost.STANDARD_RATE, shortInfo.getExpectedCostForRegion(
                parse(standardRateExample, RegionCode.FR), RegionCode.FR
            )
        )
        val standardRateNumber = PhoneNumber()
        standardRateNumber.setCountryCode(33).setNationalNumber(standardRateExample.toInt().toLong())
        assertEquals(
            ShortNumberInfo.ShortNumberCost.STANDARD_RATE, shortInfo.getExpectedCost(standardRateNumber)
        )
        val tollFreeExample = shortInfo.getExampleShortNumberForCost(
            RegionCode.FR, ShortNumberInfo.ShortNumberCost.TOLL_FREE
        )
        assertEquals(
            ShortNumberInfo.ShortNumberCost.TOLL_FREE,
            shortInfo.getExpectedCostForRegion(parse(tollFreeExample, RegionCode.FR), RegionCode.FR)
        )
        val tollFreeNumber = PhoneNumber()
        tollFreeNumber.setCountryCode(33).setNationalNumber(tollFreeExample.toInt().toLong())
        assertEquals(
            ShortNumberInfo.ShortNumberCost.TOLL_FREE, shortInfo.getExpectedCost(tollFreeNumber)
        )
        assertEquals(
            ShortNumberInfo.ShortNumberCost.UNKNOWN_COST,
            shortInfo.getExpectedCostForRegion(parse("12345", RegionCode.FR), RegionCode.FR)
        )
        val unknownCostNumber = PhoneNumber()
        unknownCostNumber.setCountryCode(33).setNationalNumber(12345L)
        assertEquals(
            ShortNumberInfo.ShortNumberCost.UNKNOWN_COST, shortInfo.getExpectedCost(unknownCostNumber)
        )

        // Test that an invalid number may nevertheless have a cost other than UNKNOWN_COST.
        assertFalse(
            shortInfo.isValidShortNumberForRegion(parse("116123", RegionCode.FR), RegionCode.FR)
        )
        assertEquals(
            ShortNumberInfo.ShortNumberCost.TOLL_FREE,
            shortInfo.getExpectedCostForRegion(parse("116123", RegionCode.FR), RegionCode.FR)
        )
        val invalidNumber = PhoneNumber()
        invalidNumber.setCountryCode(33).setNationalNumber(116123L)
        assertFalse(shortInfo.isValidShortNumber(invalidNumber))
        assertEquals(
            ShortNumberInfo.ShortNumberCost.TOLL_FREE, shortInfo.getExpectedCost(invalidNumber)
        )

        // Test a nonexistent country code.
        assertEquals(
            ShortNumberInfo.ShortNumberCost.UNKNOWN_COST,
            shortInfo.getExpectedCostForRegion(parse("911", RegionCode.US), RegionCode.ZZ)
        )
        unknownCostNumber.clear()
        unknownCostNumber.setCountryCode(123).setNationalNumber(911L)
        assertEquals(
            ShortNumberInfo.ShortNumberCost.UNKNOWN_COST, shortInfo.getExpectedCost(unknownCostNumber)
        )
    }

    @Test
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
        assertTrue(shortInfo!!.isValidShortNumber(ambiguousPremiumRateNumber))
        assertTrue(shortInfo.isValidShortNumber(ambiguousStandardRateNumber))
        assertTrue(shortInfo.isValidShortNumber(ambiguousTollFreeNumber))
        assertTrue(
            shortInfo.isValidShortNumberForRegion(
                parse(ambiguousPremiumRateString, RegionCode.AU), RegionCode.AU
            )
        )
        assertEquals(
            ShortNumberInfo.ShortNumberCost.PREMIUM_RATE, shortInfo.getExpectedCostForRegion(
                parse(ambiguousPremiumRateString, RegionCode.AU), RegionCode.AU
            )
        )
        assertFalse(
            shortInfo.isValidShortNumberForRegion(
                parse(ambiguousPremiumRateString, RegionCode.CX), RegionCode.CX
            )
        )
        assertEquals(
            ShortNumberInfo.ShortNumberCost.UNKNOWN_COST, shortInfo.getExpectedCostForRegion(
                parse(ambiguousPremiumRateString, RegionCode.CX), RegionCode.CX
            )
        )
        // PREMIUM_RATE takes precedence over UNKNOWN_COST.
        assertEquals(
            ShortNumberInfo.ShortNumberCost.PREMIUM_RATE, shortInfo.getExpectedCost(ambiguousPremiumRateNumber)
        )
        assertTrue(
            shortInfo.isValidShortNumberForRegion(
                parse(ambiguousStandardRateString, RegionCode.AU), RegionCode.AU
            )
        )
        assertEquals(
            ShortNumberInfo.ShortNumberCost.STANDARD_RATE, shortInfo.getExpectedCostForRegion(
                parse(ambiguousStandardRateString, RegionCode.AU), RegionCode.AU
            )
        )
        assertFalse(
            shortInfo.isValidShortNumberForRegion(
                parse(ambiguousStandardRateString, RegionCode.CX), RegionCode.CX
            )
        )
        assertEquals(
            ShortNumberInfo.ShortNumberCost.UNKNOWN_COST, shortInfo.getExpectedCostForRegion(
                parse(ambiguousStandardRateString, RegionCode.CX), RegionCode.CX
            )
        )
        assertEquals(
            ShortNumberInfo.ShortNumberCost.UNKNOWN_COST, shortInfo.getExpectedCost(ambiguousStandardRateNumber)
        )
        assertTrue(
            shortInfo.isValidShortNumberForRegion(
                parse(ambiguousTollFreeString, RegionCode.AU), RegionCode.AU
            )
        )
        assertEquals(
            ShortNumberInfo.ShortNumberCost.TOLL_FREE, shortInfo.getExpectedCostForRegion(
                parse(ambiguousTollFreeString, RegionCode.AU), RegionCode.AU
            )
        )
        assertFalse(
            shortInfo.isValidShortNumberForRegion(
                parse(ambiguousTollFreeString, RegionCode.CX), RegionCode.CX
            )
        )
        assertEquals(
            ShortNumberInfo.ShortNumberCost.UNKNOWN_COST, shortInfo.getExpectedCostForRegion(
                parse(ambiguousTollFreeString, RegionCode.CX), RegionCode.CX
            )
        )
        assertEquals(
            ShortNumberInfo.ShortNumberCost.UNKNOWN_COST, shortInfo.getExpectedCost(ambiguousTollFreeNumber)
        )
    }

    @Test
    fun testExampleShortNumberPresence() {
        assertFalse(shortInfo!!.getExampleShortNumber(RegionCode.AD).isEmpty())
        assertFalse(shortInfo.getExampleShortNumber(RegionCode.FR).isEmpty())
        assertTrue(shortInfo.getExampleShortNumber(RegionCode.UN001).isEmpty())
        assertTrue(shortInfo.getExampleShortNumber(null).isEmpty())
    }

    @Test
    fun testConnectsToEmergencyNumber_US() {
        assertTrue(shortInfo!!.connectsToEmergencyNumber("911", RegionCode.US))
        assertTrue(shortInfo.connectsToEmergencyNumber("112", RegionCode.US))
        assertFalse(shortInfo.connectsToEmergencyNumber("999", RegionCode.US))
    }

    @Test
    fun testConnectsToEmergencyNumberLongNumber_US() {
        assertTrue(shortInfo!!.connectsToEmergencyNumber("9116666666", RegionCode.US))
        assertTrue(shortInfo.connectsToEmergencyNumber("1126666666", RegionCode.US))
        assertFalse(shortInfo.connectsToEmergencyNumber("9996666666", RegionCode.US))
    }

    @Test
    fun testConnectsToEmergencyNumberWithFormatting_US() {
        assertTrue(shortInfo!!.connectsToEmergencyNumber("9-1-1", RegionCode.US))
        assertTrue(shortInfo.connectsToEmergencyNumber("1-1-2", RegionCode.US))
        assertFalse(shortInfo.connectsToEmergencyNumber("9-9-9", RegionCode.US))
    }

    @Test
    fun testConnectsToEmergencyNumberWithPlusSign_US() {
        assertFalse(shortInfo!!.connectsToEmergencyNumber("+911", RegionCode.US))
        assertFalse(shortInfo.connectsToEmergencyNumber("\uFF0B911", RegionCode.US))
        assertFalse(shortInfo.connectsToEmergencyNumber(" +911", RegionCode.US))
        assertFalse(shortInfo.connectsToEmergencyNumber("+112", RegionCode.US))
        assertFalse(shortInfo.connectsToEmergencyNumber("+999", RegionCode.US))
    }

    @Test
    fun testConnectsToEmergencyNumber_BR() {
        assertTrue(shortInfo!!.connectsToEmergencyNumber("911", RegionCode.BR))
        assertTrue(shortInfo.connectsToEmergencyNumber("190", RegionCode.BR))
        assertFalse(shortInfo.connectsToEmergencyNumber("999", RegionCode.BR))
    }

    @Test
    fun testConnectsToEmergencyNumberLongNumber_BR() {
        // Brazilian emergency numbers don't work when additional digits are appended.
        assertFalse(shortInfo!!.connectsToEmergencyNumber("9111", RegionCode.BR))
        assertFalse(shortInfo.connectsToEmergencyNumber("1900", RegionCode.BR))
        assertFalse(shortInfo.connectsToEmergencyNumber("9996", RegionCode.BR))
    }

    @Test
    fun testConnectsToEmergencyNumber_CL() {
        assertTrue(shortInfo!!.connectsToEmergencyNumber("131", RegionCode.CL))
        assertTrue(shortInfo.connectsToEmergencyNumber("133", RegionCode.CL))
    }

    @Test
    fun testConnectsToEmergencyNumberLongNumber_CL() {
        // Chilean emergency numbers don't work when additional digits are appended.
        assertFalse(shortInfo!!.connectsToEmergencyNumber("1313", RegionCode.CL))
        assertFalse(shortInfo.connectsToEmergencyNumber("1330", RegionCode.CL))
    }

    @Test
    fun testConnectsToEmergencyNumber_AO() {
        // Angola doesn't have any metadata for emergency numbers in the test metadata.
        assertFalse(shortInfo!!.connectsToEmergencyNumber("911", RegionCode.AO))
        assertFalse(shortInfo.connectsToEmergencyNumber("222123456", RegionCode.AO))
        assertFalse(shortInfo.connectsToEmergencyNumber("923123456", RegionCode.AO))
    }

    @Test
    fun testConnectsToEmergencyNumber_ZW() {
        // Zimbabwe doesn't have any metadata in the test metadata.
        assertFalse(shortInfo!!.connectsToEmergencyNumber("911", RegionCode.ZW))
        assertFalse(shortInfo.connectsToEmergencyNumber("01312345", RegionCode.ZW))
        assertFalse(shortInfo.connectsToEmergencyNumber("0711234567", RegionCode.ZW))
    }

    @Test
    fun testIsEmergencyNumber_US() {
        assertTrue(shortInfo!!.isEmergencyNumber("911", RegionCode.US))
        assertTrue(shortInfo.isEmergencyNumber("112", RegionCode.US))
        assertFalse(shortInfo.isEmergencyNumber("999", RegionCode.US))
    }

    @Test
    fun testIsEmergencyNumberLongNumber_US() {
        assertFalse(shortInfo!!.isEmergencyNumber("9116666666", RegionCode.US))
        assertFalse(shortInfo.isEmergencyNumber("1126666666", RegionCode.US))
        assertFalse(shortInfo.isEmergencyNumber("9996666666", RegionCode.US))
    }

    @Test
    fun testIsEmergencyNumberWithFormatting_US() {
        assertTrue(shortInfo!!.isEmergencyNumber("9-1-1", RegionCode.US))
        assertTrue(shortInfo.isEmergencyNumber("*911", RegionCode.US))
        assertTrue(shortInfo.isEmergencyNumber("1-1-2", RegionCode.US))
        assertTrue(shortInfo.isEmergencyNumber("*112", RegionCode.US))
        assertFalse(shortInfo.isEmergencyNumber("9-9-9", RegionCode.US))
        assertFalse(shortInfo.isEmergencyNumber("*999", RegionCode.US))
    }

    @Test
    fun testIsEmergencyNumberWithPlusSign_US() {
        assertFalse(shortInfo!!.isEmergencyNumber("+911", RegionCode.US))
        assertFalse(shortInfo.isEmergencyNumber("\uFF0B911", RegionCode.US))
        assertFalse(shortInfo.isEmergencyNumber(" +911", RegionCode.US))
        assertFalse(shortInfo.isEmergencyNumber("+112", RegionCode.US))
        assertFalse(shortInfo.isEmergencyNumber("+999", RegionCode.US))
    }

    @Test
    fun testIsEmergencyNumber_BR() {
        assertTrue(shortInfo!!.isEmergencyNumber("911", RegionCode.BR))
        assertTrue(shortInfo.isEmergencyNumber("190", RegionCode.BR))
        assertFalse(shortInfo.isEmergencyNumber("999", RegionCode.BR))
    }

    @Test
    fun testIsEmergencyNumberLongNumber_BR() {
        assertFalse(shortInfo!!.isEmergencyNumber("9111", RegionCode.BR))
        assertFalse(shortInfo.isEmergencyNumber("1900", RegionCode.BR))
        assertFalse(shortInfo.isEmergencyNumber("9996", RegionCode.BR))
    }

    @Test
    fun testIsEmergencyNumber_AO() {
        // Angola doesn't have any metadata for emergency numbers in the test metadata.
        assertFalse(shortInfo!!.isEmergencyNumber("911", RegionCode.AO))
        assertFalse(shortInfo.isEmergencyNumber("222123456", RegionCode.AO))
        assertFalse(shortInfo.isEmergencyNumber("923123456", RegionCode.AO))
    }

    @Test
    fun testIsEmergencyNumber_ZW() {
        // Zimbabwe doesn't have any metadata in the test metadata.
        assertFalse(shortInfo!!.isEmergencyNumber("911", RegionCode.ZW))
        assertFalse(shortInfo.isEmergencyNumber("01312345", RegionCode.ZW))
        assertFalse(shortInfo.isEmergencyNumber("0711234567", RegionCode.ZW))
    }

    @Test
    fun testEmergencyNumberForSharedCountryCallingCode() {
        // Test the emergency number 112, which is valid in both Australia and the Christmas Islands.
        assertTrue(shortInfo!!.isEmergencyNumber("112", RegionCode.AU))
        assertTrue(shortInfo.isValidShortNumberForRegion(parse("112", RegionCode.AU), RegionCode.AU))
        assertEquals(
            ShortNumberInfo.ShortNumberCost.TOLL_FREE,
            shortInfo.getExpectedCostForRegion(parse("112", RegionCode.AU), RegionCode.AU)
        )
        assertTrue(shortInfo.isEmergencyNumber("112", RegionCode.CX))
        assertTrue(shortInfo.isValidShortNumberForRegion(parse("112", RegionCode.CX), RegionCode.CX))
        assertEquals(
            ShortNumberInfo.ShortNumberCost.TOLL_FREE,
            shortInfo.getExpectedCostForRegion(parse("112", RegionCode.CX), RegionCode.CX)
        )
        val sharedEmergencyNumber = PhoneNumber().setCountryCode(61).setNationalNumber(112L)
        assertTrue(shortInfo.isValidShortNumber(sharedEmergencyNumber))
        assertEquals(
            ShortNumberInfo.ShortNumberCost.TOLL_FREE, shortInfo.getExpectedCost(sharedEmergencyNumber)
        )
    }

    @Test
    fun testOverlappingNANPANumber() {
        // 211 is an emergency number in Barbados, while it is a toll-free information line in Canada
        // and the USA.
        assertTrue(shortInfo!!.isEmergencyNumber("211", RegionCode.BB))
        assertEquals(
            ShortNumberInfo.ShortNumberCost.TOLL_FREE,
            shortInfo.getExpectedCostForRegion(parse("211", RegionCode.BB), RegionCode.BB)
        )
        assertFalse(shortInfo.isEmergencyNumber("211", RegionCode.US))
        assertEquals(
            ShortNumberInfo.ShortNumberCost.UNKNOWN_COST,
            shortInfo.getExpectedCostForRegion(parse("211", RegionCode.US), RegionCode.US)
        )
        assertFalse(shortInfo.isEmergencyNumber("211", RegionCode.CA))
        assertEquals(
            ShortNumberInfo.ShortNumberCost.TOLL_FREE,
            shortInfo.getExpectedCostForRegion(parse("211", RegionCode.CA), RegionCode.CA)
        )
    }

    @Test
    fun testCountryCallingCodeIsNotIgnored() {
        // +46 is the country calling code for Sweden (SE), and 40404 is a valid short number in the US.
        assertFalse(
            shortInfo!!.isPossibleShortNumberForRegion(
                parse("+4640404", RegionCode.SE), RegionCode.US
            )
        )
        assertFalse(
            shortInfo.isValidShortNumberForRegion(
                parse("+4640404", RegionCode.SE), RegionCode.US
            )
        )
        assertEquals(
            ShortNumberInfo.ShortNumberCost.UNKNOWN_COST, shortInfo.getExpectedCostForRegion(
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
