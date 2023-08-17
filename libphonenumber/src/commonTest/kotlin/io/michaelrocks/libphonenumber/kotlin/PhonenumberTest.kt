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

import io.michaelrocks.libphonenumber.kotlin.Phonenumber.PhoneNumber
import io.michaelrocks.libphonenumber.kotlin.Phonenumber.PhoneNumber.CountryCodeSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/**
 * Tests for the Phonenumber.PhoneNumber object itself.
 *
 * @author Lara Rennie
 */
class PhonenumberTest {
    @Test
    fun testEqualSimpleNumber() {
        val numberA = PhoneNumber()
        numberA.setCountryCode(1).setNationalNumber(6502530000L)
        val numberB = PhoneNumber()
        numberB.setCountryCode(1).setNationalNumber(6502530000L)
        assertEquals(numberA, numberB)
        assertEquals(numberA.hashCode(), numberB.hashCode())
    }

    @Test
    fun testEqualWithItalianLeadingZeroSetToDefault() {
        val numberA = PhoneNumber()
        numberA.setCountryCode(1).setNationalNumber(6502530000L).setItalianLeadingZero(false)
        val numberB = PhoneNumber()
        numberB.setCountryCode(1).setNationalNumber(6502530000L)

        // These should still be equal, since the default value for this field is false.
        assertEquals(numberA, numberB)
        assertEquals(numberA.hashCode(), numberB.hashCode())
    }

    @Test
    fun testEqualWithCountryCodeSourceSet() {
        val numberA = PhoneNumber()
        numberA.setRawInput("+1 650 253 00 00").setCountryCodeSource(CountryCodeSource.FROM_NUMBER_WITH_PLUS_SIGN)
        val numberB = PhoneNumber()
        numberB.setRawInput("+1 650 253 00 00").setCountryCodeSource(CountryCodeSource.FROM_NUMBER_WITH_PLUS_SIGN)
        assertEquals(numberA, numberB)
        assertEquals(numberA.hashCode(), numberB.hashCode())
    }

    @Test
    fun testNonEqualWithItalianLeadingZeroSetToTrue() {
        val numberA = PhoneNumber()
        numberA.setCountryCode(1).setNationalNumber(6502530000L).setItalianLeadingZero(true)
        val numberB = PhoneNumber()
        numberB.setCountryCode(1).setNationalNumber(6502530000L)
        assertFalse(numberA.equals(numberB))
        assertFalse(numberA.hashCode() == numberB.hashCode())
    }

    @Throws(Exception::class)
    fun testNonEqualWithDifferingRawInput() {
        val numberA = PhoneNumber()
        numberA.setCountryCode(1).setNationalNumber(6502530000L).setRawInput("+1 650 253 00 00")
            .setCountryCodeSource(CountryCodeSource.FROM_NUMBER_WITH_PLUS_SIGN)
        val numberB = PhoneNumber()
        // Although these numbers would pass an isNumberMatch test, they are not considered "equal" as
        // objects, since their raw input is different.
        numberB.setCountryCode(1).setNationalNumber(6502530000L).setRawInput("+1-650-253-00-00")
            .setCountryCodeSource(CountryCodeSource.FROM_NUMBER_WITH_PLUS_SIGN)
        assertFalse(numberA.equals(numberB))
        assertFalse(numberA.hashCode() == numberB.hashCode())
    }

    @Test
    fun testNonEqualWithPreferredDomesticCarrierCodeSetToDefault() {
        val numberA = PhoneNumber()
        numberA.setCountryCode(1).setNationalNumber(6502530000L).setPreferredDomesticCarrierCode("")
        val numberB = PhoneNumber()
        numberB.setCountryCode(1).setNationalNumber(6502530000L)
        assertFalse(numberA.equals(numberB))
        assertFalse(numberA.hashCode() == numberB.hashCode())
    }

    @Test
    fun testEqualWithPreferredDomesticCarrierCodeSetToDefault() {
        val numberA = PhoneNumber()
        numberA.setCountryCode(1).setNationalNumber(6502530000L).setPreferredDomesticCarrierCode("")
        val numberB = PhoneNumber()
        numberB.setCountryCode(1).setNationalNumber(6502530000L).setPreferredDomesticCarrierCode("")
        assertEquals(numberA, numberB)
        assertEquals(numberA.hashCode(), numberB.hashCode())
    }
}
