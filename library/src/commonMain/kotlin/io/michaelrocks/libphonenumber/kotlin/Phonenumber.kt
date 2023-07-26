/*
 * Copyright (C) 2010 The Libphonenumber Authors
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
/**
 * Definition of the class representing international telephone numbers. This class is hand-created
 * based on the class file compiled from phonenumber.proto. Please refer to that file for detailed
 * descriptions of the meaning of each field.
 */
package io.michaelrocks.libphonenumber.kotlin

import io.michaelrocks.libphonenumber.kotlin.io.Serializable
import io.michaelrocks.libphonenumber.kotlin.util.InplaceStringBuilder

class Phonenumber private constructor() {
    class PhoneNumber : Serializable {
        enum class CountryCodeSource {
            FROM_NUMBER_WITH_PLUS_SIGN,
            FROM_NUMBER_WITH_IDD,
            FROM_NUMBER_WITHOUT_PLUS_SIGN,
            FROM_DEFAULT_COUNTRY,
            UNSPECIFIED
        }

        // required int32 country_code = 1;
        private var hasCountryCode = false
        var countryCode = 0
            private set

        fun hasCountryCode(): Boolean {
            return hasCountryCode
        }

        fun setCountryCode(value: Int): PhoneNumber {
            hasCountryCode = true
            countryCode = value
            return this
        }

        fun clearCountryCode(): PhoneNumber {
            hasCountryCode = false
            countryCode = 0
            return this
        }

        // required uint64 national_number = 2;
        private var hasNationalNumber = false
        var nationalNumber = 0L
            private set

        fun hasNationalNumber(): Boolean {
            return hasNationalNumber
        }

        fun setNationalNumber(value: Long): PhoneNumber {
            hasNationalNumber = true
            nationalNumber = value
            return this
        }

        fun clearNationalNumber(): PhoneNumber {
            hasNationalNumber = false
            nationalNumber = 0L
            return this
        }

        // optional string extension = 3;
        private var hasExtension = false
        var extension = ""
            private set

        fun hasExtension(): Boolean {
            return hasExtension
        }

        fun setExtension(value: String?): PhoneNumber {
            if (value == null) {
                throw NullPointerException()
            }
            hasExtension = true
            this.extension = value
            return this
        }

        fun clearExtension(): PhoneNumber {
            hasExtension = false
            this.extension = ""
            return this
        }

        // optional bool italian_leading_zero = 4;
        private var hasItalianLeadingZero = false
        var isItalianLeadingZero = false
            private set

        fun hasItalianLeadingZero(): Boolean {
            return hasItalianLeadingZero
        }

        fun setItalianLeadingZero(value: Boolean): PhoneNumber {
            hasItalianLeadingZero = true
            isItalianLeadingZero = value
            return this
        }

        fun clearItalianLeadingZero(): PhoneNumber {
            hasItalianLeadingZero = false
            isItalianLeadingZero = false
            return this
        }

        // optional int32 number_of_leading_zeros = 8 [default = 1];
        private var hasNumberOfLeadingZeros = false
        var numberOfLeadingZeros = 1
            private set

        fun hasNumberOfLeadingZeros(): Boolean {
            return hasNumberOfLeadingZeros
        }

        fun setNumberOfLeadingZeros(value: Int): PhoneNumber {
            hasNumberOfLeadingZeros = true
            numberOfLeadingZeros = value
            return this
        }

        fun clearNumberOfLeadingZeros(): PhoneNumber {
            hasNumberOfLeadingZeros = false
            numberOfLeadingZeros = 1
            return this
        }

        // optional string raw_input = 5;
        private var hasRawInput = false
        var rawInput = ""
            private set

        fun hasRawInput(): Boolean {
            return hasRawInput
        }

        fun setRawInput(value: String?): PhoneNumber {
            if (value == null) {
                throw NullPointerException()
            }
            hasRawInput = true
            rawInput = value
            return this
        }

        fun clearRawInput(): PhoneNumber {
            hasRawInput = false
            rawInput = ""
            return this
        }

        // optional CountryCodeSource country_code_source = 6;
        private var hasCountryCodeSource = false
        var countryCodeSource: CountryCodeSource
            private set

        fun hasCountryCodeSource(): Boolean {
            return hasCountryCodeSource
        }

        fun setCountryCodeSource(value: CountryCodeSource?): PhoneNumber {
            if (value == null) {
                throw NullPointerException()
            }
            hasCountryCodeSource = true
            countryCodeSource = value
            return this
        }

        fun clearCountryCodeSource(): PhoneNumber {
            hasCountryCodeSource = false
            countryCodeSource = CountryCodeSource.UNSPECIFIED
            return this
        }

        // optional string preferred_domestic_carrier_code = 7;
        private var hasPreferredDomesticCarrierCode = false
        var preferredDomesticCarrierCode = ""
            private set

        init {
            countryCodeSource = CountryCodeSource.UNSPECIFIED
        }

        fun hasPreferredDomesticCarrierCode(): Boolean {
            return hasPreferredDomesticCarrierCode
        }

        fun setPreferredDomesticCarrierCode(value: String?): PhoneNumber {
            if (value == null) {
                throw NullPointerException()
            }
            hasPreferredDomesticCarrierCode = true
            preferredDomesticCarrierCode = value
            return this
        }

        fun clearPreferredDomesticCarrierCode(): PhoneNumber {
            hasPreferredDomesticCarrierCode = false
            preferredDomesticCarrierCode = ""
            return this
        }

        fun clear(): PhoneNumber {
            clearCountryCode()
            clearNationalNumber()
            clearExtension()
            clearItalianLeadingZero()
            clearNumberOfLeadingZeros()
            clearRawInput()
            clearCountryCodeSource()
            clearPreferredDomesticCarrierCode()
            return this
        }

        fun mergeFrom(other: PhoneNumber): PhoneNumber {
            if (other.hasCountryCode()) {
                setCountryCode(other.countryCode)
            }
            if (other.hasNationalNumber()) {
                setNationalNumber(other.nationalNumber)
            }
            if (other.hasExtension()) {
                setExtension(other.extension)
            }
            if (other.hasItalianLeadingZero()) {
                setItalianLeadingZero(other.isItalianLeadingZero)
            }
            if (other.hasNumberOfLeadingZeros()) {
                setNumberOfLeadingZeros(other.numberOfLeadingZeros)
            }
            if (other.hasRawInput()) {
                setRawInput(other.rawInput)
            }
            if (other.hasCountryCodeSource()) {
                setCountryCodeSource(other.countryCodeSource)
            }
            if (other.hasPreferredDomesticCarrierCode()) {
                setPreferredDomesticCarrierCode(other.preferredDomesticCarrierCode)
            }
            return this
        }

        fun exactlySameAs(other: PhoneNumber?): Boolean {
            if (other == null) {
                return false
            }
            return if (this === other) {
                true
            } else countryCode == other.countryCode && nationalNumber == other.nationalNumber && this.extension == other.extension && isItalianLeadingZero == other.isItalianLeadingZero && numberOfLeadingZeros == other.numberOfLeadingZeros && rawInput == other.rawInput && countryCodeSource == other.countryCodeSource && preferredDomesticCarrierCode == other.preferredDomesticCarrierCode && hasPreferredDomesticCarrierCode() == other.hasPreferredDomesticCarrierCode()
        }

        override fun equals(that: Any?): Boolean {
            return that is PhoneNumber && exactlySameAs(that as PhoneNumber?)
        }

        override fun hashCode(): Int {
            // Simplified rendition of the hashCode function automatically generated from the proto
            // compiler with java_generate_equals_and_hash set to true. We are happy with unset values to
            // be considered equal to their explicitly-set equivalents, so don't check if any value is
            // unknown. The only exception to this is the preferred domestic carrier code.
            var hash = 41
            hash = 53 * hash + countryCode
            hash = 53 * hash + nationalNumber.hashCode()
            hash = 53 * hash + this.extension.hashCode()
            hash = 53 * hash + if (isItalianLeadingZero) 1231 else 1237
            hash = 53 * hash + numberOfLeadingZeros
            hash = 53 * hash + rawInput.hashCode()
            hash = 53 * hash + countryCodeSource.hashCode()
            hash = 53 * hash + preferredDomesticCarrierCode.hashCode()
            hash = 53 * hash + if (hasPreferredDomesticCarrierCode()) 1231 else 1237
            return hash
        }

        override fun toString(): String {
            val outputString = InplaceStringBuilder()
            outputString.append("Country Code: ").append(countryCode)
            outputString.append(" National Number: ").append(nationalNumber)
            if (hasItalianLeadingZero() && isItalianLeadingZero) {
                outputString.append(" Leading Zero(s): true")
            }
            if (hasNumberOfLeadingZeros()) {
                outputString.append(" Number of leading zeros: ").append(numberOfLeadingZeros)
            }
            if (hasExtension()) {
                outputString.append(" Extension: ").append(this.extension)
            }
            if (hasCountryCodeSource()) {
                outputString.append(" Country Code Source: ").append(countryCodeSource)
            }
            if (hasPreferredDomesticCarrierCode()) {
                outputString.append(" Preferred Domestic Carrier Code: ").append(preferredDomesticCarrierCode)
            }
            return outputString.toString()
        }

        companion object {
            private const val serialVersionUID = 1L
        }
    }
}
