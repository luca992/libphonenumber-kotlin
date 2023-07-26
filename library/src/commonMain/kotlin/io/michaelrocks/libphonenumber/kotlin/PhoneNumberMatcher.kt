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

import io.michaelrocks.libphonenumber.kotlin.PhoneNumberUtil
import io.michaelrocks.libphonenumber.kotlin.PhoneNumberUtil.Companion.formattingRuleHasFirstGroupOnly
import io.michaelrocks.libphonenumber.kotlin.PhoneNumberUtil.Companion.normalizeDigits
import io.michaelrocks.libphonenumber.kotlin.PhoneNumberUtil.Companion.normalizeDigitsOnly
import io.michaelrocks.libphonenumber.kotlin.PhoneNumberUtil.Leniency
import io.michaelrocks.libphonenumber.kotlin.PhoneNumberUtil.PhoneNumberFormat
import io.michaelrocks.libphonenumber.kotlin.Phonenumber.PhoneNumber
import io.michaelrocks.libphonenumber.kotlin.Phonenumber.PhoneNumber.CountryCodeSource
import io.michaelrocks.libphonenumber.kotlin.internal.RegexCache

/**
 * A stateful class that finds and extracts telephone numbers from [text][CharSequence].
 * Instances can be created using the [factory methods][PhoneNumberUtil.findNumbers] in
 * [PhoneNumberUtil].
 *
 *
 * Vanity numbers (phone numbers using alphabetic digits such as <tt>1-800-SIX-FLAGS</tt> are
 * not found.
 *
 *
 * This class is not thread-safe.
 */
class PhoneNumberMatcher(
    util: PhoneNumberUtil?, text: CharSequence?, country: String, leniency: Leniency?, maxTries: Long
) : MutableIterator<PhoneNumberMatch?> {
    /** The potential states of a PhoneNumberMatcher.  */
    private enum class State {
        NOT_READY, READY, DONE
    }

    /** The phone number utility.  */
    private val phoneUtil: PhoneNumberUtil

    /** The text searched for phone numbers.  */
    private val text: CharSequence

    /**
     * The region (country) to assume for phone numbers without an international prefix, possibly
     * null.
     */
    private val preferredRegion: String

    /** The degree of validation requested.  */
    private val leniency: Leniency

    /** The maximum number of retries after matching an invalid number.  */
    private var maxTries: Long

    /** The iteration tristate.  */
    private var state = State.NOT_READY

    /** The last successful match, null unless in [State.READY].  */
    private var lastMatch: PhoneNumberMatch? = null

    /** The next index to start searching at. Undefined in [State.DONE].  */
    private var searchIndex = 0

    // A cache for frequently used country-specific regular expressions. Set to 32 to cover ~2-3
    // countries being used for the same doc with ~10 patterns for each country. Some pages will have
    // a lot more countries in use, but typically fewer numbers for each so expanding the cache for
    // that use-case won't have a lot of benefit.
    private val regexCache = RegexCache(32)

    /**
     * Creates a new instance. See the factory methods in [PhoneNumberUtil] on how to obtain a
     * new instance.
     *
     * @param util  the phone number util to use
     * @param text  the character sequence that we will search, null for no text
     * @param country  the country to assume for phone numbers not written in international format
     * (with a leading plus, or with the international dialing prefix of the specified region).
     * May be null or "ZZ" if only numbers with a leading plus should be
     * considered.
     * @param leniency  the leniency to use when evaluating candidate phone numbers
     * @param maxTries  the maximum number of invalid numbers to try before giving up on the text.
     * This is to cover degenerate cases where the text has a lot of false positives in it. Must
     * be `>= 0`.
     */
    init {
        if (util == null || leniency == null) {
            throw NullPointerException()
        }
        require(maxTries >= 0)
        phoneUtil = util
        this.text = text ?: ""
        preferredRegion = country
        this.leniency = leniency
        this.maxTries = maxTries
    }

    /**
     * Attempts to find the next subsequence in the searched sequence on or after `searchIndex`
     * that represents a phone number. Returns the next match, null if none was found.
     *
     * @param index  the search index to start searching at
     * @return  the phone number match found, null if none can be found
     */
    private fun find(index: Int): PhoneNumberMatch? {
        var index = index

        while (maxTries > 0) {
            val matcherResult = REGEX!!.find(text, index) ?: break
            val start = matcherResult.range.first
            var candidate = text.subSequence(start, matcherResult.range.last)

            // Check for extra numbers at the end.
            // TODO: This is the place to start when trying to support extraction of multiple phone number
            // from split notations (+41 79 123 45 67 / 68).
            candidate = trimAfterFirstMatch(PhoneNumberUtil.SECOND_NUMBER_START_PATTERN, candidate)
            val match = extractMatch(candidate, start)
            if (match != null) {
                return match
            }
            index = start + candidate.length
            maxTries--
        }
        return null
    }

    /**
     * Attempts to extract a match from a `candidate` character sequence.
     *
     * @param candidate  the candidate text that might contain a phone number
     * @param offset  the offset of `candidate` within [.text]
     * @return  the match found, null if none can be found
     */
    private fun extractMatch(candidate: CharSequence, offset: Int): PhoneNumberMatch? {
        // Skip a match that is more likely to be a date.
        if (SLASH_SEPARATED_DATES.find(candidate) == null) {
            return null
        }

        // Skip potential time-stamps.
        if (TIME_STAMPS.find(candidate) == null) {
            val followingText = text.toString().substring(offset + candidate.length)
            if (TIME_STAMPS_SUFFIX.matchesAt(followingText, 0)) {
                return null
            }
        }

        // Try to come up with a valid match given the entire candidate.
        val match = parseAndVerify(candidate, offset)
        return match ?: extractInnerMatch(candidate, offset)

        // If that failed, try to find an "inner match" - there might be a phone number within this
        // candidate.
    }

    /**
     * Attempts to extract a match from `candidate` if the whole candidate does not qualify as a
     * match.
     *
     * @param candidate  the candidate text that might contain a phone number
     * @param offset  the current offset of `candidate` within [.text]
     * @return  the match found, null if none can be found
     */
    private fun extractInnerMatch(candidate: CharSequence, offset: Int): PhoneNumberMatch? {
        for (possibleInnerMatch in INNER_MATCHES) {
            var isFirstMatch = true
            while (maxTries > 0) {
                var groupMatcherResult: MatchResult? = null

                if (isFirstMatch) {
                    groupMatcherResult = possibleInnerMatch.find(candidate) ?: break
                    // We should handle any group before this one too.
                    val group = trimAfterFirstMatch(
                        PhoneNumberUtil.UNWANTED_END_CHAR_PATTERN,
                        candidate.subSequence(0, groupMatcherResult.range.first)
                    )
                    val match = parseAndVerify(group, offset)
                    if (match != null) {
                        return match
                    }
                    maxTries--
                    isFirstMatch = false
                } else {
                    groupMatcherResult = groupMatcherResult!!.next() ?: break
                }
                val group = trimAfterFirstMatch(
                    PhoneNumberUtil.UNWANTED_END_CHAR_PATTERN, groupMatcherResult.groupValues[1]
                )
                val match = parseAndVerify(group, offset + groupMatcherResult.range.first)
                if (match != null) {
                    return match
                }
                maxTries--
            }
        }
        return null
    }

    /**
     * Parses a phone number from the `candidate` using [PhoneNumberUtil.parse] and
     * verifies it matches the requested [.leniency]. If parsing and verification succeed, a
     * corresponding [PhoneNumberMatch] is returned, otherwise this method returns null.
     *
     * @param candidate  the candidate match
     * @param offset  the offset of `candidate` within [.text]
     * @return  the parsed and validated phone number match, or null
     */
    private fun parseAndVerify(candidate: CharSequence, offset: Int): PhoneNumberMatch? {
        try {
            // Check the candidate doesn't contain any formatting which would indicate that it really
            // isn't a phone number.
            if (!MATCHING_BRACKETS!!.matches(candidate) || PUB_PAGES.find(candidate) == null) {
                return null
            }

            // If leniency is set to VALID or stricter, we also want to skip numbers that are surrounded
            // by Latin alphabetic characters, to skip cases like abc8005001234 or 8005001234def.
            if (leniency.compareTo(Leniency.VALID) >= 0) {
                // If the candidate is not at the start of the text, and does not start with phone-number
                // punctuation, check the previous character.
                if (offset > 0 && LEAD_CLASS!!.find(candidate, 0) == null) {
                    val previousChar = text[offset - 1]
                    // We return null if it is a latin letter or an invalid punctuation symbol.
                    if (isInvalidPunctuationSymbol(previousChar) || isLatinLetter(previousChar)) {
                        return null
                    }
                }
                val lastCharIndex = offset + candidate.length
                if (lastCharIndex < text.length) {
                    val nextChar = text[lastCharIndex]
                    if (isInvalidPunctuationSymbol(nextChar) || isLatinLetter(nextChar)) {
                        return null
                    }
                }
            }
            val number = phoneUtil.parseAndKeepRawInput(candidate, preferredRegion)
            if (leniency.verify(number, candidate, phoneUtil, this)) {
                // We used parseAndKeepRawInput to create this number, but for now we don't return the extra
                // values parsed. TODO: stop clearing all values here and switch all users over
                // to using rawInput() rather than the rawString() of PhoneNumberMatch.
                number.clearCountryCodeSource()
                number.clearRawInput()
                number.clearPreferredDomesticCarrierCode()
                return PhoneNumberMatch(offset, candidate.toString(), number)
            }
        } catch (e: NumberParseException) {
            // ignore and continue
        }
        return null
    }

    /**
     * Small helper interface such that the number groups can be checked according to different
     * criteria, both for our default way of performing formatting and for any alternate formats we
     * may want to check.
     */
    interface NumberGroupingChecker {
        /**
         * Returns true if the groups of digits found in our candidate phone number match our
         * expectations.
         *
         * @param number  the original number we found when parsing
         * @param normalizedCandidate  the candidate number, normalized to only contain ASCII digits,
         * but with non-digits (spaces etc) retained
         * @param expectedNumberGroups  the groups of digits that we would expect to see if we
         * formatted this number
         */
        fun checkGroups(
            util: PhoneNumberUtil,
            number: PhoneNumber,
            normalizedCandidate: StringBuilder,
            expectedNumberGroups: Array<String>
        ): Boolean
    }

    fun checkNumberGroupingIsValid(
        number: PhoneNumber, candidate: CharSequence?, util: PhoneNumberUtil, checker: NumberGroupingChecker
    ): Boolean {
        val normalizedCandidate = normalizeDigits(candidate!!, true /* keep non-digits */)
        var formattedNumberGroups = getNationalNumberGroups(util, number)
        if (checker.checkGroups(util, number, normalizedCandidate, formattedNumberGroups)) {
            return true
        }
        // If this didn't pass, see if there are any alternate formats that match, and try them instead.
        val alternateFormats =
            phoneUtil.metadataDependenciesProvider.alternateFormatsMetadataSource.getFormattingMetadataForCountryCallingCode(
                number.countryCode
            )
        val nationalSignificantNumber = util.getNationalSignificantNumber(number)
        if (alternateFormats != null) {
            for (alternateFormat in alternateFormats.numberFormatList) {
                if (alternateFormat.leadingDigitsPatternCount > 0) {
                    // There is only one leading digits pattern for alternate formats.
                    val regex = regexCache.getRegexForPattern(alternateFormat.getLeadingDigitsPattern(0))
                    if (!regex.matchesAt(nationalSignificantNumber, 0)) {
                        // Leading digits don't match; try another one.
                        continue
                    }
                }
                formattedNumberGroups = getNationalNumberGroups(util, number, alternateFormat)
                if (checker.checkGroups(util, number, normalizedCandidate, formattedNumberGroups)) {
                    return true
                }
            }
        }
        return false
    }

    override fun hasNext(): Boolean {
        if (state == State.NOT_READY) {
            lastMatch = find(searchIndex)
            if (lastMatch == null) {
                state = State.DONE
            } else {
                searchIndex = lastMatch!!.end()
                state = State.READY
            }
        }
        return state == State.READY
    }

    override fun next(): PhoneNumberMatch? {
        // Check the state and find the next match as a side-effect if necessary.
        if (!hasNext()) {
            throw NoSuchElementException()
        }

        // Don't retain that memory any longer than necessary.
        val result = lastMatch
        lastMatch = null
        state = State.NOT_READY
        return result
    }

    /**
     * Always throws [UnsupportedOperationException] as removal is not supported.
     */
    override fun remove() {
        throw UnsupportedOperationException()
    }

    companion object {
        /**
         * The phone number pattern used by [.find], similar to
         * `PhoneNumberUtil.VALID_PHONE_NUMBER`, but with the following differences:
         *
         *  * All captures are limited in order to place an upper bound to the text matched by the
         * pattern.
         *
         *  * Leading punctuation / plus signs are limited.
         *  * Consecutive occurrences of punctuation are limited.
         *  * Number of digits is limited.
         *
         *  * No whitespace is allowed at the start or end.
         *  * No alpha digits (vanity numbers such as 1-800-SIX-FLAGS) are currently supported.
         *
         */
        private var REGEX: Regex? = null

        /**
         * Matches strings that look like publication pages. Example:
         * <pre>Computing Complete Answers to Queries in the Presence of Limited Access Patterns.
         * Chen Li. VLDB J. 12(3): 211-227 (2003).</pre>
         *
         * The string "211-227 (2003)" is not a telephone number.
         */
        private val PUB_PAGES = Regex("\\d{1,5}-+\\d{1,5}\\s{0,4}\\(\\d{1,4}")

        /**
         * Matches strings that look like dates using "/" as a separator. Examples: 3/10/2011, 31/10/96 or
         * 08/31/95.
         */
        private val SLASH_SEPARATED_DATES =
            Regex("(?:(?:[0-3]?\\d/[01]?\\d)|(?:[01]?\\d/[0-3]?\\d))/(?:[12]\\d)?\\d{2}")

        /**
         * Matches timestamps. Examples: "2012-01-02 08:00". Note that the reg-ex does not include the
         * trailing ":\d\d" -- that is covered by TIME_STAMPS_SUFFIX.
         */
        private val TIME_STAMPS = Regex("[12]\\d{3}[-/]?[01]\\d[-/]?[0-3]\\d +[0-2]\\d$")
        private val TIME_STAMPS_SUFFIX = Regex(":[0-5]\\d")

        /**
         * Pattern to check that brackets match. Opening brackets should be closed within a phone number.
         * This also checks that there is something inside the brackets. Having no brackets at all is also
         * fine.
         */
        private var MATCHING_BRACKETS: Regex? = null

        /**
         * Patterns used to extract phone numbers from a larger phone-number-like pattern. These are
         * ordered according to specificity. For example, white-space is last since that is frequently
         * used in numbers, not just to separate two numbers. We have separate patterns since we don't
         * want to break up the phone-number-like text on more than one different kind of symbol at one
         * time, although symbols of the same type (e.g. space) can be safely grouped together.
         *
         * Note that if there is a match, we will always check any text found up to the first match as
         * well.
         */
        private val INNER_MATCHES = arrayOf( // Breaks on the slash - e.g. "651-234-2345/332-445-1234"
            Regex("/+(.*)"),  // Note that the bracket here is inside the capturing group, since we consider it part of the
            // phone number. Will match a pattern like "(650) 223 3345 (754) 223 3321".
            Regex("(\\([^(]*)"),  // Breaks on a hyphen - e.g. "12345 - 332-445-1234 is my number."
            // We require a space on either side of the hyphen for it to be considered a separator.
            Regex("(?:\\p{Z}-|-\\p{Z})\\p{Z}*(.+)"),  // Various types of wide hyphens. Note we have decided not to enforce a space here, since it's
            // possible that it's supposed to be used to break two numbers without spaces, and we haven't
            // seen many instances of it used within a number.
            Regex("[\u2012-\u2015\uFF0D]\\p{Z}*(.+)"),  // Breaks on a full stop - e.g. "12345. 332-445-1234 is my number."
            Regex("\\.+\\p{Z}*([^.]+)"),  // Breaks on space - e.g. "3324451234 8002341234"
            Regex("\\p{Z}+(\\P{Z}+)")
        )

        /**
         * Punctuation that may be at the start of a phone number - brackets and plus signs.
         */
        private var LEAD_CLASS: Regex? = null

        init {/* Builds the MATCHING_BRACKETS and PATTERN regular expressions. The building blocks below exist
     * to make the pattern more easily understood. */
            val openingParens = "(\\[\uFF08\uFF3B"
            val closingParens = ")\\]\uFF09\uFF3D"
            val nonParens = "[^$openingParens$closingParens]"

            /* Limit on the number of pairs of brackets in a phone number. */
            val bracketPairLimit = limit(0, 3)/*
     * An opening bracket at the beginning may not be closed, but subsequent ones should be.  It's
     * also possible that the leading bracket was dropped, so we shouldn't be surprised if we see a
     * closing bracket first. We limit the sets of brackets in a phone number to four.
     */
            MATCHING_BRACKETS = Regex(
                "(?:[" + openingParens + "])?" + "(?:" + nonParens + "+" + "[" + closingParens + "])?" + nonParens + "+" + "(?:[" + openingParens + "]" + nonParens + "+[" + closingParens + "])" + bracketPairLimit + nonParens + "*"
            )

            /* Limit on the number of leading (plus) characters. */
            val leadLimit = limit(0, 2)/* Limit on the number of consecutive punctuation characters. */
            val punctuationLimit = limit(0, 4)/* The maximum number of digits allowed in a digit-separated block. As we allow all digits in a
     * single block, set high enough to accommodate the entire national number and the international
     * country code. */
            val digitBlockLimit = PhoneNumberUtil.MAX_LENGTH_FOR_NSN + PhoneNumberUtil.MAX_LENGTH_COUNTRY_CODE/* Limit on the number of blocks separated by punctuation. Uses digitBlockLimit since some
     * formats use spaces to separate each digit. */
            val blockLimit = limit(0, digitBlockLimit)

            /* A punctuation sequence allowing white space. */
            val punctuation =
                "[" + PhoneNumberUtil.VALID_PUNCTUATION + "]" + punctuationLimit/* A digits block without punctuation. */
            val digitSequence = "\\p{Nd}" + limit(1, digitBlockLimit)
            val leadClassChars = openingParens + PhoneNumberUtil.PLUS_CHARS
            val leadClass = "[$leadClassChars]"
            LEAD_CLASS = Regex(leadClass)

            /* Phone number pattern allowing optional punctuation. */REGEX = Regex(
                "(?:" + leadClass + punctuation + ")" + leadLimit + digitSequence + "(?:" + punctuation + digitSequence + ")" + blockLimit + "(?:" + PhoneNumberUtil.EXTN_PATTERNS_FOR_MATCHING + ")?",
                PhoneNumberUtil.REGEX_FLAGS
            )
        }

        /** Returns a regular expression quantifier with an upper and lower limit.  */
        private fun limit(lower: Int, upper: Int): String {
            require(!(lower < 0 || upper <= 0 || upper < lower))
            return "{$lower,$upper}"
        }

        /**
         * Trims away any characters after the first match of `pattern` in `candidate`,
         * returning the trimmed version.
         */
        private fun trimAfterFirstMatch(regex: Regex, candidate: CharSequence): CharSequence {
            val trailingCharsMatcher = regex.find(candidate)
            return if (trailingCharsMatcher != null) {
                candidate.subSequence(0, trailingCharsMatcher.range.first)
            } else candidate
        }

        /**
         * Helper method to determine if a character is a Latin-script letter or not. For our purposes,
         * combining marks should also return true since we assume they have been added to a preceding
         * Latin character.
         */
        // @VisibleForTesting
        fun isLatinLetter(letter: Char): Boolean {
            val charValue = letter.code
            return ('A'.code <= charValue && charValue <= 'Z'.code) || ('a'.code <= charValue && charValue <= 'z'.code)
        }

        private fun isInvalidPunctuationSymbol(character: Char): Boolean {
            return character == '%' || isCurrencySymbol(character)
        }

        private fun isCurrencySymbol(character: Char): Boolean {
            return (character.code in 36..63) || (character.code in 1536..1541) || (character.code in 8352..8361) || (character.code in 65020..65023)
        }

        fun allNumberGroupsRemainGrouped(
            util: PhoneNumberUtil,
            number: PhoneNumber,
            normalizedCandidate: StringBuilder,
            formattedNumberGroups: Array<String>
        ): Boolean {
            var fromIndex = 0
            if (number.countryCodeSource !== CountryCodeSource.FROM_DEFAULT_COUNTRY) {
                // First skip the country code if the normalized candidate contained it.
                val countryCode = number.countryCode.toString()
                fromIndex = normalizedCandidate.indexOf(countryCode) + countryCode.length
            }
            // Check each group of consecutive digits are not broken into separate groupings in the
            // {@code normalizedCandidate} string.
            for (i in formattedNumberGroups.indices) {
                // Fails if the substring of {@code normalizedCandidate} starting from {@code fromIndex}
                // doesn't contain the consecutive digits in formattedNumberGroups[i].
                fromIndex = normalizedCandidate.indexOf(formattedNumberGroups[i], fromIndex)
                if (fromIndex < 0) {
                    return false
                }
                // Moves {@code fromIndex} forward.
                fromIndex += formattedNumberGroups[i].length
                if (i == 0 && fromIndex < normalizedCandidate.length) {
                    // We are at the position right after the NDC. We get the region used for formatting
                    // information based on the country code in the phone number, rather than the number itself,
                    // as we do not need to distinguish between different countries with the same country
                    // calling code and this is faster.
                    val region = util.getRegionCodeForCountryCode(number.countryCode)
                    if (util.getNddPrefixForRegion(region, true) != null && normalizedCandidate[fromIndex].isDigit()) {
                        // This means there is no formatting symbol after the NDC. In this case, we only
                        // accept the number if there is no formatting symbol at all in the number, except
                        // for extensions. This is only important for countries with national prefixes.
                        val nationalSignificantNumber = util.getNationalSignificantNumber(number)
                        return normalizedCandidate.substring(fromIndex - formattedNumberGroups[i].length)
                            .startsWith(nationalSignificantNumber)
                    }
                }
            }
            // The check here makes sure that we haven't mistakenly already used the extension to
            // match the last group of the subscriber number. Note the extension cannot have
            // formatting in-between digits.
            return normalizedCandidate.substring(fromIndex).contains(number.extension)
        }

        fun allNumberGroupsAreExactlyPresent(
            util: PhoneNumberUtil,
            number: PhoneNumber,
            normalizedCandidate: StringBuilder,
            formattedNumberGroups: Array<String>
        ): Boolean {
            val candidateGroups = PhoneNumberUtil.NON_DIGITS_PATTERN.split(normalizedCandidate.toString())
            // Set this to the last group, skipping it if the number has an extension.
            var candidateNumberGroupIndex =
                if (number.hasExtension()) candidateGroups.size - 2 else candidateGroups.size - 1
            // First we check if the national significant number is formatted as a block.
            // We use contains and not equals, since the national significant number may be present with
            // a prefix such as a national number prefix, or the country code itself.
            if (candidateGroups.size == 1 || candidateGroups[candidateNumberGroupIndex].contains(
                    util.getNationalSignificantNumber(number)
                )
            ) {
                return true
            }
            // Starting from the end, go through in reverse, excluding the first group, and check the
            // candidate and number groups are the same.
            var formattedNumberGroupIndex = formattedNumberGroups.size - 1
            while (formattedNumberGroupIndex > 0 && candidateNumberGroupIndex >= 0) {
                if (candidateGroups[candidateNumberGroupIndex] != formattedNumberGroups[formattedNumberGroupIndex]) {
                    return false
                }
                formattedNumberGroupIndex--
                candidateNumberGroupIndex--
            }
            // Now check the first group. There may be a national prefix at the start, so we only check
            // that the candidate group ends with the formatted number group.
            return (candidateNumberGroupIndex >= 0 && candidateGroups[candidateNumberGroupIndex].endsWith(
                formattedNumberGroups[0]
            ))
        }

        /**
         * Helper method to get the national-number part of a number, formatted without any national
         * prefix, and return it as a set of digit blocks that would be formatted together following
         * standard formatting rules.
         */
        private fun getNationalNumberGroups(util: PhoneNumberUtil, number: PhoneNumber): Array<String> {
            // This will be in the format +CC-DG1-DG2-DGX;ext=EXT where DG1..DGX represents groups of
            // digits.
            val rfc3966Format = util.format(number, PhoneNumberFormat.RFC3966)
            // We remove the extension part from the formatted string before splitting it into different
            // groups.
            var endIndex = rfc3966Format.indexOf(';')
            if (endIndex < 0) {
                endIndex = rfc3966Format.length
            }
            // The country-code will have a '-' following it.
            val startIndex = rfc3966Format.indexOf('-') + 1
            return rfc3966Format.substring(startIndex, endIndex).split("-".toRegex()).dropLastWhile { it.isEmpty() }
                .toTypedArray()
        }

        /**
         * Helper method to get the national-number part of a number, formatted without any national
         * prefix, and return it as a set of digit blocks that should be formatted together according to
         * the formatting pattern passed in.
         */
        private fun getNationalNumberGroups(
            util: PhoneNumberUtil, number: PhoneNumber, formattingPattern: Phonemetadata.NumberFormat
        ): Array<String> {
            // If a format is provided, we format the NSN only, and split that according to the separator.
            val nationalSignificantNumber = util.getNationalSignificantNumber(number)
            return util.formatNsnUsingPattern(
                nationalSignificantNumber, formattingPattern, PhoneNumberFormat.RFC3966
            ).split("-".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        }

        fun containsMoreThanOneSlashInNationalNumber(number: PhoneNumber, candidate: String): Boolean {
            val firstSlashInBodyIndex = candidate.indexOf('/')
            if (firstSlashInBodyIndex < 0) {
                // No slashes, this is okay.
                return false
            }
            // Now look for a second one.
            val secondSlashInBodyIndex = candidate.indexOf('/', firstSlashInBodyIndex + 1)
            if (secondSlashInBodyIndex < 0) {
                // Only one slash, this is okay.
                return false
            }

            // If the first slash is after the country calling code, this is permitted.
            val candidateHasCountryCode =
                (number.countryCodeSource === CountryCodeSource.FROM_NUMBER_WITH_PLUS_SIGN || number.countryCodeSource === CountryCodeSource.FROM_NUMBER_WITHOUT_PLUS_SIGN)
            return if (candidateHasCountryCode && (normalizeDigitsOnly(
                    candidate.substring(
                        0, firstSlashInBodyIndex
                    )
                ) == number.countryCode.toString())
            ) {
                // Any more slashes and this is illegal.
                candidate.substring(secondSlashInBodyIndex + 1).contains("/")
            } else true
        }

        fun containsOnlyValidXChars(
            number: PhoneNumber, candidate: String, util: PhoneNumberUtil
        ): Boolean {
            // The characters 'x' and 'X' can be (1) a carrier code, in which case they always precede the
            // national significant number or (2) an extension sign, in which case they always precede the
            // extension number. We assume a carrier code is more than 1 digit, so the first case has to
            // have more than 1 consecutive 'x' or 'X', whereas the second case can only have exactly 1 'x'
            // or 'X'. We ignore the character if it appears as the last character of the string.
            var index = 0
            while (index < candidate.length - 1) {
                val charAtIndex = candidate[index]
                if (charAtIndex == 'x' || charAtIndex == 'X') {
                    val charAtNextIndex = candidate[index + 1]
                    if (charAtNextIndex == 'x' || charAtNextIndex == 'X') {
                        // This is the carrier code case, in which the 'X's always precede the national
                        // significant number.
                        index++
                        if (util.isNumberMatch(
                                number, candidate.substring(index)
                            ) !== PhoneNumberUtil.MatchType.NSN_MATCH
                        ) {
                            return false
                        }
                        // This is the extension sign case, in which the 'x' or 'X' should always precede the
                        // extension number.
                    } else if (normalizeDigitsOnly(candidate.substring(index)) != number.extension) {
                        return false
                    }
                }
                index++
            }
            return true
        }

        fun isNationalPrefixPresentIfRequired(number: PhoneNumber, util: PhoneNumberUtil): Boolean {
            // First, check how we deduced the country code. If it was written in international format, then
            // the national prefix is not required.
            if (number.countryCodeSource !== CountryCodeSource.FROM_DEFAULT_COUNTRY) {
                return true
            }
            val phoneNumberRegion = util.getRegionCodeForCountryCode(number.countryCode)
            val metadata = util.getMetadataForRegion(phoneNumberRegion) ?: return true
            // Check if a national prefix should be present when formatting this number.
            val nationalNumber = util.getNationalSignificantNumber(number)
            val formatRule = util.chooseFormattingPatternForNumber(metadata.numberFormatList, nationalNumber)
            // To do this, we check that a national prefix formatting rule was present and that it wasn't
            // just the first-group symbol ($1) with punctuation.
            if (formatRule != null && formatRule.nationalPrefixFormattingRule.length > 0) {
                if (formatRule.nationalPrefixOptionalWhenFormatting) {
                    // The national-prefix is optional in these cases, so we don't need to check if it was
                    // present.
                    return true
                }
                if (formattingRuleHasFirstGroupOnly(
                        formatRule.nationalPrefixFormattingRule
                    )
                ) {
                    // National Prefix not needed for this number.
                    return true
                }
                // Normalize the remainder.
                val rawInputCopy = normalizeDigitsOnly(number.rawInput)
                val rawInput = StringBuilder(rawInputCopy)
                // Check if we found a national prefix and/or carrier code at the start of the raw input, and
                // return the result.
                return util.maybeStripNationalPrefixAndCarrierCode(rawInput, metadata, null)
            }
            return true
        }
    }
}
