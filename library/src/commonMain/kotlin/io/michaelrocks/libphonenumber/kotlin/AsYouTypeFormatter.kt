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
package io.michaelrocks.libphonenumber.android

import io.michaelrocks.libphonenumber.kotlin.PhoneNumberUtil
import io.michaelrocks.libphonenumber.kotlin.PhoneNumberUtil.Companion.formattingRuleHasFirstGroupOnly
import io.michaelrocks.libphonenumber.kotlin.PhoneNumberUtil.Companion.normalizeDiallableCharsOnly
import io.michaelrocks.libphonenumber.kotlin.Phonemetadata
import io.michaelrocks.libphonenumber.kotlin.Phonemetadata.PhoneMetadata
import io.michaelrocks.libphonenumber.kotlin.Phonemetadata.PhoneMetadata.Companion.newBuilder
import io.michaelrocks.libphonenumber.kotlin.internal.RegexCache
import io.michaelrocks.libphonenumber.kotlin.util.InplaceStringBuilder
import kotlin.math.min

/**
 * A formatter which formats phone numbers as they are entered.
 *
 *
 * An AsYouTypeFormatter can be created by invoking
 * [PhoneNumberUtil.getAsYouTypeFormatter]. After that, digits can be added by invoking
 * [.inputDigit] on the formatter instance, and the partially formatted phone number will be
 * returned each time a digit is added. [.clear] can be invoked before formatting a new
 * number.
 *
 *
 * See the unittests for more details on how the formatter is to be used.
 *
 * @author Shaopeng Jia
 */
class AsYouTypeFormatter internal constructor(
    private val phoneUtil: PhoneNumberUtil, private val defaultCountry: String
) {
    private var currentOutput = ""
    private val formattingTemplate = InplaceStringBuilder()

    // The pattern from numberFormat that is currently used to create formattingTemplate.
    private var currentFormattingPattern = ""
    private val accruedInput = InplaceStringBuilder()
    private val accruedInputWithoutFormatting = InplaceStringBuilder()

    // This indicates whether AsYouTypeFormatter is currently doing the formatting.
    private var ableToFormat = true

    // Set to true when users enter their own formatting. AsYouTypeFormatter will do no formatting at
    // all when this is set to true.
    private var inputHasFormatting = false

    // This is set to true when we know the user is entering a full national significant number, since
    // we have either detected a national prefix or an international dialing prefix. When this is
    // true, we will no longer use local number formatting patterns.
    private var isCompleteNumber = false
    private var isExpectingCountryCallingCode = false
    private val defaultMetadata: PhoneMetadata?
    private var currentMetadata: PhoneMetadata?
    private var lastMatchPosition = 0

    // The position of a digit upon which inputDigitAndRememberPosition is most recently invoked, as
    // found in the original sequence of characters the user entered.
    private var originalPosition = 0

    // The position of a digit upon which inputDigitAndRememberPosition is most recently invoked, as
    // found in accruedInputWithoutFormatting.
    private var positionToRemember = 0

    // This contains anything that has been entered so far preceding the national significant number,
    // and it is formatted (e.g. with space inserted). For example, this can contain IDD, country
    // code, and/or NDD, etc.
    private val prefixBeforeNationalNumber = InplaceStringBuilder()
    private var shouldAddSpaceAfterNationalPrefix = false

    // @VisibleForTesting
    // This contains the national prefix that has been extracted. It contains only digits without
    // formatting.
    var extractedNationalPrefix = ""
        private set
    private val nationalNumber = InplaceStringBuilder()
    private val possibleFormats: MutableList<Phonemetadata.NumberFormat> = ArrayList()

    // A cache for frequently used country-specific regular expressions.
    private val regexCache = RegexCache(64)

    /**
     * Constructs an as-you-type formatter. Should be obtained from [ ][PhoneNumberUtil.getAsYouTypeFormatter].
     *
     * @param phoneUtil   an instance of [PhoneNumberUtil]
     * @param regionCode  the country/region where the phone number is being entered
     */
    init {
        currentMetadata = getMetadataForRegion(defaultCountry)
        defaultMetadata = currentMetadata
    }

    // The metadata needed by this class is the same for all regions sharing the same country calling
    // code. Therefore, we return the metadata for "main" region for this country calling code.
    private fun getMetadataForRegion(regionCode: String): PhoneMetadata {
        val countryCallingCode = phoneUtil.getCountryCodeForRegion(regionCode)
        val mainCountry = phoneUtil.getRegionCodeForCountryCode(countryCallingCode)
        val metadata = phoneUtil.getMetadataForRegion(mainCountry)
        return metadata ?: EMPTY_METADATA
        // Set to a default instance of the metadata. This allows us to function with an incorrect
        // region code, even if formatting only works for numbers specified with "+".
    }

    // Returns true if a new template is created as opposed to reusing the existing template.
    private fun maybeCreateNewTemplate(): Boolean {
        // When there are multiple available formats, the formatter uses the first format where a
        // formatting template could be created.
        val it = possibleFormats.iterator()
        while (it.hasNext()) {
            val numberFormat = it.next()
            val pattern = numberFormat.pattern
            if (currentFormattingPattern == pattern) {
                return false
            }
            if (createFormattingTemplate(numberFormat)) {
                currentFormattingPattern = pattern
                shouldAddSpaceAfterNationalPrefix = NATIONAL_PREFIX_SEPARATORS_PATTERN.containsMatchIn(
                    numberFormat.nationalPrefixFormattingRule
                )
                // With a new formatting template, the matched position using the old template needs to be
                // reset.
                lastMatchPosition = 0
                return true
            } else {  // Remove the current number format from possibleFormats.
                it.remove()
            }
        }
        ableToFormat = false
        return false
    }

    private fun getAvailableFormats(leadingDigits: String) {
        // First decide whether we should use international or national number rules.
        val isInternationalNumber = isCompleteNumber && extractedNationalPrefix.length == 0
        val formatList =
            if (isInternationalNumber && currentMetadata!!.intlNumberFormatCount > 0) currentMetadata!!.intlNumberFormatList else currentMetadata!!.numberFormatList
        for (format in formatList) {
            // Discard a few formats that we know are not relevant based on the presence of the national
            // prefix.
            if (extractedNationalPrefix.length > 0 && formattingRuleHasFirstGroupOnly(
                    format.nationalPrefixFormattingRule
                ) && !format.nationalPrefixOptionalWhenFormatting && !format.hasDomesticCarrierCodeFormattingRule()
            ) {
                // If it is a national number that had a national prefix, any rules that aren't valid with a
                // national prefix should be excluded. A rule that has a carrier-code formatting rule is
                // kept since the national prefix might actually be an extracted carrier code - we don't
                // distinguish between these when extracting it in the AYTF.
                continue
            } else if (extractedNationalPrefix.length == 0 && !isCompleteNumber && !formattingRuleHasFirstGroupOnly(
                    format.nationalPrefixFormattingRule
                ) && !format.nationalPrefixOptionalWhenFormatting
            ) {
                // This number was entered without a national prefix, and this formatting rule requires one,
                // so we discard it.
                continue
            }
            if (ELIGIBLE_FORMAT_PATTERN.matches(format.format)) {
                possibleFormats.add(format)
            }
        }
        narrowDownPossibleFormats(leadingDigits)
    }

    private fun narrowDownPossibleFormats(leadingDigits: String) {
        val indexOfLeadingDigitsPattern = leadingDigits.length - MIN_LEADING_DIGITS_LENGTH
        val it = possibleFormats.iterator()
        while (it.hasNext()) {
            val format = it.next()
            if (format.leadingDigitsPatternCount == 0) {
                // Keep everything that isn't restricted by leading digits.
                continue
            }
            val lastLeadingDigitsPattern =
                min(indexOfLeadingDigitsPattern.toDouble(), (format.leadingDigitsPatternCount - 1).toDouble()).toInt()
            val leadingDigitsPattern = regexCache.getRegexForPattern(
                format.getLeadingDigitsPattern(lastLeadingDigitsPattern)
            )
            if (!leadingDigitsPattern.matchesAt(leadingDigits, 0)) {
                it.remove()
            }
        }
    }

    private fun createFormattingTemplate(format: Phonemetadata.NumberFormat): Boolean {
        val numberPattern = format.pattern
        formattingTemplate.setLength(0)
        val tempTemplate = getFormattingTemplate(numberPattern, format.format)
        if (tempTemplate.length > 0) {
            formattingTemplate.append(tempTemplate)
            return true
        }
        return false
    }

    // Gets a formatting template which can be used to efficiently format a partial number where
    // digits are added one by one.
    private fun getFormattingTemplate(numberPattern: String, numberFormat: String): String {
        // Creates a phone number consisting only of the digit 9 that matches the
        // numberPattern by applying the pattern to the longestPhoneNumber string.
        val longestPhoneNumber = "999999999999999"
        // this will always succeed
        val m = regexCache.getRegexForPattern(numberPattern).find(longestPhoneNumber)!!
        val aPhoneNumber = m.groups[0]!!.value
        // No formatting template can be created if the number of digits entered so far is longer than
        // the maximum the current formatting rule can accommodate.
        if (aPhoneNumber.length < nationalNumber.length) {
            return ""
        }
        // Formats the number according to numberFormat
        var template = aPhoneNumber.replace(numberPattern.toRegex(), numberFormat)
        // Replaces each digit with character DIGIT_PLACEHOLDER
        template = template.replace("9".toRegex(), DIGIT_PLACEHOLDER)
        return template
    }

    /**
     * Clears the internal state of the formatter, so it can be reused.
     */
    fun clear() {
        currentOutput = ""
        accruedInput.setLength(0)
        accruedInputWithoutFormatting.setLength(0)
        formattingTemplate.setLength(0)
        lastMatchPosition = 0
        currentFormattingPattern = ""
        prefixBeforeNationalNumber.setLength(0)
        extractedNationalPrefix = ""
        nationalNumber.setLength(0)
        ableToFormat = true
        inputHasFormatting = false
        positionToRemember = 0
        originalPosition = 0
        isCompleteNumber = false
        isExpectingCountryCallingCode = false
        possibleFormats.clear()
        shouldAddSpaceAfterNationalPrefix = false
        if (currentMetadata != defaultMetadata) {
            currentMetadata = getMetadataForRegion(defaultCountry)
        }
    }

    /**
     * Formats a phone number on-the-fly as each digit is entered.
     *
     * @param nextChar  the most recently entered digit of a phone number. Formatting characters are
     * allowed, but as soon as they are encountered this method formats the number as entered and
     * not "as you type" anymore. Full width digits and Arabic-indic digits are allowed, and will
     * be shown as they are.
     * @return  the partially formatted phone number.
     */
    fun inputDigit(nextChar: Char): String {
        currentOutput = inputDigitWithOptionToRememberPosition(nextChar, false)
        return currentOutput
    }

    /**
     * Same as [.inputDigit], but remembers the position where `nextChar` is inserted, so
     * that it can be retrieved later by using [.getRememberedPosition]. The remembered
     * position will be automatically adjusted if additional formatting characters are later
     * inserted/removed in front of `nextChar`.
     */
    fun inputDigitAndRememberPosition(nextChar: Char): String {
        currentOutput = inputDigitWithOptionToRememberPosition(nextChar, true)
        return currentOutput
    }

    private fun inputDigitWithOptionToRememberPosition(nextChar: Char, rememberPosition: Boolean): String {
        var nextChar = nextChar
        accruedInput.append(nextChar)
        if (rememberPosition) {
            originalPosition = accruedInput.length
        }
        // We do formatting on-the-fly only when each character entered is either a digit, or a plus
        // sign (accepted at the start of the number only).
        if (!isDigitOrLeadingPlusSign(nextChar)) {
            ableToFormat = false
            inputHasFormatting = true
        } else {
            nextChar = normalizeAndAccrueDigitsAndPlusSign(nextChar, rememberPosition)
        }
        if (!ableToFormat) {
            // When we are unable to format because of reasons other than that formatting chars have been
            // entered, it can be due to really long IDDs or NDDs. If that is the case, we might be able
            // to do formatting again after extracting them.
            if (inputHasFormatting) {
                return accruedInput.toString()
            } else if (attemptToExtractIdd()) {
                if (attemptToExtractCountryCallingCode()) {
                    return attemptToChoosePatternWithPrefixExtracted()
                }
            } else if (ableToExtractLongerNdd()) {
                // Add an additional space to separate long NDD and national significant number for
                // readability. We don't set shouldAddSpaceAfterNationalPrefix to true, since we don't want
                // this to change later when we choose formatting templates.
                prefixBeforeNationalNumber.append(SEPARATOR_BEFORE_NATIONAL_NUMBER)
                return attemptToChoosePatternWithPrefixExtracted()
            }
            return accruedInput.toString()
        }
        return when (accruedInputWithoutFormatting.length) {
            0, 1, 2 -> accruedInput.toString()
            3 -> {
                if (attemptToExtractIdd()) {
                    isExpectingCountryCallingCode = true
                } else {  // No IDD or plus sign is found, might be entering in national format.
                    extractedNationalPrefix = removeNationalPrefixFromNationalNumber()
                    return attemptToChooseFormattingPattern()
                }
                if (isExpectingCountryCallingCode) {
                    if (attemptToExtractCountryCallingCode()) {
                        isExpectingCountryCallingCode = false
                    }
                    return prefixBeforeNationalNumber.toString() + nationalNumber.toString()
                }
                if (possibleFormats.size > 0) {  // The formatting patterns are already chosen.
                    val tempNationalNumber = inputDigitHelper(nextChar)
                    // See if the accrued digits can be formatted properly already. If not, use the results
                    // from inputDigitHelper, which does formatting based on the formatting pattern chosen.
                    val formattedNumber = attemptToFormatAccruedDigits()
                    if (formattedNumber.length > 0) {
                        return formattedNumber
                    }
                    narrowDownPossibleFormats(nationalNumber.toString())
                    if (maybeCreateNewTemplate()) {
                        return inputAccruedNationalNumber()
                    }
                    if (ableToFormat) appendNationalNumber(tempNationalNumber) else accruedInput.toString()
                } else {
                    attemptToChooseFormattingPattern()
                }
            }

            else -> {
                if (isExpectingCountryCallingCode) {
                    if (attemptToExtractCountryCallingCode()) {
                        isExpectingCountryCallingCode = false
                    }
                    return prefixBeforeNationalNumber.toString() + nationalNumber.toString()
                }
                if (possibleFormats.size > 0) {
                    val tempNationalNumber = inputDigitHelper(nextChar)
                    val formattedNumber = attemptToFormatAccruedDigits()
                    if (formattedNumber.length > 0) {
                        return formattedNumber
                    }
                    narrowDownPossibleFormats(nationalNumber.toString())
                    if (maybeCreateNewTemplate()) {
                        return inputAccruedNationalNumber()
                    }
                    if (ableToFormat) appendNationalNumber(tempNationalNumber) else accruedInput.toString()
                } else {
                    attemptToChooseFormattingPattern()
                }
            }
        }
    }

    private fun attemptToChoosePatternWithPrefixExtracted(): String {
        ableToFormat = true
        isExpectingCountryCallingCode = false
        possibleFormats.clear()
        lastMatchPosition = 0
        formattingTemplate.setLength(0)
        currentFormattingPattern = ""
        return attemptToChooseFormattingPattern()
    }

    // Some national prefixes are a substring of others. If extracting the shorter NDD doesn't result
    // in a number we can format, we try to see if we can extract a longer version here.
    private fun ableToExtractLongerNdd(): Boolean {
        if (extractedNationalPrefix.length > 0) {
            // Put the extracted NDD back to the national number before attempting to extract a new NDD.
            nationalNumber.insert(0, extractedNationalPrefix)
            // Remove the previously extracted NDD from prefixBeforeNationalNumber. We cannot simply set
            // it to empty string because people sometimes incorrectly enter national prefix after the
            // country code, e.g. +44 (0)20-1234-5678.
            val indexOfPreviousNdd = prefixBeforeNationalNumber.lastIndexOf(extractedNationalPrefix)
            prefixBeforeNationalNumber.setLength(indexOfPreviousNdd)
        }
        return extractedNationalPrefix != removeNationalPrefixFromNationalNumber()
    }

    private fun isDigitOrLeadingPlusSign(nextChar: Char): Boolean {
        return (nextChar.isDigit() || (accruedInput.length == 1 && PhoneNumberUtil.PLUS_CHARS_PATTERN.matches(nextChar.toString())))
    }

    /**
     * Checks to see if there is an exact pattern match for these digits. If so, we should use this
     * instead of any other formatting template whose leadingDigitsPattern also matches the input.
     */
    fun attemptToFormatAccruedDigits(): String {
        for (numberFormat in possibleFormats) {
            val r: Regex = regexCache.getRegexForPattern(numberFormat.pattern)
            if (r.matches(nationalNumber)) {
                shouldAddSpaceAfterNationalPrefix = NATIONAL_PREFIX_SEPARATORS_PATTERN.containsMatchIn(
                    numberFormat.nationalPrefixFormattingRule
                )
                val formattedNumber: String = numberFormat.pattern.replace(r, numberFormat.format)
                // Check that we did not remove nor add any extra digits when we matched
                // this formatting pattern. This usually happens after we entered the last
                // digit during AYTF. Eg: In case of MX, we swallow mobile token (1) when
                // formatted but AYTF should retain all the number entered and not change
                // in order to match a format (of same leading digits and length) display
                // in that way.
                val fullOutput = appendNationalNumber(formattedNumber)
                val formattedNumberDigitsOnly = normalizeDiallableCharsOnly(fullOutput)
                if (formattedNumberDigitsOnly.contentEquals(accruedInputWithoutFormatting)) {
                    // If it's the same (i.e entered number and format is same), then it's
                    // safe to return this in formatted number as nothing is lost / added.
                    return fullOutput
                }
            }
        }
        return ""
    }

    val rememberedPosition: Int
        /**
         * Returns the current position in the partially formatted phone number of the character which was
         * previously passed in as the parameter of [.inputDigitAndRememberPosition].
         */
        get() {
            if (!ableToFormat) {
                return originalPosition
            }
            var accruedInputIndex = 0
            var currentOutputIndex = 0
            while (accruedInputIndex < positionToRemember && currentOutputIndex < currentOutput.length) {
                if (accruedInputWithoutFormatting[accruedInputIndex] == currentOutput[currentOutputIndex]) {
                    accruedInputIndex++
                }
                currentOutputIndex++
            }
            return currentOutputIndex
        }

    /**
     * Combines the national number with any prefix (IDD/+ and country code or national prefix) that
     * was collected. A space will be inserted between them if the current formatting template
     * indicates this to be suitable.
     */
    private fun appendNationalNumber(nationalNumber: String): String {
        val prefixBeforeNationalNumberLength = prefixBeforeNationalNumber.length
        return if (shouldAddSpaceAfterNationalPrefix && prefixBeforeNationalNumberLength > 0 && (prefixBeforeNationalNumber[prefixBeforeNationalNumberLength - 1] != SEPARATOR_BEFORE_NATIONAL_NUMBER)) {
            // We want to add a space after the national prefix if the national prefix formatting rule
            // indicates that this would normally be done, with the exception of the case where we already
            // appended a space because the NDD was surprisingly long.
            (prefixBeforeNationalNumber.toString() + SEPARATOR_BEFORE_NATIONAL_NUMBER + nationalNumber)
        } else {
            prefixBeforeNationalNumber.toString() + nationalNumber
        }
    }

    /**
     * Attempts to set the formatting template and returns a string which contains the formatted
     * version of the digits entered so far.
     */
    private fun attemptToChooseFormattingPattern(): String {
        // We start to attempt to format only when at least MIN_LEADING_DIGITS_LENGTH digits of national
        // number (excluding national prefix) have been entered.
        return if (nationalNumber.length >= MIN_LEADING_DIGITS_LENGTH) {
            getAvailableFormats(nationalNumber.toString())
            // See if the accrued digits can be formatted properly already.
            val formattedNumber = attemptToFormatAccruedDigits()
            if (formattedNumber.length > 0) {
                return formattedNumber
            }
            if (maybeCreateNewTemplate()) inputAccruedNationalNumber() else accruedInput.toString()
        } else {
            appendNationalNumber(nationalNumber.toString())
        }
    }

    /**
     * Invokes inputDigitHelper on each digit of the national number accrued, and returns a formatted
     * string in the end.
     */
    private fun inputAccruedNationalNumber(): String {
        val lengthOfNationalNumber = nationalNumber.length
        return if (lengthOfNationalNumber > 0) {
            var tempNationalNumber = ""
            for (i in 0..<lengthOfNationalNumber) {
                tempNationalNumber = inputDigitHelper(nationalNumber[i])
            }
            if (ableToFormat) appendNationalNumber(tempNationalNumber) else accruedInput.toString()
        } else {
            prefixBeforeNationalNumber.toString()
        }
    }

    private val isNanpaNumberWithNationalPrefix: Boolean
        /**
         * Returns true if the current country is a NANPA country and the national number begins with
         * the national prefix.
         */
        private get() =// For NANPA numbers beginning with 1[2-9], treat the 1 as the national prefix. The reason is
// that national significant numbers in NANPA always start with [2-9] after the national prefix.
// Numbers beginning with 1[01] can only be short/emergency numbers, which don't need the
            // national prefix.
            currentMetadata!!.countryCode == 1 && nationalNumber[0] == '1' && nationalNumber[1] != '0' && nationalNumber[1] != '1'

    // Returns the national prefix extracted, or an empty string if it is not present.
    private fun removeNationalPrefixFromNationalNumber(): String {
        var startOfNationalNumber = 0
        if (isNanpaNumberWithNationalPrefix) {
            startOfNationalNumber = 1
            prefixBeforeNationalNumber.append('1').append(SEPARATOR_BEFORE_NATIONAL_NUMBER)
            isCompleteNumber = true
        } else if (currentMetadata!!.hasNationalPrefixForParsing()) {
            val nationalPrefixForParsing = regexCache.getRegexForPattern(currentMetadata!!.nationalPrefixForParsing)
            val m = nationalPrefixForParsing.matchAt(nationalNumber, 0)
            // Since some national prefix patterns are entirely optional, check that a national prefix
            // could actually be extracted.
            if (m != null && m.range.last > 0) {
                // When the national prefix is detected, we use international formatting rules instead of
                // national ones, because national formatting rules could contain local formatting rules
                // for numbers entered without area code.
                isCompleteNumber = true
                startOfNationalNumber = m.range.last
                prefixBeforeNationalNumber.append(nationalNumber.substring(0, startOfNationalNumber))
            }
        }
        val nationalPrefix = nationalNumber.substring(0, startOfNationalNumber)
        nationalNumber.removeRange(0, startOfNationalNumber)
        return nationalPrefix
    }

    /**
     * Extracts IDD and plus sign to prefixBeforeNationalNumber when they are available, and places
     * the remaining input into nationalNumber.
     *
     * @return  true when accruedInputWithoutFormatting begins with the plus sign or valid IDD for
     * defaultCountry.
     */
    private fun attemptToExtractIdd(): Boolean {
        val internationalPrefix = regexCache.getRegexForPattern(
            "\\" + PhoneNumberUtil.PLUS_SIGN + "|" + currentMetadata!!.internationalPrefix
        )
        val iddMatchResult = internationalPrefix.matchAt(accruedInputWithoutFormatting, 0)
        if (iddMatchResult != null) {
            isCompleteNumber = true
            val startOfCountryCallingCode = iddMatchResult.range.last
            nationalNumber.setLength(0)
            nationalNumber.append(accruedInputWithoutFormatting.substring(startOfCountryCallingCode))
            prefixBeforeNationalNumber.setLength(0)
            prefixBeforeNationalNumber.append(
                accruedInputWithoutFormatting.substring(0, startOfCountryCallingCode)
            )
            if (accruedInputWithoutFormatting[0] != PhoneNumberUtil.PLUS_SIGN) {
                prefixBeforeNationalNumber.append(SEPARATOR_BEFORE_NATIONAL_NUMBER)
            }
            return true
        }
        return false
    }

    /**
     * Extracts the country calling code from the beginning of nationalNumber to
     * prefixBeforeNationalNumber when they are available, and places the remaining input into
     * nationalNumber.
     *
     * @return  true when a valid country calling code can be found.
     */
    private fun attemptToExtractCountryCallingCode(): Boolean {
        if (nationalNumber.length == 0) {
            return false
        }
        val numberWithoutCountryCallingCode = InplaceStringBuilder()
        val countryCode = phoneUtil.extractCountryCode(nationalNumber, numberWithoutCountryCallingCode)
        if (countryCode == 0) {
            return false
        }
        nationalNumber.setLength(0)
        nationalNumber.append(numberWithoutCountryCallingCode)
        val newRegionCode = phoneUtil.getRegionCodeForCountryCode(countryCode)
        if (PhoneNumberUtil.REGION_CODE_FOR_NON_GEO_ENTITY == newRegionCode) {
            currentMetadata = phoneUtil.getMetadataForNonGeographicalRegion(countryCode)
        } else if (newRegionCode != defaultCountry) {
            currentMetadata = getMetadataForRegion(newRegionCode)
        }
        val countryCodeString = countryCode.toString()
        prefixBeforeNationalNumber.append(countryCodeString).append(SEPARATOR_BEFORE_NATIONAL_NUMBER)
        // When we have successfully extracted the IDD, the previously extracted NDD should be cleared
        // because it is no longer valid.
        extractedNationalPrefix = ""
        return true
    }

    // Accrues digits and the plus sign to accruedInputWithoutFormatting for later use. If nextChar
    // contains a digit in non-ASCII format (e.g. the full-width version of digits), it is first
    // normalized to the ASCII version. The return value is nextChar itself, or its normalized
    // version, if nextChar is a digit in non-ASCII format. This method assumes its input is either a
    // digit or the plus sign.
    private fun normalizeAndAccrueDigitsAndPlusSign(nextChar: Char, rememberPosition: Boolean): Char {
        val normalizedChar: Char
        if (nextChar == PhoneNumberUtil.PLUS_SIGN) {
            normalizedChar = nextChar
            accruedInputWithoutFormatting.append(nextChar)
        } else {
            val radix = 10
            normalizedChar = (nextChar.digitToIntOrNull(radix) ?: -1).digitToChar(radix)
            accruedInputWithoutFormatting.append(normalizedChar)
            nationalNumber.append(normalizedChar)
        }
        if (rememberPosition) {
            positionToRemember = accruedInputWithoutFormatting.length
        }
        return normalizedChar
    }

    private fun inputDigitHelper(nextChar: Char): String {
        TODO()
        // Note that formattingTemplate is not guaranteed to have a value, it could be empty, e.g.
        // when the next digit is entered after extracting an IDD or NDD.
        /*
        val digitMatcher = DIGIT_PATTERN.matcher(formattingTemplate)
        return if (digitMatcher.find(lastMatchPosition)) {
            val tempTemplate = digitMatcher.replaceFirst(nextChar.toString())
            formattingTemplate.replace(0, tempTemplate.length, tempTemplate)
            lastMatchPosition = digitMatcher.start()
            formattingTemplate.substring(0, lastMatchPosition + 1)
        } else {
            if (possibleFormats.size == 1) {
                // More digits are entered than we could handle, and there are no other valid patterns to
                // try.
                ableToFormat = false
            } // else, we just reset the formatting pattern.
            currentFormattingPattern = ""
            accruedInput.toString()
        }
        */
    }

    companion object {
        // Character used when appropriate to separate a prefix, such as a long NDD or a country calling
        // code, from the national number.
        private const val SEPARATOR_BEFORE_NATIONAL_NUMBER = ' '
        private val EMPTY_METADATA = newBuilder().setId("<ignored>").setInternationalPrefix("NA").build()

        // A pattern that is used to determine if a numberFormat under availableFormats is eligible to be
        // used by the AYTF. It is eligible when the format element under numberFormat contains groups of
        // the dollar sign followed by a single digit, separated by valid phone number punctuation. This
        // prevents invalid punctuation (such as the star sign in Israeli star numbers) getting into the
        // output of the AYTF. We require that the first group is present in the output pattern to ensure
        // no data is lost while formatting; when we format as you type, this should always be the case.
        private val ELIGIBLE_FORMAT_PATTERN = Regex(
            "[" + PhoneNumberUtil.VALID_PUNCTUATION + "]*" + "\\$1" + "[" + PhoneNumberUtil.VALID_PUNCTUATION + "]*(\\$\\d" + "[" + PhoneNumberUtil.VALID_PUNCTUATION + "]*)*"
        )

        // A set of characters that, if found in a national prefix formatting rules, are an indicator to
        // us that we should separate the national prefix from the number when formatting.
        private val NATIONAL_PREFIX_SEPARATORS_PATTERN = Regex("[- ]")

        // This is the minimum length of national number accrued that is required to trigger the
        // formatter. The first element of the leadingDigitsPattern of each numberFormat contains a
        // regular expression that matches up to this number of digits.
        private const val MIN_LEADING_DIGITS_LENGTH = 3

        // The digits that have not been entered yet will be represented by a \u2008, the punctuation
        // space.
        private const val DIGIT_PLACEHOLDER = "\u2008"
        private val DIGIT_PATTERN = Regex(DIGIT_PLACEHOLDER)
    }
}
