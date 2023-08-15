/*
 * Copyright (C) 2011 The Libphonenumber Authors
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
import io.michaelrocks.libphonenumber.kotlin.PhoneNumberUtil.Leniency
import io.michaelrocks.libphonenumber.kotlin.Phonenumber.PhoneNumber
import io.michaelrocks.libphonenumber.kotlin.Phonenumber.PhoneNumber.CountryCodeSource
import io.michaelrocks.libphonenumber.kotlin.metadata.init.ClassPathResourceMetadataLoader
import java.util.*

/**
 * Tests for [PhoneNumberMatcher]. This only tests basic functionality based on test metadata.
 *
 * @see PhoneNumberUtilTest {@link PhoneNumberUtilTest} for the origin of the test data
 */
class PhoneNumberMatcherTest : TestMetadataTestCase() {

    override val metadataLoader: MetadataLoader
        get() = ClassPathResourceMetadataLoader()

    @Throws(Exception::class)
    fun testContainsMoreThanOneSlashInNationalNumber() {
        // A date should return true.
        var number = PhoneNumber()
        number.setCountryCode(1)
        number.setCountryCodeSource(CountryCodeSource.FROM_DEFAULT_COUNTRY)
        var candidate = "1/05/2013"
        assertTrue(PhoneNumberMatcher.containsMoreThanOneSlashInNationalNumber(number, candidate))

        // Here, the country code source thinks it started with a country calling code, but this is not
        // the same as the part before the slash, so it's still true.
        number = PhoneNumber()
        number.setCountryCode(274)
        number.setCountryCodeSource(CountryCodeSource.FROM_NUMBER_WITHOUT_PLUS_SIGN)
        candidate = "27/4/2013"
        assertTrue(PhoneNumberMatcher.containsMoreThanOneSlashInNationalNumber(number, candidate))

        // Now it should be false, because the first slash is after the country calling code.
        number = PhoneNumber()
        number.setCountryCode(49)
        number.setCountryCodeSource(CountryCodeSource.FROM_NUMBER_WITH_PLUS_SIGN)
        candidate = "49/69/2013"
        assertFalse(PhoneNumberMatcher.containsMoreThanOneSlashInNationalNumber(number, candidate))
        number = PhoneNumber()
        number.setCountryCode(49)
        number.setCountryCodeSource(CountryCodeSource.FROM_NUMBER_WITHOUT_PLUS_SIGN)
        candidate = "+49/69/2013"
        assertFalse(PhoneNumberMatcher.containsMoreThanOneSlashInNationalNumber(number, candidate))
        candidate = "+ 49/69/2013"
        assertFalse(PhoneNumberMatcher.containsMoreThanOneSlashInNationalNumber(number, candidate))
        candidate = "+ 49/69/20/13"
        assertTrue(PhoneNumberMatcher.containsMoreThanOneSlashInNationalNumber(number, candidate))

        // Here, the first group is not assumed to be the country calling code, even though it is the
        // same as it, so this should return true.
        number = PhoneNumber()
        number.setCountryCode(49)
        number.setCountryCodeSource(CountryCodeSource.FROM_DEFAULT_COUNTRY)
        candidate = "49/69/2013"
        assertTrue(PhoneNumberMatcher.containsMoreThanOneSlashInNationalNumber(number, candidate))
    }

    /** See [PhoneNumberUtilTest.testParseNationalNumber].  */
    @Throws(Exception::class)
    fun testFindNationalNumber() {
        // same cases as in testParseNationalNumber
        doTestFindInContext("033316005", RegionCode.NZ)
        // ("33316005", RegionCode.NZ) is omitted since the national prefix is obligatory for these
        // types of numbers in New Zealand.
        // National prefix attached and some formatting present.
        doTestFindInContext("03-331 6005", RegionCode.NZ)
        doTestFindInContext("03 331 6005", RegionCode.NZ)
        // Testing international prefixes.
        // Should strip country code.
        doTestFindInContext("0064 3 331 6005", RegionCode.NZ)
        // Try again, but this time we have an international number with Region Code US. It should
        // recognize the country code and parse accordingly.
        doTestFindInContext("01164 3 331 6005", RegionCode.US)
        doTestFindInContext("+64 3 331 6005", RegionCode.US)
        doTestFindInContext("64(0)64123456", RegionCode.NZ)
        // Check that using a "/" is fine in a phone number.
        // Note that real Polish numbers do *not* start with a 0.
        doTestFindInContext("0123/456789", RegionCode.PL)
        doTestFindInContext("123-456-7890", RegionCode.US)
    }

    /** See [PhoneNumberUtilTest.testParseWithInternationalPrefixes].  */
    @Throws(Exception::class)
    fun testFindWithInternationalPrefixes() {
        doTestFindInContext("+1 (650) 333-6000", RegionCode.NZ)
        doTestFindInContext("1-650-333-6000", RegionCode.US)
        // Calling the US number from Singapore by using different service providers
        // 1st test: calling using SingTel IDD service (IDD is 001)
        doTestFindInContext("0011-650-333-6000", RegionCode.SG)
        // 2nd test: calling using StarHub IDD service (IDD is 008)
        doTestFindInContext("0081-650-333-6000", RegionCode.SG)
        // 3rd test: calling using SingTel V019 service (IDD is 019)
        doTestFindInContext("0191-650-333-6000", RegionCode.SG)
        // Calling the US number from Poland
        doTestFindInContext("0~01-650-333-6000", RegionCode.PL)
        // Using "++" at the start.
        doTestFindInContext("++1 (650) 333-6000", RegionCode.PL)
        // Using a full-width plus sign.
        doTestFindInContext("\uFF0B1 (650) 333-6000", RegionCode.SG)
        // The whole number, including punctuation, is here represented in full-width form.
        doTestFindInContext(
            "\uFF0B\uFF11\u3000\uFF08\uFF16\uFF15\uFF10\uFF09"
                    + "\u3000\uFF13\uFF13\uFF13\uFF0D\uFF16\uFF10\uFF10\uFF10",
            RegionCode.SG
        )
    }

    /** See [PhoneNumberUtilTest.testParseWithLeadingZero].  */
    @Throws(Exception::class)
    fun testFindWithLeadingZero() {
        doTestFindInContext("+39 02-36618 300", RegionCode.NZ)
        doTestFindInContext("02-36618 300", RegionCode.IT)
        doTestFindInContext("312 345 678", RegionCode.IT)
    }

    /** See [PhoneNumberUtilTest.testParseNationalNumberArgentina].  */
    @Throws(Exception::class)
    fun testFindNationalNumberArgentina() {
        // Test parsing mobile numbers of Argentina.
        doTestFindInContext("+54 9 343 555 1212", RegionCode.AR)
        doTestFindInContext("0343 15 555 1212", RegionCode.AR)
        doTestFindInContext("+54 9 3715 65 4320", RegionCode.AR)
        doTestFindInContext("03715 15 65 4320", RegionCode.AR)

        // Test parsing fixed-line numbers of Argentina.
        doTestFindInContext("+54 11 3797 0000", RegionCode.AR)
        doTestFindInContext("011 3797 0000", RegionCode.AR)
        doTestFindInContext("+54 3715 65 4321", RegionCode.AR)
        doTestFindInContext("03715 65 4321", RegionCode.AR)
        doTestFindInContext("+54 23 1234 0000", RegionCode.AR)
        doTestFindInContext("023 1234 0000", RegionCode.AR)
    }

    /** See [PhoneNumberUtilTest.testParseWithXInNumber].  */
    @Throws(Exception::class)
    fun testFindWithXInNumber() {
        doTestFindInContext("(0xx) 123456789", RegionCode.AR)
        // A case where x denotes both carrier codes and extension symbol.
        doTestFindInContext("(0xx) 123456789 x 1234", RegionCode.AR)

        // This test is intentionally constructed such that the number of digit after xx is larger than
        // 7, so that the number won't be mistakenly treated as an extension, as we allow extensions up
        // to 7 digits. This assumption is okay for now as all the countries where a carrier selection
        // code is written in the form of xx have a national significant number of length larger than 7.
        doTestFindInContext("011xx5481429712", RegionCode.US)
    }

    /** See [PhoneNumberUtilTest.testParseNumbersMexico].  */
    @Throws(Exception::class)
    fun testFindNumbersMexico() {
        // Test parsing fixed-line numbers of Mexico.
        doTestFindInContext("+52 (449)978-0001", RegionCode.MX)
        doTestFindInContext("01 (449)978-0001", RegionCode.MX)
        doTestFindInContext("(449)978-0001", RegionCode.MX)

        // Test parsing mobile numbers of Mexico.
        doTestFindInContext("+52 1 33 1234-5678", RegionCode.MX)
        doTestFindInContext("044 (33) 1234-5678", RegionCode.MX)
        doTestFindInContext("045 33 1234-5678", RegionCode.MX)
    }

    /** See [PhoneNumberUtilTest.testParseNumbersWithPlusWithNoRegion].  */
    @Throws(Exception::class)
    fun testFindNumbersWithPlusWithNoRegion() {
        // RegionCode.ZZ is allowed only if the number starts with a '+' - then the country code can be
        // calculated.
        doTestFindInContext("+64 3 331 6005", RegionCode.ZZ)
        // Null is also allowed for the region code in these cases.
        doTestFindInContext("+64 3 331 6005", null)
    }

    /** See [PhoneNumberUtilTest.testParseExtensions].  */
    @Throws(Exception::class)
    fun testFindExtensions() {
        doTestFindInContext("03 331 6005 ext 3456", RegionCode.NZ)
        doTestFindInContext("03-3316005x3456", RegionCode.NZ)
        doTestFindInContext("03-3316005 int.3456", RegionCode.NZ)
        doTestFindInContext("03 3316005 #3456", RegionCode.NZ)
        doTestFindInContext("0~0 1800 7493 524", RegionCode.PL)
        doTestFindInContext("(1800) 7493.524", RegionCode.US)
        // Check that the last instance of an extension token is matched.
        doTestFindInContext("0~0 1800 7493 524 ~1234", RegionCode.PL)
        // Verifying bug-fix where the last digit of a number was previously omitted if it was a 0 when
        // extracting the extension. Also verifying a few different cases of extensions.
        doTestFindInContext("+44 2034567890x456", RegionCode.NZ)
        doTestFindInContext("+44 2034567890x456", RegionCode.GB)
        doTestFindInContext("+44 2034567890 x456", RegionCode.GB)
        doTestFindInContext("+44 2034567890 X456", RegionCode.GB)
        doTestFindInContext("+44 2034567890 X 456", RegionCode.GB)
        doTestFindInContext("+44 2034567890 X  456", RegionCode.GB)
        doTestFindInContext("+44 2034567890  X 456", RegionCode.GB)
        doTestFindInContext("(800) 901-3355 x 7246433", RegionCode.US)
        doTestFindInContext("(800) 901-3355 , ext 7246433", RegionCode.US)
        doTestFindInContext("(800) 901-3355 ,extension 7246433", RegionCode.US)
        // The next test differs from PhoneNumberUtil -> when matching we don't consider a lone comma to
        // indicate an extension, although we accept it when parsing.
        doTestFindInContext("(800) 901-3355 ,x 7246433", RegionCode.US)
        doTestFindInContext("(800) 901-3355 ext: 7246433", RegionCode.US)
    }

    @Throws(Exception::class)
    fun testFindInterspersedWithSpace() {
        doTestFindInContext("0 3   3 3 1   6 0 0 5", RegionCode.NZ)
    }

    /**
     * Test matching behavior when starting in the middle of a phone number.
     */
    @Throws(Exception::class)
    fun testIntermediateParsePositions() {
        val text = "Call 033316005  or 032316005!"
        //             |    |    |    |    |    |
        //             0    5   10   15   20   25

        // Iterate over all possible indices.
        for (i in 0..5) {
            assertEqualRange(text, i, 5, 14)
        }
        // 7 and 8 digits in a row are still parsed as number.
        assertEqualRange(text, 6, 6, 14)
        assertEqualRange(text, 7, 7, 14)
        // Anything smaller is skipped to the second instance.
        for (i in 8..19) {
            assertEqualRange(text, i, 19, 28)
        }
    }

    @Throws(Exception::class)
    fun testFourMatchesInARow() {
        val number1 = "415-666-7777"
        val number2 = "800-443-1223"
        val number3 = "212-443-1223"
        val number4 = "650-443-1223"
        val text = "$number1 - $number2 - $number3 - $number4"
        val iterator: Iterator<PhoneNumberMatch> = phoneUtil.findNumbers(text, RegionCode.US).iterator()
        var match: PhoneNumberMatch? = if (iterator.hasNext()) iterator.next() else null
        assertMatchProperties(match, text, number1, RegionCode.US)
        match = if (iterator.hasNext()) iterator.next() else null
        assertMatchProperties(match, text, number2, RegionCode.US)
        match = if (iterator.hasNext()) iterator.next() else null
        assertMatchProperties(match, text, number3, RegionCode.US)
        match = if (iterator.hasNext()) iterator.next() else null
        assertMatchProperties(match, text, number4, RegionCode.US)
    }

    @Throws(Exception::class)
    fun testMatchesFoundWithMultipleSpaces() {
        val number1 = "(415) 666-7777"
        val number2 = "(800) 443-1223"
        val text = "$number1 $number2"
        val iterator: Iterator<PhoneNumberMatch> = phoneUtil.findNumbers(text, RegionCode.US).iterator()
        var match: PhoneNumberMatch? = if (iterator.hasNext()) iterator.next() else null
        assertMatchProperties(match, text, number1, RegionCode.US)
        match = if (iterator.hasNext()) iterator.next() else null
        assertMatchProperties(match, text, number2, RegionCode.US)
    }

    @Throws(Exception::class)
    fun testMatchWithSurroundingZipcodes() {
        var number = "415-666-7777"
        val zipPreceding = "My address is CA 34215 - $number is my number."
        var iterator: Iterator<PhoneNumberMatch?> = phoneUtil.findNumbers(zipPreceding, RegionCode.US).iterator()
        val match: PhoneNumberMatch? = if (iterator.hasNext()) iterator.next() else null
        assertMatchProperties(match, zipPreceding, number, RegionCode.US)

        // Now repeat, but this time the phone number has spaces in it. It should still be found.
        number = "(415) 666 7777"
        val zipFollowing = "My number is $number. 34215 is my zip-code."
        iterator = phoneUtil.findNumbers(zipFollowing, RegionCode.US).iterator()
        val matchWithSpaces: PhoneNumberMatch? = if (iterator.hasNext()) iterator.next() else null
        assertMatchProperties(matchWithSpaces, zipFollowing, number, RegionCode.US)
    }

    @Throws(Exception::class)
    fun testIsLatinLetter() {
        assertTrue(PhoneNumberMatcher.isLatinLetter('c'))
        assertTrue(PhoneNumberMatcher.isLatinLetter('C'))
        assertTrue(PhoneNumberMatcher.isLatinLetter('\u00C9'))
        assertTrue(PhoneNumberMatcher.isLatinLetter('\u0301')) // Combining acute accent
        // Punctuation, digits and white-space are not considered "latin letters".
        assertFalse(PhoneNumberMatcher.isLatinLetter(':'))
        assertFalse(PhoneNumberMatcher.isLatinLetter('5'))
        assertFalse(PhoneNumberMatcher.isLatinLetter('-'))
        assertFalse(PhoneNumberMatcher.isLatinLetter('.'))
        assertFalse(PhoneNumberMatcher.isLatinLetter(' '))
        assertFalse(PhoneNumberMatcher.isLatinLetter('\u6211')) // Chinese character
        assertFalse(PhoneNumberMatcher.isLatinLetter('\u306E')) // Hiragana letter no
    }

    @Throws(Exception::class)
    fun testMatchesWithSurroundingLatinChars() {
        val possibleOnlyContexts = ArrayList<NumberContext>()
        possibleOnlyContexts.add(NumberContext("abc", "def"))
        possibleOnlyContexts.add(NumberContext("abc", ""))
        possibleOnlyContexts.add(NumberContext("", "def"))
        // Latin capital letter e with an acute accent.
        possibleOnlyContexts.add(NumberContext("\u00C9", ""))
        // e with an acute accent decomposed (with combining mark).
        possibleOnlyContexts.add(NumberContext("e\u0301", ""))

        // Numbers should not be considered valid, if they are surrounded by Latin characters, but
        // should be considered possible.
        findMatchesInContexts(possibleOnlyContexts, false, true)
    }

    @Throws(Exception::class)
    fun testMoneyNotSeenAsPhoneNumber() {
        val possibleOnlyContexts = ArrayList<NumberContext>()
        possibleOnlyContexts.add(NumberContext("$", ""))
        possibleOnlyContexts.add(NumberContext("", "$"))
        possibleOnlyContexts.add(NumberContext("\u00A3", "")) // Pound sign
        possibleOnlyContexts.add(NumberContext("\u00A5", "")) // Yen sign
        findMatchesInContexts(possibleOnlyContexts, false, true)
    }

    @Throws(Exception::class)
    fun testPercentageNotSeenAsPhoneNumber() {
        val possibleOnlyContexts = ArrayList<NumberContext>()
        possibleOnlyContexts.add(NumberContext("", "%"))
        // Numbers followed by % should be dropped.
        findMatchesInContexts(possibleOnlyContexts, false, true)
    }

    @Throws(Exception::class)
    fun testPhoneNumberWithLeadingOrTrailingMoneyMatches() {
        // Because of the space after the 20 (or before the 100) these dollar amounts should not stop
        // the actual number from being found.
        val contexts = ArrayList<NumberContext>()
        contexts.add(NumberContext("$20 ", ""))
        contexts.add(NumberContext("", " 100$"))
        findMatchesInContexts(contexts, true, true)
    }

    @Throws(Exception::class)
    fun testMatchesWithSurroundingLatinCharsAndLeadingPunctuation() {
        // Contexts with trailing characters. Leading characters are okay here since the numbers we will
        // insert start with punctuation, but trailing characters are still not allowed.
        val possibleOnlyContexts = ArrayList<NumberContext>()
        possibleOnlyContexts.add(NumberContext("abc", "def"))
        possibleOnlyContexts.add(NumberContext("", "def"))
        possibleOnlyContexts.add(NumberContext("", "\u00C9"))

        // Numbers should not be considered valid, if they have trailing Latin characters, but should be
        // considered possible.
        val numberWithPlus = "+14156667777"
        val numberWithBrackets = "(415)6667777"
        findMatchesInContexts(possibleOnlyContexts, false, true, RegionCode.US, numberWithPlus)
        findMatchesInContexts(possibleOnlyContexts, false, true, RegionCode.US, numberWithBrackets)
        val validContexts = ArrayList<NumberContext>()
        validContexts.add(NumberContext("abc", ""))
        validContexts.add(NumberContext("\u00C9", ""))
        validContexts.add(NumberContext("\u00C9", ".")) // Trailing punctuation.
        validContexts.add(NumberContext("\u00C9", " def")) // Trailing white-space.

        // Numbers should be considered valid, since they start with punctuation.
        findMatchesInContexts(validContexts, true, true, RegionCode.US, numberWithPlus)
        findMatchesInContexts(validContexts, true, true, RegionCode.US, numberWithBrackets)
    }

    @Throws(Exception::class)
    fun testMatchesWithSurroundingChineseChars() {
        val validContexts = ArrayList<NumberContext>()
        validContexts.add(NumberContext("\u6211\u7684\u7535\u8BDD\u53F7\u7801\u662F", ""))
        validContexts.add(NumberContext("", "\u662F\u6211\u7684\u7535\u8BDD\u53F7\u7801"))
        validContexts.add(NumberContext("\u8BF7\u62E8\u6253", "\u6211\u5728\u660E\u5929"))

        // Numbers should be considered valid, since they are surrounded by Chinese.
        findMatchesInContexts(validContexts, true, true)
    }

    @Throws(Exception::class)
    fun testMatchesWithSurroundingPunctuation() {
        val validContexts = ArrayList<NumberContext>()
        validContexts.add(NumberContext("My number-", "")) // At end of text.
        validContexts.add(NumberContext("", ".Nice day.")) // At start of text.
        validContexts.add(NumberContext("Tel:", ".")) // Punctuation surrounds number.
        validContexts.add(NumberContext("Tel: ", " on Saturdays.")) // White-space is also fine.

        // Numbers should be considered valid, since they are surrounded by punctuation.
        findMatchesInContexts(validContexts, true, true)
    }

    @Throws(Exception::class)
    fun testMatchesMultiplePhoneNumbersSeparatedByPhoneNumberPunctuation() {
        val text = "Call 650-253-4561 -- 455-234-3451"
        val region = RegionCode.US
        val number1 = PhoneNumber()
        number1.setCountryCode(phoneUtil.getCountryCodeForRegion(region))
        number1.setNationalNumber(6502534561L)
        val match1 = PhoneNumberMatch(5, "650-253-4561", number1)
        val number2 = PhoneNumber()
        number2.setCountryCode(phoneUtil.getCountryCodeForRegion(region))
        number2.setNationalNumber(4552343451L)
        val match2 = PhoneNumberMatch(21, "455-234-3451", number2)
        val matches: Iterator<PhoneNumberMatch> = phoneUtil.findNumbers(text, region).iterator()
        assertEquals(match1, matches.next())
        assertEquals(match2, matches.next())
    }

    @Throws(Exception::class)
    fun testDoesNotMatchMultiplePhoneNumbersSeparatedWithNoWhiteSpace() {
        // No white-space found between numbers - neither is found.
        val text = "Call 650-253-4561--455-234-3451"
        val region = RegionCode.US
        assertTrue(hasNoMatches(phoneUtil.findNumbers(text, region)))
    }

    @Throws(Exception::class)
    fun testMatchesWithPossibleLeniency() {
        val testCases: MutableList<NumberTest> = ArrayList()
        testCases.addAll(Arrays.asList(*STRICT_GROUPING_CASES))
        testCases.addAll(Arrays.asList(*EXACT_GROUPING_CASES))
        testCases.addAll(Arrays.asList(*VALID_CASES))
        testCases.addAll(Arrays.asList(*POSSIBLE_ONLY_CASES))
        doTestNumberMatchesForLeniency(testCases, Leniency.POSSIBLE)
    }

    @Throws(Exception::class)
    fun testNonMatchesWithPossibleLeniency() {
        val testCases: MutableList<NumberTest> = ArrayList()
        testCases.addAll(Arrays.asList(*IMPOSSIBLE_CASES))
        doTestNumberNonMatchesForLeniency(testCases, Leniency.POSSIBLE)
    }

    @Throws(Exception::class)
    fun testMatchesWithValidLeniency() {
        val testCases: MutableList<NumberTest> = ArrayList()
        testCases.addAll(Arrays.asList(*STRICT_GROUPING_CASES))
        testCases.addAll(Arrays.asList(*EXACT_GROUPING_CASES))
        testCases.addAll(Arrays.asList(*VALID_CASES))
        doTestNumberMatchesForLeniency(testCases, Leniency.VALID)
    }

    @Throws(Exception::class)
    fun testNonMatchesWithValidLeniency() {
        val testCases: MutableList<NumberTest> = ArrayList()
        testCases.addAll(Arrays.asList(*IMPOSSIBLE_CASES))
        testCases.addAll(Arrays.asList(*POSSIBLE_ONLY_CASES))
        doTestNumberNonMatchesForLeniency(testCases, Leniency.VALID)
    }

    @Throws(Exception::class)
    fun testMatchesWithStrictGroupingLeniency() {
        val testCases: MutableList<NumberTest> = ArrayList()
        testCases.addAll(Arrays.asList(*STRICT_GROUPING_CASES))
        testCases.addAll(Arrays.asList(*EXACT_GROUPING_CASES))
        doTestNumberMatchesForLeniency(testCases, Leniency.STRICT_GROUPING)
    }

    @Throws(Exception::class)
    fun testNonMatchesWithStrictGroupLeniency() {
        val testCases: MutableList<NumberTest> = ArrayList()
        testCases.addAll(Arrays.asList(*IMPOSSIBLE_CASES))
        testCases.addAll(Arrays.asList(*POSSIBLE_ONLY_CASES))
        testCases.addAll(Arrays.asList(*VALID_CASES))
        doTestNumberNonMatchesForLeniency(testCases, Leniency.STRICT_GROUPING)
    }

    @Throws(Exception::class)
    fun testMatchesWithExactGroupingLeniency() {
        val testCases: MutableList<NumberTest> = ArrayList()
        testCases.addAll(Arrays.asList(*EXACT_GROUPING_CASES))
        doTestNumberMatchesForLeniency(testCases, Leniency.EXACT_GROUPING)
    }

    @Throws(Exception::class)
    fun testNonMatchesExactGroupLeniency() {
        val testCases: MutableList<NumberTest> = ArrayList()
        testCases.addAll(Arrays.asList(*IMPOSSIBLE_CASES))
        testCases.addAll(Arrays.asList(*POSSIBLE_ONLY_CASES))
        testCases.addAll(Arrays.asList(*VALID_CASES))
        testCases.addAll(Arrays.asList(*STRICT_GROUPING_CASES))
        doTestNumberNonMatchesForLeniency(testCases, Leniency.EXACT_GROUPING)
    }

    private fun doTestNumberMatchesForLeniency(testCases: List<NumberTest>, leniency: Leniency) {
        var noMatchFoundCount = 0
        var wrongMatchFoundCount = 0
        for (test in testCases) {
            val iterator: Iterator<PhoneNumberMatch> = findNumbersForLeniency(test.rawString, test.region, leniency)
            val match: PhoneNumberMatch? = if (iterator.hasNext()) iterator.next() else null
            if (match == null) {
                noMatchFoundCount++
                System.err.println("No match found in $test for leniency: $leniency")
            } else {
                if (test.rawString != match.rawString()) {
                    wrongMatchFoundCount++
                    System.err.println(
                        "Found wrong match in test " + test.toString()
                                + ". Found " + match.rawString()
                    )
                }
            }
        }
        assertEquals(0, noMatchFoundCount)
        assertEquals(0, wrongMatchFoundCount)
    }

    private fun doTestNumberNonMatchesForLeniency(testCases: List<NumberTest>, leniency: Leniency) {
        var matchFoundCount = 0
        for (test in testCases) {
            val iterator: Iterator<PhoneNumberMatch> = findNumbersForLeniency(test.rawString, test.region, leniency)
            val match: PhoneNumberMatch? = if (iterator.hasNext()) iterator.next() else null
            if (match != null) {
                matchFoundCount++
                System.err.println("Match found in $test for leniency: $leniency")
            }
        }
        assertEquals(0, matchFoundCount)
    }

    /**
     * Helper method which tests the contexts provided and ensures that:
     * -- if isValid is true, they all find a test number inserted in the middle when leniency of
     * matching is set to VALID; else no test number should be extracted at that leniency level
     * -- if isPossible is true, they all find a test number inserted in the middle when leniency of
     * matching is set to POSSIBLE; else no test number should be extracted at that leniency level
     */
    private fun findMatchesInContexts(
        contexts: List<NumberContext>, isValid: Boolean,
        isPossible: Boolean, region: String, number: String
    ) {
        if (isValid) {
            doTestInContext(number, region, contexts, Leniency.VALID)
        } else {
            for (context in contexts) {
                val text = context.leadingText + number + context.trailingText
                assertTrue(
                    "Should not have found a number in $text",
                    hasNoMatches(phoneUtil.findNumbers(text, region))
                )
            }
        }
        if (isPossible) {
            doTestInContext(number, region, contexts, Leniency.POSSIBLE)
        } else {
            for (context in contexts) {
                val text = context.leadingText + number + context.trailingText
                assertTrue(
                    "Should not have found a number in $text",
                    hasNoMatches(phoneUtil.findNumbers(text, region, Leniency.POSSIBLE, Long.MAX_VALUE))
                )
            }
        }
    }

    /**
     * Variant of findMatchesInContexts that uses a default number and region.
     */
    private fun findMatchesInContexts(
        contexts: List<NumberContext>, isValid: Boolean,
        isPossible: Boolean
    ) {
        val region = RegionCode.US
        val number = "415-666-7777"
        findMatchesInContexts(contexts, isValid, isPossible, region, number)
    }

    @Throws(Exception::class)
    fun testNonMatchingBracketsAreInvalid() {
        // The digits up to the ", " form a valid US number, but it shouldn't be matched as one since
        // there was a non-matching bracket present.
        assertTrue(
            hasNoMatches(
                phoneUtil.findNumbers(
                    "80.585 [79.964, 81.191]", RegionCode.US
                )
            )
        )

        // The trailing "]" is thrown away before parsing, so the resultant number, while a valid US
        // number, does not have matching brackets.
        assertTrue(
            hasNoMatches(
                phoneUtil.findNumbers(
                    "80.585 [79.964]", RegionCode.US
                )
            )
        )
        assertTrue(
            hasNoMatches(
                phoneUtil.findNumbers(
                    "80.585 ((79.964)", RegionCode.US
                )
            )
        )

        // This case has too many sets of brackets to be valid.
        assertTrue(
            hasNoMatches(
                phoneUtil.findNumbers(
                    "(80).(585) (79).(9)64", RegionCode.US
                )
            )
        )
    }

    @Throws(Exception::class)
    fun testNoMatchIfRegionIsNull() {
        // Fail on non-international prefix if region code is null.
        assertTrue(
            hasNoMatches(
                phoneUtil.findNumbers(
                    "Random text body - number is 0331 6005, see you there", null
                )
            )
        )
    }

    @Throws(Exception::class)
    fun testNoMatchInEmptyString() {
        assertTrue(hasNoMatches(phoneUtil.findNumbers("", RegionCode.US)))
        assertTrue(hasNoMatches(phoneUtil.findNumbers("  ", RegionCode.US)))
    }

    @Throws(Exception::class)
    fun testNoMatchIfNoNumber() {
        assertTrue(
            hasNoMatches(
                phoneUtil.findNumbers(
                    "Random text body - number is foobar, see you there", RegionCode.US
                )
            )
        )
    }

    @Throws(Exception::class)
    fun testSequences() {
        // Test multiple occurrences.
        val text = "Call 033316005  or 032316005!"
        val region = RegionCode.NZ
        val number1 = PhoneNumber()
        number1.setCountryCode(phoneUtil.getCountryCodeForRegion(region))
        number1.setNationalNumber(33316005)
        val match1 = PhoneNumberMatch(5, "033316005", number1)
        val number2 = PhoneNumber()
        number2.setCountryCode(phoneUtil.getCountryCodeForRegion(region))
        number2.setNationalNumber(32316005)
        val match2 = PhoneNumberMatch(19, "032316005", number2)
        val matches: Iterator<PhoneNumberMatch> =
            phoneUtil.findNumbers(text, region, Leniency.POSSIBLE, Long.MAX_VALUE).iterator()
        assertEquals(match1, matches.next())
        assertEquals(match2, matches.next())
    }

    @Throws(Exception::class)
    fun testNullInput() {
        assertTrue(hasNoMatches(phoneUtil.findNumbers(null, RegionCode.US)))
        assertTrue(hasNoMatches(phoneUtil.findNumbers(null, null)))
    }

    @Throws(Exception::class)
    fun testMaxMatches() {
        // Set up text with 100 valid phone numbers.
        val numbers = StringBuilder()
        for (i in 0..99) {
            numbers.append("My info: 415-666-7777,")
        }

        // Matches all 100. Max only applies to failed cases.
        val expected: MutableList<PhoneNumber> = ArrayList(100)
        val number = phoneUtil.parse("+14156667777", null)
        for (i in 0..99) {
            expected.add(number)
        }
        val iterable: Iterable<PhoneNumberMatch> =
            phoneUtil.findNumbers(numbers.toString(), RegionCode.US, Leniency.VALID, 10)
        val actual: MutableList<PhoneNumber> = ArrayList(100)
        for (match in iterable) {
            actual.add(match.number())
        }
        assertEquals(expected, actual)
    }

    @Throws(Exception::class)
    fun testMaxMatchesInvalid() {
        // Set up text with 10 invalid phone numbers followed by 100 valid.
        val numbers = StringBuilder()
        for (i in 0..9) {
            numbers.append("My address 949-8945-0")
        }
        for (i in 0..99) {
            numbers.append("My info: 415-666-7777,")
        }
        val iterable: Iterable<PhoneNumberMatch> =
            phoneUtil.findNumbers(numbers.toString(), RegionCode.US, Leniency.VALID, 10)
        assertFalse(iterable.iterator().hasNext())
    }

    @Throws(Exception::class)
    fun testMaxMatchesMixed() {
        // Set up text with 100 valid numbers inside an invalid number.
        val numbers = StringBuilder()
        for (i in 0..99) {
            numbers.append("My info: 415-666-7777 123 fake street")
        }

        // Only matches the first 10 despite there being 100 numbers due to max matches.
        val expected: MutableList<PhoneNumber> = ArrayList(100)
        val number = phoneUtil.parse("+14156667777", null)
        for (i in 0..9) {
            expected.add(number)
        }
        val iterable: Iterable<PhoneNumberMatch> =
            phoneUtil.findNumbers(numbers.toString(), RegionCode.US, Leniency.VALID, 10)
        val actual: MutableList<PhoneNumber> = ArrayList(100)
        for (match in iterable) {
            actual.add(match.number())
        }
        assertEquals(expected, actual)
    }

    @Throws(Exception::class)
    fun testNonPlusPrefixedNumbersNotFoundForInvalidRegion() {
        // Does not start with a "+", we won't match it.
        val iterable: Iterable<PhoneNumberMatch> = phoneUtil.findNumbers("1 456 764 156", RegionCode.ZZ)
        val iterator: Iterator<PhoneNumberMatch> = iterable.iterator()
        assertFalse(iterator.hasNext())
        try {
            iterator.next()
            fail("Violation of the Iterator contract.")
        } catch (e: NoSuchElementException) { /* Success */
        }
        assertFalse(iterator.hasNext())
    }

    @Throws(Exception::class)
    fun testEmptyIteration() {
        val iterable: Iterable<PhoneNumberMatch> = phoneUtil.findNumbers("", RegionCode.ZZ)
        val iterator: Iterator<PhoneNumberMatch> = iterable.iterator()
        assertFalse(iterator.hasNext())
        assertFalse(iterator.hasNext())
        try {
            iterator.next()
            fail("Violation of the Iterator contract.")
        } catch (e: NoSuchElementException) { /* Success */
        }
        assertFalse(iterator.hasNext())
    }

    @Throws(Exception::class)
    fun testSingleIteration() {
        val iterable: Iterable<PhoneNumberMatch> = phoneUtil.findNumbers("+14156667777", RegionCode.ZZ)

        // With hasNext() -> next().
        var iterator: Iterator<PhoneNumberMatch?> = iterable.iterator()
        // Double hasNext() to ensure it does not advance.
        assertTrue(iterator.hasNext())
        assertTrue(iterator.hasNext())
        assertNotNull(iterator.next())
        assertFalse(iterator.hasNext())
        try {
            iterator.next()
            fail("Violation of the Iterator contract.")
        } catch (e: NoSuchElementException) { /* Success */
        }
        assertFalse(iterator.hasNext())

        // With next() only.
        iterator = iterable.iterator()
        assertNotNull(iterator.next())
        try {
            iterator.next()
            fail("Violation of the Iterator contract.")
        } catch (e: NoSuchElementException) { /* Success */
        }
    }

    @Throws(Exception::class)
    fun testDoubleIteration() {
        val iterable: Iterable<PhoneNumberMatch> =
            phoneUtil.findNumbers("+14156667777 foobar +14156667777 ", RegionCode.ZZ)

        // With hasNext() -> next().
        var iterator: Iterator<PhoneNumberMatch?> = iterable.iterator()
        // Double hasNext() to ensure it does not advance.
        assertTrue(iterator.hasNext())
        assertTrue(iterator.hasNext())
        assertNotNull(iterator.next())
        assertTrue(iterator.hasNext())
        assertTrue(iterator.hasNext())
        assertNotNull(iterator.next())
        assertFalse(iterator.hasNext())
        try {
            iterator.next()
            fail("Violation of the Iterator contract.")
        } catch (e: NoSuchElementException) { /* Success */
        }
        assertFalse(iterator.hasNext())

        // With next() only.
        iterator = iterable.iterator()
        assertNotNull(iterator.next())
        assertNotNull(iterator.next())
        try {
            iterator.next()
            fail("Violation of the Iterator contract.")
        } catch (e: NoSuchElementException) { /* Success */
        }
    }

//    /**
//     * Ensures that [Iterator.remove] is not supported and that calling it does not
//     * change iteration behavior.
//     */
//    @Throws(Exception::class)
//    fun testRemovalNotSupported() {
//        val iterable: Iterable<PhoneNumberMatch> = phoneUtil.findNumbers("+14156667777", RegionCode.ZZ)
//        val iterator: Iterator<PhoneNumberMatch> = iterable.iterator()
//        try {
//            iterator.remove()
//            fail("Iterator must not support remove.")
//        } catch (e: UnsupportedOperationException) { /* success */
//        }
//        assertTrue(iterator.hasNext())
//        try {
//            iterator.remove()
//            fail("Iterator must not support remove.")
//        } catch (e: UnsupportedOperationException) { /* success */
//        }
//        assertNotNull(iterator.next())
//        try {
//            iterator.remove()
//            fail("Iterator must not support remove.")
//        } catch (e: UnsupportedOperationException) { /* success */
//        }
//        assertFalse(iterator.hasNext())
//    }

    /**
     * Asserts that the expected match is non-null, and that the raw string and expected
     * proto buffer are set appropriately.
     */
    @Throws(Exception::class)
    private fun assertMatchProperties(
        match: PhoneNumberMatch?, text: String, number: String, region: String
    ) {
        val expectedResult = phoneUtil.parse(number, region)
        assertNotNull("Did not find a number in '$text'; expected $number", match)
        assertEquals(expectedResult, match!!.number())
        assertEquals(number, match.rawString())
    }

    /**
     * Asserts that another number can be found in `text` starting at `index`, and that
     * its corresponding range is `[start, end)`.
     */
    private fun assertEqualRange(text: CharSequence, index: Int, start: Int, end: Int) {
        val sub = text.subSequence(index, text.length)
        val matches: Iterator<PhoneNumberMatch> =
            phoneUtil.findNumbers(sub, RegionCode.NZ, Leniency.POSSIBLE, Long.MAX_VALUE).iterator()
        assertTrue(matches.hasNext())
        val match: PhoneNumberMatch = matches.next()
        assertEquals(start - index, match.start())
        assertEquals(end - index, match.end())
        assertEquals(sub.subSequence(match.start(), match.end()).toString(), match.rawString())
    }

    /**
     * Tests numbers found by [PhoneNumberUtil.findNumbers] in various
     * textual contexts.
     *
     * @param number the number to test and the corresponding region code to use
     */
    @Throws(Exception::class)
    private fun doTestFindInContext(number: String, defaultCountry: String?) {
        findPossibleInContext(number, defaultCountry)
        val parsed = phoneUtil.parse(number, defaultCountry)
        if (phoneUtil.isValidNumber(parsed)) {
            findValidInContext(number, defaultCountry)
        }
    }

    /**
     * Tests valid numbers in contexts that should pass for [Leniency.POSSIBLE].
     */
    private fun findPossibleInContext(number: String, defaultCountry: String?) {
        val contextPairs = ArrayList<NumberContext>()
        contextPairs.add(NumberContext("", "")) // no context
        contextPairs.add(NumberContext("   ", "\t")) // whitespace only
        contextPairs.add(NumberContext("Hello ", "")) // no context at end
        contextPairs.add(NumberContext("", " to call me!")) // no context at start
        contextPairs.add(NumberContext("Hi there, call ", " to reach me!")) // no context at start
        contextPairs.add(NumberContext("Hi there, call ", ", or don't")) // with commas
        // Three examples without whitespace around the number.
        contextPairs.add(NumberContext("Hi call", ""))
        contextPairs.add(NumberContext("", "forme"))
        contextPairs.add(NumberContext("Hi call", "forme"))
        // With other small numbers.
        contextPairs.add(NumberContext("It's cheap! Call ", " before 6:30"))
        // With a second number later.
        contextPairs.add(NumberContext("Call ", " or +1800-123-4567!"))
        contextPairs.add(NumberContext("Call me on June 2 at", "")) // with a Month-Day date
        // With publication pages.
        contextPairs.add(
            NumberContext(
                "As quoted by Alfonso 12-15 (2009), you may call me at ", ""
            )
        )
        contextPairs.add(
            NumberContext(
                "As quoted by Alfonso et al. 12-15 (2009), you may call me at ", ""
            )
        )
        // With dates, written in the American style.
        contextPairs.add(
            NumberContext(
                "As I said on 03/10/2011, you may call me at ", ""
            )
        )
        // With trailing numbers after a comma. The 45 should not be considered an extension.
        contextPairs.add(NumberContext("", ", 45 days a year"))
        // When matching we don't consider semicolon along with legitimate extension symbol to indicate
        // an extension. The 7246433 should not be considered an extension.
        contextPairs.add(NumberContext("", ";x 7246433"))
        // With a postfix stripped off as it looks like the start of another number.
        contextPairs.add(NumberContext("Call ", "/x12 more"))
        doTestInContext(number, defaultCountry, contextPairs, Leniency.POSSIBLE)
    }

    /**
     * Tests valid numbers in contexts that fail for [Leniency.POSSIBLE] but are valid for
     * [Leniency.VALID].
     */
    private fun findValidInContext(number: String, defaultCountry: String?) {
        val contextPairs = ArrayList<NumberContext>()
        // With other small numbers.
        contextPairs.add(NumberContext("It's only 9.99! Call ", " to buy"))
        // With a number Day.Month.Year date.
        contextPairs.add(NumberContext("Call me on 21.6.1984 at ", ""))
        // With a number Month/Day date.
        contextPairs.add(NumberContext("Call me on 06/21 at ", ""))
        // With a number Day.Month date.
        contextPairs.add(NumberContext("Call me on 21.6. at ", ""))
        // With a number Month/Day/Year date.
        contextPairs.add(NumberContext("Call me on 06/21/84 at ", ""))
        doTestInContext(number, defaultCountry, contextPairs, Leniency.VALID)
    }

    private fun doTestInContext(
        number: String, defaultCountry: String?,
        contextPairs: List<NumberContext>, leniency: Leniency
    ) {
        for (context in contextPairs) {
            val prefix = context.leadingText
            val text = prefix + number + context.trailingText
            val start = prefix.length
            val end = start + number.length
            val iterator: Iterator<PhoneNumberMatch> =
                phoneUtil.findNumbers(text, defaultCountry, leniency, Long.MAX_VALUE).iterator()
            val match: PhoneNumberMatch? = if (iterator.hasNext()) iterator.next() else null
            assertNotNull("Did not find a number in '$text'; expected '$number'", match)
            val extracted = text.subSequence(match!!.start(), match.end())
            assertTrue(
                "Unexpected phone region in '$text'; extracted '$extracted'",
                start == match.start() && end == match.end()
            )
            assertTrue(number.contentEquals(extracted))
            assertTrue(match.rawString().contentEquals(extracted))
            ensureTermination(text, defaultCountry, leniency)
        }
    }

    /**
     * Exhaustively searches for phone numbers from each index within `text` to test that
     * finding matches always terminates.
     */
    private fun ensureTermination(text: String, defaultCountry: String?, leniency: Leniency) {
        for (index in 0..text.length) {
            val sub = text.substring(index)
            val matches = StringBuilder()
            // Iterates over all matches.
            for (match in phoneUtil.findNumbers(sub, defaultCountry, leniency, Long.MAX_VALUE)) {
                matches.append(", ").append(match.toString())
            }
        }
    }

    private fun findNumbersForLeniency(
        text: String, defaultCountry: String, leniency: Leniency
    ): Iterator<PhoneNumberMatch> {
        return phoneUtil.findNumbers(text, defaultCountry, leniency, Long.MAX_VALUE).iterator()
    }

    private fun hasNoMatches(iterable: Iterable<PhoneNumberMatch>): Boolean {
        return !iterable.iterator().hasNext()
    }

    /**
     * Small class that holds the context of the number we are testing against. The test will
     * insert the phone number to be found between leadingText and trailingText.
     */
    private class NumberContext internal constructor(val leadingText: String, val trailingText: String)

    /**
     * Small class that holds the number we want to test and the region for which it should be valid.
     */
    private class NumberTest internal constructor(val rawString: String, val region: String) {
        override fun toString(): String {
            return "$rawString ($region)"
        }
    }

    companion object {
        /**
         * Strings with number-like things that shouldn't be found under any level.
         */
        private val IMPOSSIBLE_CASES = arrayOf(
            NumberTest("12345", RegionCode.US),
            NumberTest("23456789", RegionCode.US),
            NumberTest("234567890112", RegionCode.US),
            NumberTest("650+253+1234", RegionCode.US),
            NumberTest("3/10/1984", RegionCode.CA),
            NumberTest("03/27/2011", RegionCode.US),
            NumberTest("31/8/2011", RegionCode.US),
            NumberTest("1/12/2011", RegionCode.US),
            NumberTest("10/12/82", RegionCode.DE),
            NumberTest("650x2531234", RegionCode.US),
            NumberTest("2012-01-02 08:00", RegionCode.US),
            NumberTest("2012/01/02 08:00", RegionCode.US),
            NumberTest("20120102 08:00", RegionCode.US),
            NumberTest("2014-04-12 04:04 PM", RegionCode.US),
            NumberTest("2014-04-12 &nbsp;04:04 PM", RegionCode.US),
            NumberTest("2014-04-12 &nbsp;04:04 PM", RegionCode.US),
            NumberTest("2014-04-12  04:04 PM", RegionCode.US)
        )

        /**
         * Strings with number-like things that should only be found under "possible".
         */
        private val POSSIBLE_ONLY_CASES = arrayOf( // US numbers cannot start with 7 in the test metadata to be valid.
            NumberTest(
                "7121115678",
                RegionCode.US
            ),  // 'X' should not be found in numbers at leniencies stricter than POSSIBLE, unless it represents
            // a carrier code or extension.
            NumberTest("1650 x 253 - 1234", RegionCode.US),
            NumberTest("650 x 253 - 1234", RegionCode.US),
            NumberTest("6502531x234", RegionCode.US),
            NumberTest("(20) 3346 1234", RegionCode.GB)
        )

        /**
         * Strings with number-like things that should only be found up to and including the "valid"
         * leniency level.
         */
        private val VALID_CASES = arrayOf(
            NumberTest("65 02 53 00 00", RegionCode.US),
            NumberTest("6502 538365", RegionCode.US),
            NumberTest("650//253-1234", RegionCode.US),  // 2 slashes are illegal at higher levels
            NumberTest("650/253/1234", RegionCode.US),
            NumberTest("9002309. 158", RegionCode.US),
            NumberTest("12 7/8 - 14 12/34 - 5", RegionCode.US),
            NumberTest("12.1 - 23.71 - 23.45", RegionCode.US),
            NumberTest("800 234 1 111x1111", RegionCode.US),
            NumberTest("1979-2011 100", RegionCode.US),
            NumberTest("+494949-4-94", RegionCode.DE),  // National number in wrong format
            NumberTest("\uFF14\uFF11\uFF15\uFF16\uFF16\uFF16\uFF16-\uFF17\uFF17\uFF17", RegionCode.US),
            NumberTest("2012-0102 08", RegionCode.US),  // Very strange formatting.
            NumberTest("2012-01-02 08", RegionCode.US),  // Breakdown assistance number with unexpected formatting.
            NumberTest("1800-1-0-10 22", RegionCode.AU),
            NumberTest("030-3-2 23 12 34", RegionCode.DE),
            NumberTest("03 0 -3 2 23 12 34", RegionCode.DE),
            NumberTest("(0)3 0 -3 2 23 12 34", RegionCode.DE),
            NumberTest(
                "0 3 0 -3 2 23 12 34",
                RegionCode.DE
            ),  // Fits an alternate pattern, but the leading digits don't match.
            NumberTest("+52 332 123 23 23", RegionCode.MX)
        )

        /**
         * Strings with number-like things that should only be found up to and including the
         * "strict_grouping" leniency level.
         */
        private val STRICT_GROUPING_CASES = arrayOf(
            NumberTest("(415) 6667777", RegionCode.US),
            NumberTest(
                "415-6667777",
                RegionCode.US
            ),  // Should be found by strict grouping but not exact grouping, as the last two groups are
            // formatted together as a block.
            NumberTest(
                "0800-2491234",
                RegionCode.DE
            ),  // Doesn't match any formatting in the test file, but almost matches an alternate format (the
            // last two groups have been squashed together here).
            NumberTest("0900-1 123123", RegionCode.DE),
            NumberTest("(0)900-1 123123", RegionCode.DE),
            NumberTest(
                "0 900-1 123123",
                RegionCode.DE
            ),  // NDC also found as part of the country calling code; this shouldn't ruin the grouping
            // expectations.
            NumberTest("+33 3 34 2312", RegionCode.FR)
        )

        /**
         * Strings with number-like things that should be found at all levels.
         */
        private val EXACT_GROUPING_CASES = arrayOf(
            NumberTest("\uFF14\uFF11\uFF15\uFF16\uFF16\uFF16\uFF17\uFF17\uFF17\uFF17", RegionCode.US),
            NumberTest("\uFF14\uFF11\uFF15-\uFF16\uFF16\uFF16-\uFF17\uFF17\uFF17\uFF17", RegionCode.US),
            NumberTest("4156667777", RegionCode.US),
            NumberTest("4156667777 x 123", RegionCode.US),
            NumberTest("415-666-7777", RegionCode.US),
            NumberTest("415/666-7777", RegionCode.US),
            NumberTest("415-666-7777 ext. 503", RegionCode.US),
            NumberTest("1 415 666 7777 x 123", RegionCode.US),
            NumberTest("+1 415-666-7777", RegionCode.US),
            NumberTest("+494949 49", RegionCode.DE),
            NumberTest("+49-49-34", RegionCode.DE),
            NumberTest("+49-4931-49", RegionCode.DE),
            NumberTest("04931-49", RegionCode.DE),  // With National Prefix
            NumberTest("+49-494949", RegionCode.DE),  // One group with country code
            NumberTest("+49-494949 ext. 49", RegionCode.DE),
            NumberTest("+49494949 ext. 49", RegionCode.DE),
            NumberTest("0494949", RegionCode.DE),
            NumberTest("0494949 ext. 49", RegionCode.DE),
            NumberTest("01 (33) 3461 2234", RegionCode.MX),  // Optional NP present
            NumberTest("(33) 3461 2234", RegionCode.MX),  // Optional NP omitted
            NumberTest("1800-10-10 22", RegionCode.AU),  // Breakdown assistance number.
            // Doesn't match any formatting in the test file, but matches an alternate format exactly.
            NumberTest("0900-1 123 123", RegionCode.DE),
            NumberTest("(0)900-1 123 123", RegionCode.DE),
            NumberTest("0 900-1 123 123", RegionCode.DE),
            NumberTest("+33 3 34 23 12", RegionCode.FR)
        )
    }
}
