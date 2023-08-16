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
import io.michaelrocks.libphonenumber.android.AsYouTypeFormatter
import io.michaelrocks.libphonenumber.kotlin.*
import io.michaelrocks.libphonenumber.kotlin.CountryCodeToRegionCodeMap.countryCodeToRegionCodeMap
import io.michaelrocks.libphonenumber.kotlin.PhoneNumberMatcher.NumberGroupingChecker
import io.michaelrocks.libphonenumber.kotlin.Phonemetadata.NumberFormat.Companion.newBuilder
import io.michaelrocks.libphonenumber.kotlin.Phonemetadata.PhoneMetadata
import io.michaelrocks.libphonenumber.kotlin.Phonemetadata.PhoneNumberDesc
import io.michaelrocks.libphonenumber.kotlin.Phonenumber.PhoneNumber
import io.michaelrocks.libphonenumber.kotlin.Phonenumber.PhoneNumber.CountryCodeSource
import io.michaelrocks.libphonenumber.kotlin.internal.RegexBasedMatcher.Companion.create
import io.michaelrocks.libphonenumber.kotlin.internal.RegexCache
import io.michaelrocks.libphonenumber.kotlin.metadata.DefaultMetadataDependenciesProvider
import io.michaelrocks.libphonenumber.kotlin.metadata.source.MetadataSource
import io.michaelrocks.libphonenumber.kotlin.metadata.source.MetadataSourceImpl
import io.michaelrocks.libphonenumber.kotlin.util.InplaceStringBuilder
import kotlin.jvm.JvmField
import kotlin.jvm.JvmOverloads
import kotlin.jvm.JvmStatic
import kotlin.jvm.Volatile

/**
 * Utility for international phone numbers. Functionality includes formatting, parsing and
 * validation.
 *
 *
 * If you use this library, and want to be notified about important changes, please sign up to
 * our [mailing list](https://groups.google.com/forum/#!aboutgroup/libphonenumber-discuss).
 *
 * NOTE: A lot of methods in this class require Region Code strings. These must be provided using
 * CLDR two-letter region-code format. These should be in upper-case. The list of the codes
 * can be found here:
 * http://www.unicode.org/cldr/charts/30/supplemental/territory_information.html
 */
class PhoneNumberUtil internal constructor(// A source of metadata for different regions.
    val metadataSource: MetadataSource,
    // A provider of metadata dependencies.
    @JvmField val metadataDependenciesProvider: DefaultMetadataDependenciesProvider,
    // A mapping from a country calling code to the region codes which denote the region represented
    // by that country calling code. In the case of multiple regions sharing a calling code, such as
    // the NANPA regions, the one indicated with "isMainCountryForCode" in the metadata should be
    // first.
    private val countryCallingCodeToRegionCodeMap: Map<Int, List<String>>
) {
    /**
     * INTERNATIONAL and NATIONAL formats are consistent with the definition in ITU-T Recommendation
     * E.123. However we follow local conventions such as using '-' instead of whitespace as
     * separators. For example, the number of the Google Switzerland office will be written as
     * "+41 44 668 1800" in INTERNATIONAL format, and as "044 668 1800" in NATIONAL format. E164
     * format is as per INTERNATIONAL format but with no formatting applied, e.g. "+41446681800".
     * RFC3966 is as per INTERNATIONAL format, but with all spaces and other separating symbols
     * replaced with a hyphen, and with any phone number extension appended with ";ext=". It also
     * will have a prefix of "tel:" added, e.g. "tel:+41-44-668-1800".
     *
     * Note: If you are considering storing the number in a neutral format, you are highly advised to
     * use the PhoneNumber class.
     */
    enum class PhoneNumberFormat {
        E164, INTERNATIONAL, NATIONAL, RFC3966
    }

    /**
     * Type of phone numbers.
     */
    enum class PhoneNumberType {
        FIXED_LINE, MOBILE,

        // In some regions (e.g. the USA), it is impossible to distinguish between fixed-line and
        // mobile numbers by looking at the phone number itself.
        FIXED_LINE_OR_MOBILE,

        // Freephone lines
        TOLL_FREE, PREMIUM_RATE,

        // The cost of this call is shared between the caller and the recipient, and is hence typically
        // less than PREMIUM_RATE calls. See // http://en.wikipedia.org/wiki/Shared_Cost_Service for
        // more information.
        SHARED_COST,

        // Voice over IP numbers. This includes TSoIP (Telephony Service over IP).
        VOIP,

        // A personal number is associated with a particular person, and may be routed to either a
        // MOBILE or FIXED_LINE number. Some more information can be found here:
        // http://en.wikipedia.org/wiki/Personal_Numbers
        PERSONAL_NUMBER, PAGER,

        // Used for "Universal Access Numbers" or "Company Numbers". They may be further routed to
        // specific offices, but allow one number to be used for a company.
        UAN,

        // Used for "Voice Mail Access Numbers".
        VOICEMAIL,

        // A phone number is of type UNKNOWN when it does not fit any of the known patterns for a
        // specific region.
        UNKNOWN
    }

    /**
     * Types of phone number matches. See detailed description beside the isNumberMatch() method.
     */
    enum class MatchType {
        NOT_A_NUMBER, NO_MATCH, SHORT_NSN_MATCH, NSN_MATCH, EXACT_MATCH
    }

    /**
     * Possible outcomes when testing if a PhoneNumber is possible.
     */
    enum class ValidationResult {
        /** The number length matches that of valid numbers for this region.  */
        IS_POSSIBLE,

        /**
         * The number length matches that of local numbers for this region only (i.e. numbers that may
         * be able to be dialled within an area, but do not have all the information to be dialled from
         * anywhere inside or outside the country).
         */
        IS_POSSIBLE_LOCAL_ONLY,

        /** The number has an invalid country calling code.  */
        INVALID_COUNTRY_CODE,

        /** The number is shorter than all valid numbers for this region.  */
        TOO_SHORT,

        /**
         * The number is longer than the shortest valid numbers for this region, shorter than the
         * longest valid numbers for this region, and does not itself have a number length that matches
         * valid numbers for this region. This can also be returned in the case where
         * isPossibleNumberForTypeWithReason was called, and there are no numbers of this type at all
         * for this region.
         */
        INVALID_LENGTH,

        /** The number is longer than all valid numbers for this region.  */
        TOO_LONG
    }

    /**
     * Leniency when [finding][PhoneNumberUtil.findNumbers] potential phone numbers in text
     * segments. The levels here are ordered in increasing strictness.
     */
    enum class Leniency {
        /**
         * Phone numbers accepted are [ possible][PhoneNumberUtil.isPossibleNumber], but not necessarily [valid][PhoneNumberUtil.isValidNumber].
         */
        POSSIBLE {
            override fun verify(
                number: PhoneNumber, candidate: CharSequence, util: PhoneNumberUtil, matcher: PhoneNumberMatcher
            ): Boolean {
                return util.isPossibleNumber(number)
            }
        },

        /**
         * Phone numbers accepted are [ possible][PhoneNumberUtil.isPossibleNumber] and [valid][PhoneNumberUtil.isValidNumber]. Numbers written
         * in national format must have their national-prefix present if it is usually written for a
         * number of this type.
         */
        VALID {
            override fun verify(
                number: PhoneNumber, candidate: CharSequence, util: PhoneNumberUtil, matcher: PhoneNumberMatcher
            ): Boolean {
                return if ((!util.isValidNumber(number) || !PhoneNumberMatcher.containsOnlyValidXChars(
                        number, candidate.toString(), util
                    ))
                ) {
                    false
                } else PhoneNumberMatcher.isNationalPrefixPresentIfRequired(number, util)
            }
        },

        /**
         * Phone numbers accepted are [valid][PhoneNumberUtil.isValidNumber] and
         * are grouped in a possible way for this locale. For example, a US number written as
         * "65 02 53 00 00" and "650253 0000" are not accepted at this leniency level, whereas
         * "650 253 0000", "650 2530000" or "6502530000" are.
         * Numbers with more than one '/' symbol in the national significant number are also dropped at
         * this level.
         *
         *
         * Warning: This level might result in lower coverage especially for regions outside of country
         * code "+1". If you are not sure about which level to use, email the discussion group
         * libphonenumber-discuss@googlegroups.com.
         */
        STRICT_GROUPING {
            override fun verify(
                number: PhoneNumber, candidate: CharSequence, util: PhoneNumberUtil, matcher: PhoneNumberMatcher
            ): Boolean {
                val candidateString = candidate.toString()
                return if ((!util.isValidNumber(number) || !PhoneNumberMatcher.containsOnlyValidXChars(
                        number, candidateString, util
                    ) || PhoneNumberMatcher.containsMoreThanOneSlashInNationalNumber(
                        number, candidateString
                    ) || !PhoneNumberMatcher.isNationalPrefixPresentIfRequired(number, util))
                ) {
                    false
                } else {
                    matcher.checkNumberGroupingIsValid(number,
                        candidate,
                        util,
                        object : NumberGroupingChecker {
                            override fun checkGroups(
                                util: PhoneNumberUtil,
                                number: PhoneNumber,
                                normalizedCandidate: InplaceStringBuilder,
                                expectedNumberGroups: Array<String>
                            ): Boolean {
                                return PhoneNumberMatcher.allNumberGroupsRemainGrouped(
                                    util, number, normalizedCandidate, expectedNumberGroups
                                )
                            }
                        })
                }
            }
        },

        /**
         * Phone numbers accepted are [valid][PhoneNumberUtil.isValidNumber] and
         * are grouped in the same way that we would have formatted it, or as a single block. For
         * example, a US number written as "650 2530000" is not accepted at this leniency level, whereas
         * "650 253 0000" or "6502530000" are.
         * Numbers with more than one '/' symbol are also dropped at this level.
         *
         *
         * Warning: This level might result in lower coverage especially for regions outside of country
         * code "+1". If you are not sure about which level to use, email the discussion group
         * libphonenumber-discuss@googlegroups.com.
         */
        EXACT_GROUPING {
            override fun verify(
                number: PhoneNumber, candidate: CharSequence, util: PhoneNumberUtil, matcher: PhoneNumberMatcher
            ): Boolean {
                val candidateString = candidate.toString()
                return if ((!util.isValidNumber(number) || !PhoneNumberMatcher.containsOnlyValidXChars(
                        number, candidateString, util
                    ) || PhoneNumberMatcher.containsMoreThanOneSlashInNationalNumber(
                        number, candidateString
                    ) || !PhoneNumberMatcher.isNationalPrefixPresentIfRequired(number, util))
                ) {
                    false
                } else matcher.checkNumberGroupingIsValid(number, candidate, util, object : NumberGroupingChecker {
                    override fun checkGroups(
                        util: PhoneNumberUtil,
                        number: PhoneNumber,
                        normalizedCandidate: InplaceStringBuilder,
                        expectedNumberGroups: Array<String>
                    ): Boolean {
                        return PhoneNumberMatcher.allNumberGroupsAreExactlyPresent(
                            util, number, normalizedCandidate, expectedNumberGroups
                        )
                    }
                })
            }
        };

        /** Returns true if `number` is a verified number according to this leniency.  */
        abstract fun verify(
            number: PhoneNumber, candidate: CharSequence, util: PhoneNumberUtil, matcher: PhoneNumberMatcher
        ): Boolean
    }

    // A helper class for getting information about short phone numbers.
    @Volatile
    var shortNumberInfo: ShortNumberInfo? = null
        get() {
            if (field == null) {
//                synchronized(this) {
                if (field == null) {
                    field = ShortNumberInfo(
                        create(), metadataDependenciesProvider.shortNumberMetadataSource
                    )
                }
//                }
            }
            return field
        }
        private set

    // An API for validation checking.
    private val matcherApi = create()

    // The set of regions that share country calling code 1.
    // There are roughly 26 regions.
    // We set the initial capacity of the HashSet to 35 to offer a load factor of roughly 0.75.
    private val nanpaRegions: MutableSet<String> = HashSet(35)

    // A cache for frequently used region-specific regular expressions.
    // The initial capacity is set to 100 as this seems to be an optimal value for Android, based on
    // performance measurements.
    private val regexCache = RegexCache(100)

    // The set of regions the library supports.
    // There are roughly 240 of them and we set the initial capacity of the HashSet to 320 to offer a
    // load factor of roughly 0.75.
    private val supportedRegions: MutableSet<String> = HashSet(320)

    // The set of country calling codes that map to the non-geo entity region ("001"). This set
    // currently contains < 12 elements so the default capacity of 16 (load factor=0.75) is fine.
    private val countryCodesForNonGeographicalRegion: MutableSet<Int> = HashSet()

    /**
     * This class implements a singleton, the constructor is only visible to facilitate testing.
     */
    // @VisibleForTesting
    init {
        for ((key, regionCodes) in countryCallingCodeToRegionCodeMap) {
            // We can assume that if the country calling code maps to the non-geo entity region code then
            // that's the only region code it maps to.
            if (regionCodes.size == 1 && REGION_CODE_FOR_NON_GEO_ENTITY == regionCodes[0]) {
                // This is the subset of all country codes that map to the non-geo entity region code.
                countryCodesForNonGeographicalRegion.add(key)
            } else {
                // The supported regions set does not include the "001" non-geo entity region code.
                supportedRegions.addAll(regionCodes)
            }
        }
        // If the non-geo entity still got added to the set of supported regions it must be because
        // there are entries that list the non-geo entity alongside normal regions (which is wrong).
        // If we discover this, remove the non-geo entity from the set of supported regions and log.
        if (supportedRegions.remove(REGION_CODE_FOR_NON_GEO_ENTITY)) {
            logger.w(
                "invalid metadata (country calling code was mapped to the non-geo " + "entity as well as specific region(s))"
            )
        }
        nanpaRegions.addAll(countryCallingCodeToRegionCodeMap[NANPA_COUNTRY_CODE]!!)
    }

    /**
     * Gets the length of the geographical area code from the
     * PhoneNumber object passed in, so that clients could use it
     * to split a national significant number into geographical area code and subscriber number. It
     * works in such a way that the resultant subscriber number should be diallable, at least on some
     * devices. An example of how this could be used:
     *
     * <pre>`PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
     * PhoneNumber number = phoneUtil.parse("16502530000", "US");
     * String nationalSignificantNumber = phoneUtil.getNationalSignificantNumber(number);
     * String areaCode;
     * String subscriberNumber;
     *
     * int areaCodeLength = phoneUtil.getLengthOfGeographicalAreaCode(number);
     * if (areaCodeLength > 0) {
     * areaCode = nationalSignificantNumber.substring(0, areaCodeLength);
     * subscriberNumber = nationalSignificantNumber.substring(areaCodeLength);
     * } else {
     * areaCode = "";
     * subscriberNumber = nationalSignificantNumber;
     * }
    `</pre> *
     *
     * N.B.: area code is a very ambiguous concept, so the I18N team generally recommends against
     * using it for most purposes, but recommends using the more general `national_number`
     * instead. Read the following carefully before deciding to use this method:
     *
     *  *  geographical area codes change over time, and this method honors those changes;
     * therefore, it doesn't guarantee the stability of the result it produces.
     *  *  subscriber numbers may not be diallable from all devices (notably mobile devices, which
     * typically requires the full national_number to be dialled in most regions).
     *  *  most non-geographical numbers have no area codes, including numbers from non-geographical
     * entities
     *  *  some geographical numbers have no area codes.
     *
     * @param number  the PhoneNumber object for which clients
     * want to know the length of the area code
     * @return  the length of area code of the PhoneNumber object
     * passed in
     */
    fun getLengthOfGeographicalAreaCode(number: PhoneNumber): Int {
        val metadata = getMetadataForRegion(getRegionCodeForNumber(number)) ?: return 0
        // If a country doesn't use a national prefix, and this number doesn't have an Italian leading
        // zero, we assume it is a closed dialling plan with no area codes.
        if (!metadata.hasNationalPrefix() && !number.isItalianLeadingZero) {
            return 0
        }
        val type = getNumberType(number)
        val countryCallingCode = number.countryCode
        if (type == PhoneNumberType.MOBILE // Note this is a rough heuristic; it doesn't cover Indonesia well, for example, where area
            // codes are present for some mobile phones but not for others. We have no better way of
            // representing this in the metadata at this point.
            && GEO_MOBILE_COUNTRIES_WITHOUT_MOBILE_AREA_CODES!!.contains(countryCallingCode)
        ) {
            return 0
        }
        return if (!isNumberGeographical(type, countryCallingCode)) {
            0
        } else getLengthOfNationalDestinationCode(number)
    }

    /**
     * Gets the length of the national destination code (NDC) from the
     * PhoneNumber object passed in, so that clients could use it
     * to split a national significant number into NDC and subscriber number. The NDC of a phone
     * number is normally the first group of digit(s) right after the country calling code when the
     * number is formatted in the international format, if there is a subscriber number part that
     * follows.
     *
     * N.B.: similar to an area code, not all numbers have an NDC!
     *
     * An example of how this could be used:
     *
     * <pre>`PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
     * PhoneNumber number = phoneUtil.parse("18002530000", "US");
     * String nationalSignificantNumber = phoneUtil.getNationalSignificantNumber(number);
     * String nationalDestinationCode;
     * String subscriberNumber;
     *
     * int nationalDestinationCodeLength = phoneUtil.getLengthOfNationalDestinationCode(number);
     * if (nationalDestinationCodeLength > 0) {
     * nationalDestinationCode = nationalSignificantNumber.substring(0,
     * nationalDestinationCodeLength);
     * subscriberNumber = nationalSignificantNumber.substring(nationalDestinationCodeLength);
     * } else {
     * nationalDestinationCode = "";
     * subscriberNumber = nationalSignificantNumber;
     * }
    `</pre> *
     *
     * Refer to the unittests to see the difference between this function and
     * [.getLengthOfGeographicalAreaCode].
     *
     * @param number  the PhoneNumber object for which clients
     * want to know the length of the NDC
     * @return  the length of NDC of the PhoneNumber object
     * passed in, which could be zero
     */
    fun getLengthOfNationalDestinationCode(number: PhoneNumber): Int {
        val copiedProto: PhoneNumber
        if (number.hasExtension()) {
            // We don't want to alter the proto given to us, but we don't want to include the extension
            // when we format it, so we copy it and clear the extension here.
            copiedProto = PhoneNumber()
            copiedProto.mergeFrom(number)
            copiedProto.clearExtension()
        } else {
            copiedProto = number
        }
        val nationalSignificantNumber = format(
            copiedProto, PhoneNumberFormat.INTERNATIONAL
        )
        val numberGroups = NON_DIGITS_PATTERN.split(nationalSignificantNumber)
        // The pattern will start with "+COUNTRY_CODE " so the first group will always be the empty
        // string (before the + symbol) and the second group will be the country calling code. The third
        // group will be area code if it is not the last group.
        if (numberGroups.size <= 3) {
            return 0
        }
        if (getNumberType(number) == PhoneNumberType.MOBILE) {
            // For example Argentinian mobile numbers, when formatted in the international format, are in
            // the form of +54 9 NDC XXXX.... As a result, we take the length of the third group (NDC) and
            // add the length of the second group (which is the mobile token), which also forms part of
            // the national significant number. This assumes that the mobile token is always formatted
            // separately from the rest of the phone number.
            val mobileToken = getCountryMobileToken(number.countryCode)
            if (mobileToken != "") {
                return numberGroups[2].length + numberGroups[3].length
            }
        }
        return numberGroups[2].length
    }

    /**
     * Returns all regions the library has metadata for.
     *
     * @return  an unordered set of the two-letter region codes for every geographical region the
     * library supports
     */
    fun getSupportedRegions(): Set<String> {
        return supportedRegions
    }

    val supportedGlobalNetworkCallingCodes: Set<Int>
        /**
         * Returns all global network calling codes the library has metadata for.
         *
         * @return  an unordered set of the country calling codes for every non-geographical entity the
         * library supports
         */
        get() = countryCodesForNonGeographicalRegion
    val supportedCallingCodes: Set<Int>
        /**
         * Returns all country calling codes the library has metadata for, covering both non-geographical
         * entities (global network calling codes) and those used for geographical entities. This could be
         * used to populate a drop-down box of country calling codes for a phone-number widget, for
         * instance.
         *
         * @return  an unordered set of the country calling codes for every geographical and
         * non-geographical entity the library supports
         */
        get() = countryCallingCodeToRegionCodeMap.keys

    /**
     * Returns the types we have metadata for based on the PhoneMetadata object passed in, which must
     * be non-null.
     */
    private fun getSupportedTypesForMetadata(metadata: PhoneMetadata?): Set<PhoneNumberType> {
        val types: MutableSet<PhoneNumberType> = mutableSetOf()
        for (type in PhoneNumberType.values()) {
            if (type == PhoneNumberType.FIXED_LINE_OR_MOBILE || type == PhoneNumberType.UNKNOWN) {
                // Never return FIXED_LINE_OR_MOBILE (it is a convenience type, and represents that a
                // particular number type can't be determined) or UNKNOWN (the non-type).
                continue
            }
            if (descHasData(getNumberDescByType(metadata, type))) {
                types.add(type)
            }
        }
        return types
    }

    /**
     * Returns the types for a given region which the library has metadata for. Will not include
     * FIXED_LINE_OR_MOBILE (if numbers in this region could be classified as FIXED_LINE_OR_MOBILE,
     * both FIXED_LINE and MOBILE would be present) and UNKNOWN.
     *
     * No types will be returned for invalid or unknown region codes.
     */
    fun getSupportedTypesForRegion(regionCode: String): Set<PhoneNumberType> {
        if (!isValidRegionCode(regionCode)) {
            logger.w("Invalid or unknown region code provided: $regionCode")
            return setOf()
        }
        val metadata = getMetadataForRegion(regionCode)
        return getSupportedTypesForMetadata(metadata)
    }

    /**
     * Returns the types for a country-code belonging to a non-geographical entity which the library
     * has metadata for. Will not include FIXED_LINE_OR_MOBILE (if numbers for this non-geographical
     * entity could be classified as FIXED_LINE_OR_MOBILE, both FIXED_LINE and MOBILE would be
     * present) and UNKNOWN.
     *
     * No types will be returned for country calling codes that do not map to a known non-geographical
     * entity.
     */
    fun getSupportedTypesForNonGeoEntity(countryCallingCode: Int): Set<PhoneNumberType> {
        val metadata = getMetadataForNonGeographicalRegion(countryCallingCode)
        if (metadata == null) {
            logger.w(
                "Unknown country calling code for a non-geographical entity " + "provided: " + countryCallingCode
            )
            return setOf()
        }
        return getSupportedTypesForMetadata(metadata)
    }

    /**
     * Tests whether a phone number has a geographical association. It checks if the number is
     * associated with a certain region in the country to which it belongs. Note that this doesn't
     * verify if the number is actually in use.
     */
    fun isNumberGeographical(phoneNumber: PhoneNumber): Boolean {
        return isNumberGeographical(getNumberType(phoneNumber), phoneNumber.countryCode)
    }

    /**
     * Overload of isNumberGeographical(PhoneNumber), since calculating the phone number type is
     * expensive; if we have already done this, we don't want to do it again.
     */
    fun isNumberGeographical(phoneNumberType: PhoneNumberType, countryCallingCode: Int): Boolean {
        return phoneNumberType == PhoneNumberType.FIXED_LINE || phoneNumberType == PhoneNumberType.FIXED_LINE_OR_MOBILE || (GEO_MOBILE_COUNTRIES!!.contains(
            countryCallingCode
        ) && phoneNumberType == PhoneNumberType.MOBILE)
    }

    /**
     * Helper function to check region code is not unknown or null.
     */
    private fun isValidRegionCode(regionCode: String?): Boolean {
        return regionCode != null && supportedRegions.contains(regionCode)
    }

    /**
     * Helper function to check the country calling code is valid.
     */
    private fun hasValidCountryCallingCode(countryCallingCode: Int): Boolean {
        return countryCallingCodeToRegionCodeMap.containsKey(countryCallingCode)
    }

    /**
     * Formats a phone number in the specified format using default rules. Note that this does not
     * promise to produce a phone number that the user can dial from where they are - although we do
     * format in either 'national' or 'international' format depending on what the client asks for, we
     * do not currently support a more abbreviated format, such as for users in the same "area" who
     * could potentially dial the number without area code. Note that if the phone number has a
     * country calling code of 0 or an otherwise invalid country calling code, we cannot work out
     * which formatting rules to apply so we return the national significant number with no formatting
     * applied.
     *
     * @param number  the phone number to be formatted
     * @param numberFormat  the format the phone number should be formatted into
     * @return  the formatted phone number
     */
    fun format(number: PhoneNumber, numberFormat: PhoneNumberFormat): String {
        if (number.nationalNumber == 0L && number.hasRawInput()) {
            // Unparseable numbers that kept their raw input just use that.
            // This is the only case where a number can be formatted as E164 without a
            // leading '+' symbol (but the original number wasn't parseable anyway).
            // TODO: Consider removing the 'if' above so that unparseable
            // strings without raw input format to the empty string instead of "+00".
            val rawInput = number.rawInput
            if (rawInput.length > 0) {
                return rawInput
            }
        }
        val formattedNumber = InplaceStringBuilder(20)
        format(number, numberFormat, formattedNumber)
        return formattedNumber.toString()
    }

    /**
     * Same as [.format], but accepts a mutable StringBuilder as
     * a parameter to decrease object creation when invoked many times.
     */
    fun format(
        number: PhoneNumber, numberFormat: PhoneNumberFormat, formattedNumber: InplaceStringBuilder
    ) {
        // Clear the StringBuilder first.
        formattedNumber.setLength(0)
        val countryCallingCode = number.countryCode
        val nationalSignificantNumber = getNationalSignificantNumber(number)
        if (numberFormat == PhoneNumberFormat.E164) {
            // Early exit for E164 case (even if the country calling code is invalid) since no formatting
            // of the national number needs to be applied. Extensions are not formatted.
            formattedNumber.append(nationalSignificantNumber)
            prefixNumberWithCountryCallingCode(
                countryCallingCode, PhoneNumberFormat.E164, formattedNumber
            )
            return
        }
        if (!hasValidCountryCallingCode(countryCallingCode)) {
            formattedNumber.append(nationalSignificantNumber)
            return
        }
        // Note getRegionCodeForCountryCode() is used because formatting information for regions which
        // share a country calling code is contained by only one region for performance reasons. For
        // example, for NANPA regions it will be contained in the metadata for US.
        val regionCode = getRegionCodeForCountryCode(countryCallingCode)
        // Metadata cannot be null because the country calling code is valid (which means that the
        // region code cannot be ZZ and must be one of our supported region codes).
        val metadata = getMetadataForRegionOrCallingCode(countryCallingCode, regionCode)
        formattedNumber.append(formatNsn(nationalSignificantNumber, metadata, numberFormat))
        maybeAppendFormattedExtension(number, metadata, numberFormat, formattedNumber)
        prefixNumberWithCountryCallingCode(countryCallingCode, numberFormat, formattedNumber)
    }

    /**
     * Formats a phone number in the specified format using client-defined formatting rules. Note that
     * if the phone number has a country calling code of zero or an otherwise invalid country calling
     * code, we cannot work out things like whether there should be a national prefix applied, or how
     * to format extensions, so we return the national significant number with no formatting applied.
     *
     * @param number  the phone number to be formatted
     * @param numberFormat  the format the phone number should be formatted into
     * @param userDefinedFormats  formatting rules specified by clients
     * @return  the formatted phone number
     */
    fun formatByPattern(
        number: PhoneNumber, numberFormat: PhoneNumberFormat, userDefinedFormats: List<Phonemetadata.NumberFormat>
    ): String {
        val countryCallingCode = number.countryCode
        val nationalSignificantNumber = getNationalSignificantNumber(number)
        if (!hasValidCountryCallingCode(countryCallingCode)) {
            return nationalSignificantNumber
        }
        // Note getRegionCodeForCountryCode() is used because formatting information for regions which
        // share a country calling code is contained by only one region for performance reasons. For
        // example, for NANPA regions it will be contained in the metadata for US.
        val regionCode = getRegionCodeForCountryCode(countryCallingCode)
        // Metadata cannot be null because the country calling code is valid.
        val metadata = getMetadataForRegionOrCallingCode(countryCallingCode, regionCode)
        val formattedNumber = InplaceStringBuilder(20)
        val formattingPattern = chooseFormattingPatternForNumber(userDefinedFormats, nationalSignificantNumber)
        if (formattingPattern == null) {
            // If no pattern above is matched, we format the number as a whole.
            formattedNumber.append(nationalSignificantNumber)
        } else {
            val numFormatCopy = newBuilder()
            // Before we do a replacement of the national prefix pattern $NP with the national prefix, we
            // need to copy the rule so that subsequent replacements for different numbers have the
            // appropriate national prefix.
            numFormatCopy.mergeFrom(formattingPattern)
            var nationalPrefixFormattingRule = formattingPattern.nationalPrefixFormattingRule
            if (nationalPrefixFormattingRule.length > 0) {
                val nationalPrefix = metadata!!.nationalPrefix
                if (nationalPrefix.length > 0) {
                    // Replace $NP with national prefix and $FG with the first group ($1).
                    nationalPrefixFormattingRule = nationalPrefixFormattingRule.replace(NP_STRING, nationalPrefix)
                    nationalPrefixFormattingRule = nationalPrefixFormattingRule.replace(FG_STRING, "$1")
                    numFormatCopy.setNationalPrefixFormattingRule(nationalPrefixFormattingRule)
                } else {
                    // We don't want to have a rule for how to format the national prefix if there isn't one.
                    numFormatCopy.clearNationalPrefixFormattingRule()
                }
            }
            formattedNumber.append(
                formatNsnUsingPattern(nationalSignificantNumber, numFormatCopy.build(), numberFormat)
            )
        }
        maybeAppendFormattedExtension(number, metadata, numberFormat, formattedNumber)
        prefixNumberWithCountryCallingCode(countryCallingCode, numberFormat, formattedNumber)
        return formattedNumber.toString()
    }

    /**
     * Formats a phone number in national format for dialing using the carrier as specified in the
     * `carrierCode`. The `carrierCode` will always be used regardless of whether the
     * phone number already has a preferred domestic carrier code stored. If `carrierCode`
     * contains an empty string, returns the number in national format without any carrier code.
     *
     * @param number  the phone number to be formatted
     * @param carrierCode  the carrier selection code to be used
     * @return  the formatted phone number in national format for dialing using the carrier as
     * specified in the `carrierCode`
     */
    fun formatNationalNumberWithCarrierCode(number: PhoneNumber, carrierCode: CharSequence?): String {
        val countryCallingCode = number.countryCode
        val nationalSignificantNumber = getNationalSignificantNumber(number)
        if (!hasValidCountryCallingCode(countryCallingCode)) {
            return nationalSignificantNumber
        }

        // Note getRegionCodeForCountryCode() is used because formatting information for regions which
        // share a country calling code is contained by only one region for performance reasons. For
        // example, for NANPA regions it will be contained in the metadata for US.
        val regionCode = getRegionCodeForCountryCode(countryCallingCode)
        // Metadata cannot be null because the country calling code is valid.
        val metadata = getMetadataForRegionOrCallingCode(countryCallingCode, regionCode)
        val formattedNumber = InplaceStringBuilder(20)
        formattedNumber.append(
            formatNsn(
                nationalSignificantNumber, metadata, PhoneNumberFormat.NATIONAL, carrierCode
            )
        )
        maybeAppendFormattedExtension(number, metadata, PhoneNumberFormat.NATIONAL, formattedNumber)
        prefixNumberWithCountryCallingCode(
            countryCallingCode, PhoneNumberFormat.NATIONAL, formattedNumber
        )
        return formattedNumber.toString()
    }

    private fun getMetadataForRegionOrCallingCode(
        countryCallingCode: Int, regionCode: String?
    ): PhoneMetadata? {
        return if (REGION_CODE_FOR_NON_GEO_ENTITY == regionCode) getMetadataForNonGeographicalRegion(countryCallingCode) else getMetadataForRegion(
            regionCode
        )
    }

    /**
     * Formats a phone number in national format for dialing using the carrier as specified in the
     * preferredDomesticCarrierCode field of the PhoneNumber object passed in. If that is missing,
     * use the `fallbackCarrierCode` passed in instead. If there is no
     * `preferredDomesticCarrierCode`, and the `fallbackCarrierCode` contains an empty
     * string, return the number in national format without any carrier code.
     *
     *
     * Use [.formatNationalNumberWithCarrierCode] instead if the carrier code passed in
     * should take precedence over the number's `preferredDomesticCarrierCode` when formatting.
     *
     * @param number  the phone number to be formatted
     * @param fallbackCarrierCode  the carrier selection code to be used, if none is found in the
     * phone number itself
     * @return  the formatted phone number in national format for dialing using the number's
     * `preferredDomesticCarrierCode`, or the `fallbackCarrierCode` passed in if
     * none is found
     */
    fun formatNationalNumberWithPreferredCarrierCode(
        number: PhoneNumber, fallbackCarrierCode: CharSequence?
    ): String {
        return formatNationalNumberWithCarrierCode(
            number,  // Historically, we set this to an empty string when parsing with raw input if none was
            // found in the input string. However, this doesn't result in a number we can dial. For this
            // reason, we treat the empty string the same as if it isn't set at all.
            if (number.preferredDomesticCarrierCode.length > 0) number.preferredDomesticCarrierCode else fallbackCarrierCode
        )
    }

    /**
     * Returns a number formatted in such a way that it can be dialed from a mobile phone in a
     * specific region. If the number cannot be reached from the region (e.g. some countries block
     * toll-free numbers from being called outside of the country), the method returns an empty
     * string.
     *
     * @param number  the phone number to be formatted
     * @param regionCallingFrom  the region where the call is being placed
     * @param withFormatting  whether the number should be returned with formatting symbols, such as
     * spaces and dashes.
     * @return  the formatted phone number
     */
    fun formatNumberForMobileDialing(
        number: PhoneNumber, regionCallingFrom: String, withFormatting: Boolean
    ): String {
        val countryCallingCode = number.countryCode
        if (!hasValidCountryCallingCode(countryCallingCode)) {
            return if (number.hasRawInput()) number.rawInput else ""
        }
        var formattedNumber = ""
        // Clear the extension, as that part cannot normally be dialed together with the main number.
        val numberNoExt = PhoneNumber().mergeFrom(number).clearExtension()
        val regionCode = getRegionCodeForCountryCode(countryCallingCode)
        val numberType = getNumberType(numberNoExt)
        val isValidNumber = (numberType != PhoneNumberType.UNKNOWN)
        if ((regionCallingFrom == regionCode)) {
            val isFixedLineOrMobile =
                ((numberType == PhoneNumberType.FIXED_LINE) || (numberType == PhoneNumberType.MOBILE) || (numberType == PhoneNumberType.FIXED_LINE_OR_MOBILE))
            // Carrier codes may be needed in some countries. We handle this here.
            if ((regionCode == "BR") && isFixedLineOrMobile) {
                // Historically, we set this to an empty string when parsing with raw input if none was
                // found in the input string. However, this doesn't result in a number we can dial. For this
                // reason, we treat the empty string the same as if it isn't set at all.
                formattedNumber =
                    if (numberNoExt.preferredDomesticCarrierCode.length > 0) formatNationalNumberWithPreferredCarrierCode(
                        numberNoExt, ""
                    ).also {
                        formattedNumber =
                            it // Brazilian fixed line and mobile numbers need to be dialed with a carrier code when
                        // called within Brazil. Without that, most of the carriers won't connect the call.
                        // Because of that, we return an empty string here.
                    } else ""
            } else if (countryCallingCode == NANPA_COUNTRY_CODE) {
                // For NANPA countries, we output international format for numbers that can be dialed
                // internationally, since that always works, except for numbers which might potentially be
                // short numbers, which are always dialled in national format.
                val regionMetadata = getMetadataForRegion(regionCallingFrom)
                if ((canBeInternationallyDialled(numberNoExt) && testNumberLength(
                        getNationalSignificantNumber(
                            numberNoExt
                        ), regionMetadata
                    ) != ValidationResult.TOO_SHORT)
                ) {
                    formattedNumber = format(numberNoExt, PhoneNumberFormat.INTERNATIONAL)
                } else {
                    formattedNumber = format(numberNoExt, PhoneNumberFormat.NATIONAL)
                }
            } else {
                // For non-geographical countries, and Mexican, Chilean, and Uzbek fixed line and mobile
                // numbers, we output international format for numbers that can be dialed internationally as
                // that always works.
                if ((((regionCode == REGION_CODE_FOR_NON_GEO_ENTITY) || ((((regionCode == "MX") || (regionCode == "CL") || (regionCode == "UZ"))) && isFixedLineOrMobile)) && canBeInternationallyDialled(
                        numberNoExt
                    ))
                ) {
                    formattedNumber = format(numberNoExt, PhoneNumberFormat.INTERNATIONAL)
                } else {
                    formattedNumber = format(numberNoExt, PhoneNumberFormat.NATIONAL)
                }
            }
        } else if (isValidNumber && canBeInternationallyDialled(numberNoExt)) {
            // We assume that short numbers are not diallable from outside their region, so if a number
            // is not a valid regular length phone number, we treat it as if it cannot be internationally
            // dialled.
            return if (withFormatting) format(numberNoExt, PhoneNumberFormat.INTERNATIONAL) else format(
                numberNoExt, PhoneNumberFormat.E164
            )
        }
        return if (withFormatting) formattedNumber else normalizeDiallableCharsOnly(formattedNumber)
    }

    /**
     * Formats a phone number for out-of-country dialing purposes. If no regionCallingFrom is
     * supplied, we format the number in its INTERNATIONAL format. If the country calling code is the
     * same as that of the region where the number is from, then NATIONAL formatting will be applied.
     *
     *
     * If the number itself has a country calling code of zero or an otherwise invalid country
     * calling code, then we return the number with no formatting applied.
     *
     *
     * Note this function takes care of the case for calling inside of NANPA and between Russia and
     * Kazakhstan (who share the same country calling code). In those cases, no international prefix
     * is used. For regions which have multiple international prefixes, the number in its
     * INTERNATIONAL format will be returned instead.
     *
     * @param number  the phone number to be formatted
     * @param regionCallingFrom  the region where the call is being placed
     * @return  the formatted phone number
     */
    fun formatOutOfCountryCallingNumber(
        number: PhoneNumber, regionCallingFrom: String
    ): String {
        if (!isValidRegionCode(regionCallingFrom)) {
            logger.w(
                "Trying to format number from invalid region " + regionCallingFrom + ". International formatting applied."
            )
            return format(number, PhoneNumberFormat.INTERNATIONAL)
        }
        val countryCallingCode = number.countryCode
        val nationalSignificantNumber = getNationalSignificantNumber(number)
        if (!hasValidCountryCallingCode(countryCallingCode)) {
            return nationalSignificantNumber
        }
        if (countryCallingCode == NANPA_COUNTRY_CODE) {
            if (isNANPACountry(regionCallingFrom)) {
                // For NANPA regions, return the national format for these regions but prefix it with the
                // country calling code.
                return countryCallingCode.toString() + " " + format(number, PhoneNumberFormat.NATIONAL)
            }
        } else if (countryCallingCode == getCountryCodeForValidRegion(regionCallingFrom)) {
            // If regions share a country calling code, the country calling code need not be dialled.
            // This also applies when dialling within a region, so this if clause covers both these cases.
            // Technically this is the case for dialling from La Reunion to other overseas departments of
            // France (French Guiana, Martinique, Guadeloupe), but not vice versa - so we don't cover this
            // edge case for now and for those cases return the version including country calling code.
            // Details here: http://www.petitfute.com/voyage/225-info-pratiques-reunion
            return format(number, PhoneNumberFormat.NATIONAL)
        }
        // Metadata cannot be null because we checked 'isValidRegionCode()' above.
        val metadataForRegionCallingFrom = getMetadataForRegion(regionCallingFrom)
        val internationalPrefix = metadataForRegionCallingFrom!!.internationalPrefix

        // In general, if there is a preferred international prefix, use that. Otherwise, for regions
        // that have multiple international prefixes, the international format of the number is
        // returned since we would not know which one to use.
        var internationalPrefixForFormatting: String? = ""
        if (metadataForRegionCallingFrom.hasPreferredInternationalPrefix()) {
            internationalPrefixForFormatting = metadataForRegionCallingFrom.preferredInternationalPrefix
        } else if (SINGLE_INTERNATIONAL_PREFIX.matches(internationalPrefix)) {
            internationalPrefixForFormatting = internationalPrefix
        }
        val regionCode = getRegionCodeForCountryCode(countryCallingCode)
        // Metadata cannot be null because the country calling code is valid.
        val metadataForRegion = getMetadataForRegionOrCallingCode(countryCallingCode, regionCode)
        val formattedNationalNumber =
            formatNsn(nationalSignificantNumber, metadataForRegion, PhoneNumberFormat.INTERNATIONAL)
        val formattedNumber = InplaceStringBuilder(formattedNationalNumber)
        maybeAppendFormattedExtension(
            number, metadataForRegion, PhoneNumberFormat.INTERNATIONAL, formattedNumber
        )
        if (internationalPrefixForFormatting!!.length > 0) {
            formattedNumber.insert(0, " ").insert(0, countryCallingCode).insert(0, " ")
                .insert(0, internationalPrefixForFormatting)
        } else {
            prefixNumberWithCountryCallingCode(
                countryCallingCode, PhoneNumberFormat.INTERNATIONAL, formattedNumber
            )
        }
        return formattedNumber.toString()
    }

    /**
     * Formats a phone number using the original phone number format (e.g. INTERNATIONAL or NATIONAL)
     * that the number is parsed from, provided that the number has been parsed with [ ]. Otherwise the number will be formatted in NATIONAL format.
     *
     *
     * The original format is embedded in the country_code_source field of the PhoneNumber object
     * passed in, which is only set when parsing keeps the raw input. When we don't have a formatting
     * pattern for the number, the method falls back to returning the raw input.
     *
     *
     * Note this method guarantees no digit will be inserted, removed or modified as a result of
     * formatting.
     *
     * @param number the phone number that needs to be formatted in its original number format
     * @param regionCallingFrom the region whose IDD needs to be prefixed if the original number has
     * one
     * @return the formatted phone number in its original number format
     */
    fun formatInOriginalFormat(number: PhoneNumber, regionCallingFrom: String): String? {
        if (number.hasRawInput() && !hasFormattingPatternForNumber(number)) {
            // We check if we have the formatting pattern because without that, we might format the number
            // as a group without national prefix.
            return number.rawInput
        }
        if (!number.hasCountryCodeSource()) {
            return format(number, PhoneNumberFormat.NATIONAL)
        }
        var formattedNumber: String?
        when (number.countryCodeSource) {
            CountryCodeSource.FROM_NUMBER_WITH_PLUS_SIGN -> formattedNumber =
                format(number, PhoneNumberFormat.INTERNATIONAL)

            CountryCodeSource.FROM_NUMBER_WITH_IDD -> formattedNumber =
                formatOutOfCountryCallingNumber(number, regionCallingFrom)

            CountryCodeSource.FROM_NUMBER_WITHOUT_PLUS_SIGN -> formattedNumber =
                format(number, PhoneNumberFormat.INTERNATIONAL).substring(1)

            CountryCodeSource.FROM_DEFAULT_COUNTRY -> run {
                val regionCode = getRegionCodeForCountryCode(number.countryCode)
                // We strip non-digits from the NDD here, and from the raw input later, so that we can
                // compare them easily.
                val nationalPrefix = getNddPrefixForRegion(regionCode, true /* strip non-digits */)
                val nationalFormat = format(number, PhoneNumberFormat.NATIONAL)
                if (nationalPrefix == null || nationalPrefix.length == 0) {
                    // If the region doesn't have a national prefix at all, we can safely return the national
                    // format without worrying about a national prefix being added.
                    formattedNumber = nationalFormat
                    return@run
                }
                // Otherwise, we check if the original number was entered with a national prefix.
                if (rawInputContainsNationalPrefix(
                        number.rawInput, nationalPrefix, regionCode
                    )
                ) {
                    // If so, we can safely return the national format.
                    formattedNumber = nationalFormat
                    return@run
                }
                // Metadata cannot be null here because getNddPrefixForRegion() (above) returns null if
                // there is no metadata for the region.
                val metadata = getMetadataForRegion(regionCode)
                val nationalNumber = getNationalSignificantNumber(number)
                val formatRule = chooseFormattingPatternForNumber(
                    metadata!!.numberFormatList, nationalNumber
                )
                // The format rule could still be null here if the national number was 0 and there was no
                // raw input (this should not be possible for numbers generated by the phonenumber library
                // as they would also not have a country calling code and we would have exited earlier).
                if (formatRule == null) {
                    formattedNumber = nationalFormat
                    return@run
                }
                // When the format we apply to this number doesn't contain national prefix, we can just
                // return the national format.
                // TODO: Refactor the code below with the code in
                // isNationalPrefixPresentIfRequired.
                var candidateNationalPrefixRule = formatRule.nationalPrefixFormattingRule
                // We assume that the first-group symbol will never be _before_ the national prefix.
                val indexOfFirstGroup = candidateNationalPrefixRule.indexOf("$1")
                if (indexOfFirstGroup <= 0) {
                    formattedNumber = nationalFormat
                    return@run
                }
                candidateNationalPrefixRule = candidateNationalPrefixRule.substring(0, indexOfFirstGroup)
                candidateNationalPrefixRule = normalizeDigitsOnly(candidateNationalPrefixRule)
                if (candidateNationalPrefixRule.length == 0) {
                    // National prefix not used when formatting this number.
                    formattedNumber = nationalFormat
                    return@run
                }
                // Otherwise, we need to remove the national prefix from our output.
                val numFormatCopy = newBuilder()
                numFormatCopy.mergeFrom(formatRule)
                numFormatCopy.clearNationalPrefixFormattingRule()
                val numberFormats: MutableList<Phonemetadata.NumberFormat> = ArrayList(1)
                numberFormats.add(numFormatCopy.build())
                formattedNumber = formatByPattern(number, PhoneNumberFormat.NATIONAL, numberFormats)
            }

            else -> run {
                val regionCode = getRegionCodeForCountryCode(number.countryCode)
                val nationalPrefix = getNddPrefixForRegion(regionCode, true)
                val nationalFormat = format(number, PhoneNumberFormat.NATIONAL)
                if (nationalPrefix == null || nationalPrefix.length == 0) {
                    formattedNumber = nationalFormat
                    return@run
                }
                if (rawInputContainsNationalPrefix(
                        number.rawInput, nationalPrefix, regionCode
                    )
                ) {
                    formattedNumber = nationalFormat
                    return@run
                }
                val metadata = getMetadataForRegion(regionCode)
                val nationalNumber = getNationalSignificantNumber(number)
                val formatRule = chooseFormattingPatternForNumber(
                    metadata!!.numberFormatList, nationalNumber
                )
                if (formatRule == null) {
                    formattedNumber = nationalFormat
                    return@run
                }
                var candidateNationalPrefixRule = formatRule.nationalPrefixFormattingRule
                val indexOfFirstGroup = candidateNationalPrefixRule.indexOf("$1")
                if (indexOfFirstGroup <= 0) {
                    formattedNumber = nationalFormat
                    return@run
                }
                candidateNationalPrefixRule = candidateNationalPrefixRule.substring(0, indexOfFirstGroup)
                candidateNationalPrefixRule = normalizeDigitsOnly(candidateNationalPrefixRule)
                if (candidateNationalPrefixRule.length == 0) {
                    formattedNumber = nationalFormat
                    return@run
                }
                val numFormatCopy = newBuilder()
                numFormatCopy.mergeFrom(formatRule)
                numFormatCopy.clearNationalPrefixFormattingRule()
                val numberFormats: MutableList<Phonemetadata.NumberFormat> = ArrayList(1)
                numberFormats.add(numFormatCopy.build())
                formattedNumber = formatByPattern(number, PhoneNumberFormat.NATIONAL, numberFormats)
            }
        }
        val rawInput = number.rawInput
        // If no digit is inserted/removed/modified as a result of our formatting, we return the
        // formatted phone number; otherwise we return the raw input the user entered.
        if (formattedNumber != null && rawInput.isNotEmpty()) {
            val normalizedFormattedNumber = normalizeDiallableCharsOnly(formattedNumber!!)
            val normalizedRawInput = normalizeDiallableCharsOnly(rawInput)
            if (normalizedFormattedNumber != normalizedRawInput) {
                formattedNumber = rawInput
            }
        }
        return formattedNumber
    }

    // Check if rawInput, which is assumed to be in the national format, has a national prefix. The
    // national prefix is assumed to be in digits-only form.
    private fun rawInputContainsNationalPrefix(
        rawInput: String, nationalPrefix: String, regionCode: String
    ): Boolean {
        val normalizedNationalNumber = normalizeDigitsOnly(rawInput)
        return if (normalizedNationalNumber.startsWith(nationalPrefix)) {
            try {
                // Some Japanese numbers (e.g. 00777123) might be mistaken to contain the national prefix
                // when written without it (e.g. 0777123) if we just do prefix matching. To tackle that, we
                // check the validity of the number if the assumed national prefix is removed (777123 won't
                // be valid in Japan).
                isValidNumber(
                    parse(normalizedNationalNumber.substring(nationalPrefix.length), regionCode)
                )
            } catch (e: NumberParseException) {
                false
            }
        } else false
    }

    private fun hasFormattingPatternForNumber(number: PhoneNumber): Boolean {
        val countryCallingCode = number.countryCode
        val phoneNumberRegion = getRegionCodeForCountryCode(countryCallingCode)
        val metadata = getMetadataForRegionOrCallingCode(countryCallingCode, phoneNumberRegion) ?: return false
        val nationalNumber = getNationalSignificantNumber(number)
        val formatRule = chooseFormattingPatternForNumber(metadata.numberFormatList, nationalNumber)
        return formatRule != null
    }

    /**
     * Formats a phone number for out-of-country dialing purposes.
     *
     * Note that in this version, if the number was entered originally using alpha characters and
     * this version of the number is stored in raw_input, this representation of the number will be
     * used rather than the digit representation. Grouping information, as specified by characters
     * such as "-" and " ", will be retained.
     *
     *
     * **Caveats:**
     *
     *  *  This will not produce good results if the country calling code is both present in the raw
     * input _and_ is the start of the national number. This is not a problem in the regions
     * which typically use alpha numbers.
     *  *  This will also not produce good results if the raw input has any grouping information
     * within the first three digits of the national number, and if the function needs to strip
     * preceding digits/words in the raw input before these digits. Normally people group the
     * first three digits together so this is not a huge problem - and will be fixed if it
     * proves to be so.
     *
     *
     * @param number  the phone number that needs to be formatted
     * @param regionCallingFrom  the region where the call is being placed
     * @return  the formatted phone number
     */
    fun formatOutOfCountryKeepingAlphaChars(
        number: PhoneNumber, regionCallingFrom: String
    ): String {
        var rawInput = number.rawInput
        // If there is no raw input, then we can't keep alpha characters because there aren't any.
        // In this case, we return formatOutOfCountryCallingNumber.
        if (rawInput.length == 0) {
            return formatOutOfCountryCallingNumber(number, regionCallingFrom)
        }
        val countryCode = number.countryCode
        if (!hasValidCountryCallingCode(countryCode)) {
            return rawInput
        }
        // Strip any prefix such as country calling code, IDD, that was present. We do this by comparing
        // the number in raw_input with the parsed number.
        // To do this, first we normalize punctuation. We retain number grouping symbols such as " "
        // only.
        rawInput = normalizeHelper(rawInput, ALL_PLUS_NUMBER_GROUPING_SYMBOLS, true)
        // Now we trim everything before the first three digits in the parsed number. We choose three
        // because all valid alpha numbers have 3 digits at the start - if it does not, then we don't
        // trim anything at all. Similarly, if the national number was less than three digits, we don't
        // trim anything at all.
        val nationalNumber = getNationalSignificantNumber(number)
        if (nationalNumber.length > 3) {
            val firstNationalNumberDigit = rawInput.indexOf(nationalNumber.substring(0, 3))
            if (firstNationalNumberDigit != -1) {
                rawInput = rawInput.substring(firstNationalNumberDigit)
            }
        }
        val metadataForRegionCallingFrom = getMetadataForRegion(regionCallingFrom)
        if (countryCode == NANPA_COUNTRY_CODE) {
            if (isNANPACountry(regionCallingFrom)) {
                return "$countryCode $rawInput"
            }
        } else if ((metadataForRegionCallingFrom != null && countryCode == getCountryCodeForValidRegion(
                regionCallingFrom
            ))
        ) {
            val formattingPattern = chooseFormattingPatternForNumber(
                metadataForRegionCallingFrom.numberFormatList, nationalNumber
            ) ?: // If no pattern above is matched, we format the original input.
            return rawInput
            val newFormat = newBuilder()
            newFormat.mergeFrom(formattingPattern)
            // The first group is the first group of digits that the user wrote together.
            newFormat.setPattern("(\\d+)(.*)")
            // Here we just concatenate them back together after the national prefix has been fixed.
            newFormat.setFormat("$1$2")
            // Now we format using this pattern instead of the default pattern, but with the national
            // prefix prefixed if necessary.
            // This will not work in the cases where the pattern (and not the leading digits) decide
            // whether a national prefix needs to be used, since we have overridden the pattern to match
            // anything, but that is not the case in the metadata to date.
            return formatNsnUsingPattern(rawInput, newFormat.build(), PhoneNumberFormat.NATIONAL)
        }
        var internationalPrefixForFormatting = ""
        // If an unsupported region-calling-from is entered, or a country with multiple international
        // prefixes, the international format of the number is returned, unless there is a preferred
        // international prefix.
        if (metadataForRegionCallingFrom != null) {
            val internationalPrefix = metadataForRegionCallingFrom.internationalPrefix
            internationalPrefixForFormatting = if (SINGLE_INTERNATIONAL_PREFIX.matches(internationalPrefix)) {
                internationalPrefix
            } else metadataForRegionCallingFrom.preferredInternationalPrefix
        }
        val formattedNumber = InplaceStringBuilder(rawInput)
        val regionCode = getRegionCodeForCountryCode(countryCode)
        // Metadata cannot be null because the country calling code is valid.
        val metadataForRegion = getMetadataForRegionOrCallingCode(countryCode, regionCode)
        maybeAppendFormattedExtension(
            number, metadataForRegion, PhoneNumberFormat.INTERNATIONAL, formattedNumber
        )
        if (internationalPrefixForFormatting.length > 0) {
            formattedNumber.insert(0, " ").insert(0, countryCode).insert(0, " ")
                .insert(0, internationalPrefixForFormatting)
        } else {
            // Invalid region entered as country-calling-from (so no metadata was found for it) or the
            // region chosen has multiple international dialling prefixes.
            if (!isValidRegionCode(regionCallingFrom)) {
                logger.w(
                    "Trying to format number from invalid region $regionCallingFrom. International formatting applied."
                )
            }
            prefixNumberWithCountryCallingCode(
                countryCode, PhoneNumberFormat.INTERNATIONAL, formattedNumber
            )
        }
        return formattedNumber.toString()
    }

    /**
     * Gets the national significant number of a phone number. Note a national significant number
     * doesn't contain a national prefix or any formatting.
     *
     * @param number  the phone number for which the national significant number is needed
     * @return  the national significant number of the PhoneNumber object passed in
     */
    fun getNationalSignificantNumber(number: PhoneNumber): String {
        // If leading zero(s) have been set, we prefix this now. Note this is not a national prefix.
        val nationalNumber = InplaceStringBuilder()
        if (number.isItalianLeadingZero && number.numberOfLeadingZeros > 0) {
            val zeros = CharArray(number.numberOfLeadingZeros) { '0' }
            nationalNumber.append(zeros)
        }
        nationalNumber.append(number.nationalNumber)
        return nationalNumber.toString()
    }

    /**
     * A helper function that is used by format and formatByPattern.
     */
    private fun prefixNumberWithCountryCallingCode(
        countryCallingCode: Int, numberFormat: PhoneNumberFormat, formattedNumber: InplaceStringBuilder
    ) {
        when (numberFormat) {
            PhoneNumberFormat.E164 -> {
                formattedNumber.insert(0, countryCallingCode).insert(0, PLUS_SIGN)
                return
            }

            PhoneNumberFormat.INTERNATIONAL -> {
                formattedNumber.insert(0, " ").insert(0, countryCallingCode).insert(0, PLUS_SIGN)
                return
            }

            PhoneNumberFormat.RFC3966 -> {
                formattedNumber.insert(0, "-").insert(0, countryCallingCode).insert(0, PLUS_SIGN)
                    .insert(0, RFC3966_PREFIX)
                return
            }

            PhoneNumberFormat.NATIONAL -> return
            else -> return
        }
    }

    // Note in some regions, the national number can be written in two completely different ways
    // depending on whether it forms part of the NATIONAL format or INTERNATIONAL format. The
    // numberFormat parameter here is used to specify which format to use for those cases. If a
    // carrierCode is specified, this will be inserted into the formatted string to replace $CC.
    // Simple wrapper of formatNsn for the common case of no carrier code.
    private fun formatNsn(
        number: String, metadata: PhoneMetadata?, numberFormat: PhoneNumberFormat, carrierCode: CharSequence? = null
    ): String {
        val intlNumberFormats = metadata!!.intlNumberFormatList
        // When the intlNumberFormats exists, we use that to format national number for the
        // INTERNATIONAL format instead of using the numberDesc.numberFormats.
        val availableFormats =
            if (intlNumberFormats.size == 0 || numberFormat == PhoneNumberFormat.NATIONAL) metadata.numberFormatList else metadata.intlNumberFormatList
        val formattingPattern = chooseFormattingPatternForNumber(availableFormats, number)
        return formattingPattern?.let { formatNsnUsingPattern(number, it, numberFormat, carrierCode) } ?: number
    }

    fun chooseFormattingPatternForNumber(
        availableFormats: List<Phonemetadata.NumberFormat>, nationalNumber: String
    ): Phonemetadata.NumberFormat? {
        for (numFormat in availableFormats) {
            val size = numFormat.leadingDigitsPatternCount
            if (size == 0 || regexCache.getRegexForPattern( // We always use the last leading_digits_pattern, as it is the most detailed.
                    numFormat.getLeadingDigitsPattern(size - 1)
                ).matchesAt(nationalNumber, 0)
            ) {
                if (regexCache.getRegexForPattern(numFormat.pattern).matches(nationalNumber)) {
                    return numFormat
                }
            }
        }
        return null
    }

    // Simple wrapper of formatNsnUsingPattern for the common case of no carrier code.
    fun formatNsnUsingPattern(
        nationalNumber: String, formattingPattern: Phonemetadata.NumberFormat, numberFormat: PhoneNumberFormat
    ): String {
        return formatNsnUsingPattern(nationalNumber, formattingPattern, numberFormat, null)
    }

    // Note that carrierCode is optional - if null or an empty string, no carrier code replacement
    // will take place.
    private fun formatNsnUsingPattern(
        nationalNumber: String,
        formattingPattern: Phonemetadata.NumberFormat,
        numberFormat: PhoneNumberFormat,
        carrierCode: CharSequence?
    ): String {
        var numberFormatRule = formattingPattern.format
        val formattingPatternRegex = regexCache.getRegexForPattern(formattingPattern.pattern)
        var formattedNationalNumber = ""
        if (numberFormat == PhoneNumberFormat.NATIONAL && carrierCode != null && carrierCode.length > 0 && formattingPattern.domesticCarrierCodeFormattingRule.length > 0) {
            // Replace the $CC in the formatting rule with the desired carrier code.
            var carrierCodeFormattingRule = formattingPattern.domesticCarrierCodeFormattingRule
            carrierCodeFormattingRule = carrierCodeFormattingRule.replace(CC_STRING, carrierCode.toString())
            // Now replace the $FG in the formatting rule with the first group and the carrier code
            // combined in the appropriate way.
            numberFormatRule = numberFormatRule.replaceFirst(FIRST_GROUP_PATTERN, carrierCodeFormattingRule)
            formattedNationalNumber = nationalNumber.replace(formattingPatternRegex, numberFormatRule)
        } else {
            // Use the national prefix formatting rule instead.
            val nationalPrefixFormattingRule = formattingPattern.nationalPrefixFormattingRule
            formattedNationalNumber =
                if (numberFormat == PhoneNumberFormat.NATIONAL && nationalPrefixFormattingRule.isNotEmpty()) {
                    nationalNumber.replace(
                        formattingPatternRegex,
                        numberFormatRule.replaceFirst(FIRST_GROUP_PATTERN, nationalPrefixFormattingRule)
                    )
                } else {
                    nationalNumber.replace(formattingPatternRegex, numberFormatRule)
                }
        }
        if (numberFormat == PhoneNumberFormat.RFC3966) {
            // Strip any leading punctuation.
            val matchResult = SEPARATOR_PATTERN.matchAt(formattedNationalNumber, 0)
            if (matchResult != null) {
                formattedNationalNumber = formattedNationalNumber.replaceFirst(SEPARATOR_PATTERN, "")
            }
            // Replace the rest with a dash between each number group.
            formattedNationalNumber = formattedNationalNumber.replace(SEPARATOR_PATTERN, "-")
        }
        return formattedNationalNumber
    }

    /**
     * Gets a valid number for the specified region.
     *
     * @param regionCode  the region for which an example number is needed
     * @return  a valid fixed-line number for the specified region. Returns null when the metadata
     * does not contain such information, or the region 001 is passed in. For 001 (representing
     * non-geographical numbers), call [.getExampleNumberForNonGeoEntity] instead.
     */
    fun getExampleNumber(regionCode: String): PhoneNumber? {
        return getExampleNumberForType(regionCode, PhoneNumberType.FIXED_LINE)
    }

    /**
     * Gets an invalid number for the specified region. This is useful for unit-testing purposes,
     * where you want to test what will happen with an invalid number. Note that the number that is
     * returned will always be able to be parsed and will have the correct country code. It may also
     * be a valid *short* number/code for this region. Validity checking such numbers is handled with
     * [io.michaelrocks.libphonenumber.android.ShortNumberInfo].
     *
     * @param regionCode  the region for which an example number is needed
     * @return  an invalid number for the specified region. Returns null when an unsupported region or
     * the region 001 (Earth) is passed in.
     */
    fun getInvalidExampleNumber(regionCode: String): PhoneNumber? {
        if (!isValidRegionCode(regionCode)) {
            logger.w("Invalid or unknown region code provided: $regionCode")
            return null
        }
        // We start off with a valid fixed-line number since every country supports this. Alternatively
        // we could start with a different number type, since fixed-line numbers typically have a wide
        // breadth of valid number lengths and we may have to make it very short before we get an
        // invalid number.
        val desc = getNumberDescByType(
            getMetadataForRegion(regionCode), PhoneNumberType.FIXED_LINE
        )
        if (!desc!!.hasExampleNumber()) {
            // This shouldn't happen; we have a test for this.
            return null
        }
        val exampleNumber = desc.exampleNumber
        // Try and make the number invalid. We do this by changing the length. We try reducing the
        // length of the number, since currently no region has a number that is the same length as
        // MIN_LENGTH_FOR_NSN. This is probably quicker than making the number longer, which is another
        // alternative. We could also use the possible number pattern to extract the possible lengths of
        // the number to make this faster, but this method is only for unit-testing so simplicity is
        // preferred to performance.  We don't want to return a number that can't be parsed, so we check
        // the number is long enough. We try all possible lengths because phone number plans often have
        // overlapping prefixes so the number 123456 might be valid as a fixed-line number, and 12345 as
        // a mobile number. It would be faster to loop in a different order, but we prefer numbers that
        // look closer to real numbers (and it gives us a variety of different lengths for the resulting
        // phone numbers - otherwise they would all be MIN_LENGTH_FOR_NSN digits long.)
        for (phoneNumberLength in exampleNumber.length - 1 downTo MIN_LENGTH_FOR_NSN) {
            val numberToTry = exampleNumber.substring(0, phoneNumberLength)
            try {
                val possiblyValidNumber = parse(numberToTry, regionCode)
                if (!isValidNumber(possiblyValidNumber)) {
                    return possiblyValidNumber
                }
            } catch (e: NumberParseException) {
                // Shouldn't happen: we have already checked the length, we know example numbers have
                // only valid digits, and we know the region code is fine.
            }
        }
        // We have a test to check that this doesn't happen for any of our supported regions.
        return null
    }

    /**
     * Gets a valid number for the specified region and number type.
     *
     * @param regionCode  the region for which an example number is needed
     * @param type  the type of number that is needed
     * @return  a valid number for the specified region and type. Returns null when the metadata
     * does not contain such information or if an invalid region or region 001 was entered.
     * For 001 (representing non-geographical numbers), call
     * [.getExampleNumberForNonGeoEntity] instead.
     */
    fun getExampleNumberForType(regionCode: String, type: PhoneNumberType?): PhoneNumber? {
        // Check the region code is valid.
        if (!isValidRegionCode(regionCode)) {
            logger.w("Invalid or unknown region code provided: $regionCode")
            return null
        }
        val desc = getNumberDescByType(getMetadataForRegion(regionCode), type)
        try {
            if (desc!!.hasExampleNumber()) {
                return parse(desc.exampleNumber, regionCode)
            }
        } catch (e: NumberParseException) {
            logger.e(e.toString())
        }
        return null
    }

    /**
     * Gets a valid number for the specified number type (it may belong to any country).
     *
     * @param type  the type of number that is needed
     * @return  a valid number for the specified type. Returns null when the metadata
     * does not contain such information. This should only happen when no numbers of this type are
     * allocated anywhere in the world anymore.
     */
    fun getExampleNumberForType(type: PhoneNumberType?): PhoneNumber? {
        for (regionCode in getSupportedRegions()) {
            val exampleNumber = getExampleNumberForType(regionCode, type)
            if (exampleNumber != null) {
                return exampleNumber
            }
        }
        // If there wasn't an example number for a region, try the non-geographical entities.
        for (countryCallingCode in supportedGlobalNetworkCallingCodes) {
            val desc = getNumberDescByType(
                getMetadataForNonGeographicalRegion(countryCallingCode), type
            )
            try {
                if (desc!!.hasExampleNumber()) {
                    return parse("+" + countryCallingCode + desc.exampleNumber, UNKNOWN_REGION)
                }
            } catch (e: NumberParseException) {
                logger.e(e.toString())
            }
        }
        // There are no example numbers of this type for any country in the library.
        return null
    }

    /**
     * Gets a valid number for the specified country calling code for a non-geographical entity.
     *
     * @param countryCallingCode  the country calling code for a non-geographical entity
     * @return  a valid number for the non-geographical entity. Returns null when the metadata
     * does not contain such information, or the country calling code passed in does not belong
     * to a non-geographical entity.
     */
    fun getExampleNumberForNonGeoEntity(countryCallingCode: Int): PhoneNumber? {
        val metadata = getMetadataForNonGeographicalRegion(countryCallingCode)
        if (metadata != null) {
            // For geographical entities, fixed-line data is always present. However, for non-geographical
            // entities, this is not the case, so we have to go through different types to find the
            // example number. We don't check fixed-line or personal number since they aren't used by
            // non-geographical entities (if this changes, a unit-test will catch this.)
            for (desc: PhoneNumberDesc? in listOf(
                metadata.mobile,
                metadata.tollFree,
                metadata.sharedCost,
                metadata.voip,
                metadata.voicemail,
                metadata.uan,
                metadata.premiumRate
            )) {
                try {
                    if (desc != null && desc.hasExampleNumber()) {
                        return parse("+" + countryCallingCode + desc.exampleNumber, UNKNOWN_REGION)
                    }
                } catch (e: NumberParseException) {
                    logger.e(e.toString())
                }
            }
        } else {
            logger.w(
                "Invalid or unknown country calling code provided: $countryCallingCode"
            )
        }
        return null
    }

    /**
     * Appends the formatted extension of a phone number to formattedNumber, if the phone number had
     * an extension specified.
     */
    private fun maybeAppendFormattedExtension(
        number: PhoneNumber,
        metadata: PhoneMetadata?,
        numberFormat: PhoneNumberFormat,
        formattedNumber: InplaceStringBuilder
    ) {
        if (number.hasExtension() && number.extension.length > 0) {
            if (numberFormat == PhoneNumberFormat.RFC3966) {
                formattedNumber.append(RFC3966_EXTN_PREFIX).append(number.extension)
            } else {
                if (metadata!!.hasPreferredExtnPrefix()) {
                    formattedNumber.append(metadata.preferredExtnPrefix).append(number.extension)
                } else {
                    formattedNumber.append(DEFAULT_EXTN_PREFIX).append(number.extension)
                }
            }
        }
    }

    fun getNumberDescByType(metadata: PhoneMetadata?, type: PhoneNumberType?): PhoneNumberDesc? {
        return when (type) {
            PhoneNumberType.PREMIUM_RATE -> metadata!!.premiumRate
            PhoneNumberType.TOLL_FREE -> metadata!!.tollFree
            PhoneNumberType.MOBILE -> metadata!!.mobile
            PhoneNumberType.FIXED_LINE, PhoneNumberType.FIXED_LINE_OR_MOBILE -> metadata!!.fixedLine
            PhoneNumberType.SHARED_COST -> metadata!!.sharedCost
            PhoneNumberType.VOIP -> metadata!!.voip
            PhoneNumberType.PERSONAL_NUMBER -> metadata!!.personalNumber
            PhoneNumberType.PAGER -> metadata!!.pager
            PhoneNumberType.UAN -> metadata!!.uan
            PhoneNumberType.VOICEMAIL -> metadata!!.voicemail
            else -> metadata!!.generalDesc
        }
    }

    /**
     * Gets the type of a valid phone number.
     *
     * @param number  the phone number that we want to know the type
     * @return  the type of the phone number, or UNKNOWN if it is invalid
     */
    fun getNumberType(number: PhoneNumber): PhoneNumberType {
        val regionCode = getRegionCodeForNumber(number)
        val metadata =
            getMetadataForRegionOrCallingCode(number.countryCode, regionCode) ?: return PhoneNumberType.UNKNOWN
        val nationalSignificantNumber = getNationalSignificantNumber(number)
        return getNumberTypeHelper(nationalSignificantNumber, metadata)
    }

    private fun getNumberTypeHelper(nationalNumber: String, metadata: PhoneMetadata?): PhoneNumberType {
        if (!isNumberMatchingDesc(nationalNumber, metadata!!.generalDesc)) {
            return PhoneNumberType.UNKNOWN
        }
        if (isNumberMatchingDesc(nationalNumber, metadata.premiumRate)) {
            return PhoneNumberType.PREMIUM_RATE
        }
        if (isNumberMatchingDesc(nationalNumber, metadata.tollFree)) {
            return PhoneNumberType.TOLL_FREE
        }
        if (isNumberMatchingDesc(nationalNumber, metadata.sharedCost)) {
            return PhoneNumberType.SHARED_COST
        }
        if (isNumberMatchingDesc(nationalNumber, metadata.voip)) {
            return PhoneNumberType.VOIP
        }
        if (isNumberMatchingDesc(nationalNumber, metadata.personalNumber)) {
            return PhoneNumberType.PERSONAL_NUMBER
        }
        if (isNumberMatchingDesc(nationalNumber, metadata.pager)) {
            return PhoneNumberType.PAGER
        }
        if (isNumberMatchingDesc(nationalNumber, metadata.uan)) {
            return PhoneNumberType.UAN
        }
        if (isNumberMatchingDesc(nationalNumber, metadata.voicemail)) {
            return PhoneNumberType.VOICEMAIL
        }
        val isFixedLine = isNumberMatchingDesc(nationalNumber, metadata.fixedLine)
        if (isFixedLine) {
            if (metadata.sameMobileAndFixedLinePattern) {
                return PhoneNumberType.FIXED_LINE_OR_MOBILE
            } else if (isNumberMatchingDesc(nationalNumber, metadata.mobile)) {
                return PhoneNumberType.FIXED_LINE_OR_MOBILE
            }
            return PhoneNumberType.FIXED_LINE
        }
        // Otherwise, test to see if the number is mobile. Only do this if certain that the patterns for
        // mobile and fixed line aren't the same.
        return if ((!metadata.sameMobileAndFixedLinePattern && isNumberMatchingDesc(nationalNumber, metadata.mobile))) {
            PhoneNumberType.MOBILE
        } else PhoneNumberType.UNKNOWN
    }

    /**
     * Returns the metadata for the given region code or `null` if the region code is invalid or
     * unknown.
     *
     * @throws MissingMetadataException if the region code is valid, but metadata cannot be found.
     */
    fun getMetadataForRegion(regionCode: String?): PhoneMetadata? {
        if (!isValidRegionCode(regionCode)) {
            return null
        }
        val phoneMetadata = metadataSource.getMetadataForRegion(regionCode)
        ensureMetadataIsNonNull(phoneMetadata, "Missing metadata for region code $regionCode")
        return phoneMetadata
    }

    /**
     * Returns the metadata for the given country calling code or `null` if the country calling
     * code is invalid or unknown.
     *
     * @throws MissingMetadataException if the country calling code is valid, but metadata cannot be
     * found.
     */
    fun getMetadataForNonGeographicalRegion(countryCallingCode: Int): PhoneMetadata? {
        if (!countryCodesForNonGeographicalRegion.contains(countryCallingCode)) {
            return null
        }
        val phoneMetadata = metadataSource.getMetadataForNonGeographicalRegion(
            countryCallingCode
        )
        ensureMetadataIsNonNull(
            phoneMetadata, "Missing metadata for country code $countryCallingCode"
        )
        return phoneMetadata
    }

    fun isNumberMatchingDesc(nationalNumber: String, numberDesc: PhoneNumberDesc?): Boolean {
        // Check if any possible number lengths are present; if so, we use them to avoid checking the
        // validation pattern if they don't match. If they are absent, this means they match the general
        // description, which we have already checked before checking a specific number type.
        val actualLength = nationalNumber.length
        val possibleLengths = numberDesc!!.possibleLengthList
        return if (possibleLengths.size > 0 && !possibleLengths.contains(actualLength)) {
            false
        } else matcherApi.matchNationalNumber(
            nationalNumber, (numberDesc), false
        )
    }

    /**
     * Tests whether a phone number matches a valid pattern. Note this doesn't verify the number
     * is actually in use, which is impossible to tell by just looking at a number itself. It only
     * verifies whether the parsed, canonicalised number is valid: not whether a particular series of
     * digits entered by the user is diallable from the region provided when parsing. For example, the
     * number +41 (0) 78 927 2696 can be parsed into a number with country code "41" and national
     * significant number "789272696". This is valid, while the original string is not diallable.
     *
     * @param number  the phone number that we want to validate
     * @return  a boolean that indicates whether the number is of a valid pattern
     */
    fun isValidNumber(number: PhoneNumber): Boolean {
        val regionCode = getRegionCodeForNumber(number)
        return isValidNumberForRegion(number, regionCode)
    }

    /**
     * Tests whether a phone number is valid for a certain region. Note this doesn't verify the number
     * is actually in use, which is impossible to tell by just looking at a number itself. If the
     * country calling code is not the same as the country calling code for the region, this
     * immediately exits with false. After this, the specific number pattern rules for the region are
     * examined. This is useful for determining for example whether a particular number is valid for
     * Canada, rather than just a valid NANPA number.
     * Warning: In most cases, you want to use [.isValidNumber] instead. For example, this
     * method will mark numbers from British Crown dependencies such as the Isle of Man as invalid for
     * the region "GB" (United Kingdom), since it has its own region code, "IM", which may be
     * undesirable.
     *
     * @param number  the phone number that we want to validate
     * @param regionCode  the region that we want to validate the phone number for
     * @return  a boolean that indicates whether the number is of a valid pattern
     */
    fun isValidNumberForRegion(number: PhoneNumber, regionCode: String?): Boolean {
        val countryCode = number.countryCode
        val metadata = getMetadataForRegionOrCallingCode(countryCode, regionCode)
        if (metadata == null || (REGION_CODE_FOR_NON_GEO_ENTITY != regionCode && countryCode != getCountryCodeForValidRegion(
                regionCode
            ))
        ) {
            // Either the region code was invalid, or the country calling code for this number does not
            // match that of the region code.
            return false
        }
        val nationalSignificantNumber = getNationalSignificantNumber(number)
        return getNumberTypeHelper(nationalSignificantNumber, metadata) != PhoneNumberType.UNKNOWN
    }

    /**
     * Returns the region where a phone number is from. This could be used for geocoding at the region
     * level. Only guarantees correct results for valid, full numbers (not short-codes, or invalid
     * numbers).
     *
     * @param number  the phone number whose origin we want to know
     * @return  the region where the phone number is from, or null if no region matches this calling
     * code
     */
    fun getRegionCodeForNumber(number: PhoneNumber): String? {
        val countryCode = number.countryCode
        val regions = countryCallingCodeToRegionCodeMap[countryCode]
        if (regions == null) {
            logger.i("Missing/invalid country_code ($countryCode)")
            return null
        }
        return if (regions.size == 1) {
            regions[0]
        } else {
            getRegionCodeForNumberFromRegionList(number, regions)
        }
    }

    private fun getRegionCodeForNumberFromRegionList(
        number: PhoneNumber, regionCodes: List<String>
    ): String? {
        val nationalNumber = getNationalSignificantNumber(number)
        for (regionCode in regionCodes) {
            // If leadingDigits is present, use this. Otherwise, do full validation.
            // Metadata cannot be null because the region codes come from the country calling code map.
            val metadata = getMetadataForRegion(regionCode)
            if (metadata!!.hasLeadingDigits()) {
                if (regexCache.getRegexForPattern(metadata.leadingDigits).matchesAt(nationalNumber, 0)) {
                    return regionCode
                }
            } else if (getNumberTypeHelper(nationalNumber, metadata) != PhoneNumberType.UNKNOWN) {
                return regionCode
            }
        }
        return null
    }

    /**
     * Returns the region code that matches the specific country calling code. In the case of no
     * region code being found, ZZ will be returned. In the case of multiple regions, the one
     * designated in the metadata as the "main" region for this calling code will be returned. If the
     * countryCallingCode entered is valid but doesn't match a specific region (such as in the case of
     * non-geographical calling codes like 800) the value "001" will be returned (corresponding to
     * the value for World in the UN M.49 schema).
     */
    fun getRegionCodeForCountryCode(countryCallingCode: Int): String {
        val regionCodes = countryCallingCodeToRegionCodeMap[countryCallingCode]
        return regionCodes?.get(0) ?: UNKNOWN_REGION
    }

    /**
     * Returns a list with the region codes that match the specific country calling code. For
     * non-geographical country calling codes, the region code 001 is returned. Also, in the case
     * of no region code being found, an empty list is returned.
     */
    fun getRegionCodesForCountryCode(countryCallingCode: Int): List<String> {
        val regionCodes = countryCallingCodeToRegionCodeMap[countryCallingCode]
        return regionCodes ?: ArrayList(0)
    }

    /**
     * Returns the country calling code for a specific region. For example, this would be 1 for the
     * United States, and 64 for New Zealand.
     *
     * @param regionCode  the region that we want to get the country calling code for
     * @return  the country calling code for the region denoted by regionCode
     */
    fun getCountryCodeForRegion(regionCode: String?): Int {
        if (!isValidRegionCode(regionCode)) {
            logger.w(
                "Invalid or missing region code (" + (regionCode ?: "null") + ") provided."
            )
            return 0
        }
        return getCountryCodeForValidRegion(regionCode)
    }

    /**
     * Returns the country calling code for a specific region. For example, this would be 1 for the
     * United States, and 64 for New Zealand. Assumes the region is already valid.
     *
     * @param regionCode  the region that we want to get the country calling code for
     * @return  the country calling code for the region denoted by regionCode
     * @throws IllegalArgumentException if the region is invalid
     */
    private fun getCountryCodeForValidRegion(regionCode: String?): Int {
        val metadata =
            getMetadataForRegion(regionCode) ?: throw IllegalArgumentException("Invalid region code: $regionCode")
        return metadata.countryCode
    }

    /**
     * Returns the national dialling prefix for a specific region. For example, this would be 1 for
     * the United States, and 0 for New Zealand. Set stripNonDigits to true to strip symbols like "~"
     * (which indicates a wait for a dialling tone) from the prefix returned. If no national prefix is
     * present, we return null.
     *
     *
     * Warning: Do not use this method for do-your-own formatting - for some regions, the
     * national dialling prefix is used only for certain types of numbers. Use the library's
     * formatting functions to prefix the national prefix when required.
     *
     * @param regionCode  the region that we want to get the dialling prefix for
     * @param stripNonDigits  true to strip non-digits from the national dialling prefix
     * @return  the dialling prefix for the region denoted by regionCode
     */
    fun getNddPrefixForRegion(regionCode: String?, stripNonDigits: Boolean): String? {
        val metadata = getMetadataForRegion(regionCode)
        if (metadata == null) {
            logger.w(
                "Invalid or missing region code (" + (regionCode ?: "null") + ") provided."
            )
            return null
        }
        var nationalPrefix = metadata.nationalPrefix
        // If no national prefix was found, we return null.
        if (nationalPrefix.length == 0) {
            return null
        }
        if (stripNonDigits) {
            // Note: if any other non-numeric symbols are ever used in national prefixes, these would have
            // to be removed here as well.
            nationalPrefix = nationalPrefix.replace("~", "")
        }
        return nationalPrefix
    }

    /**
     * Checks if this is a region under the North American Numbering Plan Administration (NANPA).
     *
     * @return  true if regionCode is one of the regions under NANPA
     */
    fun isNANPACountry(regionCode: String?): Boolean {
        return nanpaRegions.contains(regionCode)
    }

    /**
     * Checks if the number is a valid vanity (alpha) number such as 800 MICROSOFT. A valid vanity
     * number will start with at least 3 digits and will have three or more alpha characters. This
     * does not do region-specific checks - to work out if this number is actually valid for a region,
     * it should be parsed and methods such as [.isPossibleNumberWithReason] and
     * [.isValidNumber] should be used.
     *
     * @param number  the number that needs to be checked
     * @return  true if the number is a valid vanity number
     */
    fun isAlphaNumber(number: CharSequence): Boolean {
        if (!isViablePhoneNumber(number)) {
            // Number is too short, or doesn't match the basic phone number pattern.
            return false
        }
        val strippedNumber = InplaceStringBuilder(number)
        maybeStripExtension(strippedNumber)
        return VALID_ALPHA_PHONE_PATTERN.matches(strippedNumber)
    }

    /**
     * Convenience wrapper around [.isPossibleNumberWithReason]. Instead of returning the reason
     * for failure, this method returns true if the number is either a possible fully-qualified number
     * (containing the area code and country code), or if the number could be a possible local number
     * (with a country code, but missing an area code). Local numbers are considered possible if they
     * could be possibly dialled in this format: if the area code is needed for a call to connect, the
     * number is not considered possible without it.
     *
     * @param number  the number that needs to be checked
     * @return  true if the number is possible
     */
    fun isPossibleNumber(number: PhoneNumber): Boolean {
        val result = isPossibleNumberWithReason(number)
        return (result == ValidationResult.IS_POSSIBLE || result == ValidationResult.IS_POSSIBLE_LOCAL_ONLY)
    }

    /**
     * Convenience wrapper around [.isPossibleNumberForTypeWithReason]. Instead of returning the
     * reason for failure, this method returns true if the number is either a possible fully-qualified
     * number (containing the area code and country code), or if the number could be a possible local
     * number (with a country code, but missing an area code). Local numbers are considered possible
     * if they could be possibly dialled in this format: if the area code is needed for a call to
     * connect, the number is not considered possible without it.
     *
     * @param number  the number that needs to be checked
     * @param type  the type we are interested in
     * @return  true if the number is possible for this particular type
     */
    fun isPossibleNumberForType(number: PhoneNumber, type: PhoneNumberType): Boolean {
        val result = isPossibleNumberForTypeWithReason(number, type)
        return (result == ValidationResult.IS_POSSIBLE || result == ValidationResult.IS_POSSIBLE_LOCAL_ONLY)
    }
    /**
     * Helper method to check a number against possible lengths for this number type, and determine
     * whether it matches, or is too short or too long.
     */
    /**
     * Helper method to check a number against possible lengths for this region, based on the metadata
     * being passed in, and determine whether it matches, or is too short or too long.
     */
    private fun testNumberLength(
        number: CharSequence, metadata: PhoneMetadata?, type: PhoneNumberType = PhoneNumberType.UNKNOWN
    ): ValidationResult {
        val descForType = getNumberDescByType(metadata, type)
        // There should always be "possibleLengths" set for every element. This is declared in the XML
        // schema which is verified by PhoneNumberMetadataSchemaTest.
        // For size efficiency, where a sub-description (e.g. fixed-line) has the same possibleLengths
        // as the parent, this is missing, so we fall back to the general desc (where no numbers of the
        // type exist at all, there is one possible length (-1) which is guaranteed not to match the
        // length of any real phone number).
        val possibleLengths =
            (if (descForType!!.possibleLengthList.isEmpty()) metadata!!.generalDesc!!.possibleLengthList else descForType.possibleLengthList).toMutableList()
        var localLengths: List<Int?> = descForType.possibleLengthLocalOnlyList
        if (type == PhoneNumberType.FIXED_LINE_OR_MOBILE) {
            if (!descHasPossibleNumberData(getNumberDescByType(metadata, PhoneNumberType.FIXED_LINE))) {
                // The rare case has been encountered where no fixedLine data is available (true for some
                // non-geographical entities), so we just check mobile.
                return testNumberLength(number, metadata, PhoneNumberType.MOBILE)
            } else {
                val mobileDesc = getNumberDescByType(metadata, PhoneNumberType.MOBILE)
                if (descHasPossibleNumberData(mobileDesc)) {
                    // Merge the mobile data in if there was any.
                    // Note that when adding the possible lengths from mobile, we have to again check they
                    // aren't empty since if they are this indicates they are the same as the general desc and
                    // should be obtained from there.
                    possibleLengths.addAll(if (mobileDesc!!.possibleLengthCount == 0) metadata!!.generalDesc!!.possibleLengthList else mobileDesc.possibleLengthList)
                    // The current list is sorted; we need to merge in the new list and re-sort (duplicates
                    // are okay). Sorting isn't so expensive because the lists are very small.
                    possibleLengths.sort()
                    if (localLengths.isEmpty()) {
                        localLengths = mobileDesc.possibleLengthLocalOnlyList
                    } else {
                        localLengths = ArrayList(localLengths)
                        localLengths.addAll(mobileDesc.possibleLengthLocalOnlyList)
                        possibleLengths.sort()
                    }
                }
            }
        }

        // If the type is not supported at all (indicated by the possible lengths containing -1 at this
        // point) we return invalid length.
        if (possibleLengths[0] == -1) {
            return ValidationResult.INVALID_LENGTH
        }
        val actualLength = number.length
        // This is safe because there is never an overlap beween the possible lengths and the local-only
        // lengths; this is checked at build time.
        if (localLengths.contains(actualLength)) {
            return ValidationResult.IS_POSSIBLE_LOCAL_ONLY
        }
        val minimumLength = possibleLengths[0]
        if (minimumLength == actualLength) {
            return ValidationResult.IS_POSSIBLE
        } else if (minimumLength > actualLength) {
            return ValidationResult.TOO_SHORT
        } else if (possibleLengths[possibleLengths.size - 1] < actualLength) {
            return ValidationResult.TOO_LONG
        }
        // We skip the first element; we've already checked it.
        return if (possibleLengths.subList(1, possibleLengths.size)
                .contains(actualLength)
        ) ValidationResult.IS_POSSIBLE else ValidationResult.INVALID_LENGTH
    }

    /**
     * Check whether a phone number is a possible number. It provides a more lenient check than
     * [.isValidNumber] in the following sense:
     *
     *  1.  It only checks the length of phone numbers. In particular, it doesn't check starting
     * digits of the number.
     *  1.  It doesn't attempt to figure out the type of the number, but uses general rules which
     * applies to all types of phone numbers in a region. Therefore, it is much faster than
     * isValidNumber.
     *  1.  For some numbers (particularly fixed-line), many regions have the concept of area code,
     * which together with subscriber number constitute the national significant number. It is
     * sometimes okay to dial only the subscriber number when dialing in the same area. This
     * function will return IS_POSSIBLE_LOCAL_ONLY if the subscriber-number-only version is
     * passed in. On the other hand, because isValidNumber validates using information on both
     * starting digits (for fixed line numbers, that would most likely be area codes) and
     * length (obviously includes the length of area codes for fixed line numbers), it will
     * return false for the subscriber-number-only version.
     *
     * @param number  the number that needs to be checked
     * @return  a ValidationResult object which indicates whether the number is possible
     */
    fun isPossibleNumberWithReason(number: PhoneNumber): ValidationResult {
        return isPossibleNumberForTypeWithReason(number, PhoneNumberType.UNKNOWN)
    }

    /**
     * Check whether a phone number is a possible number of a particular type. For types that don't
     * exist in a particular region, this will return a result that isn't so useful; it is recommended
     * that you use [.getSupportedTypesForRegion] or [.getSupportedTypesForNonGeoEntity]
     * respectively before calling this method to determine whether you should call it for this number
     * at all.
     *
     * This provides a more lenient check than [.isValidNumber] in the following sense:
     *
     *
     *  1.  It only checks the length of phone numbers. In particular, it doesn't check starting
     * digits of the number.
     *  1.  For some numbers (particularly fixed-line), many regions have the concept of area code,
     * which together with subscriber number constitute the national significant number. It is
     * sometimes okay to dial only the subscriber number when dialing in the same area. This
     * function will return IS_POSSIBLE_LOCAL_ONLY if the subscriber-number-only version is
     * passed in. On the other hand, because isValidNumber validates using information on both
     * starting digits (for fixed line numbers, that would most likely be area codes) and
     * length (obviously includes the length of area codes for fixed line numbers), it will
     * return false for the subscriber-number-only version.
     *
     *
     * @param number  the number that needs to be checked
     * @param type  the type we are interested in
     * @return  a ValidationResult object which indicates whether the number is possible
     */
    fun isPossibleNumberForTypeWithReason(
        number: PhoneNumber, type: PhoneNumberType
    ): ValidationResult {
        val nationalNumber = getNationalSignificantNumber(number)
        val countryCode = number.countryCode
        // Note: For regions that share a country calling code, like NANPA numbers, we just use the
        // rules from the default region (US in this case) since the getRegionCodeForNumber will not
        // work if the number is possible but not valid. There is in fact one country calling code (290)
        // where the possible number pattern differs between various regions (Saint Helena and Tristan
        // da Cuñha), but this is handled by putting all possible lengths for any country with this
        // country calling code in the metadata for the default region in this case.
        if (!hasValidCountryCallingCode(countryCode)) {
            return ValidationResult.INVALID_COUNTRY_CODE
        }
        val regionCode = getRegionCodeForCountryCode(countryCode)
        // Metadata cannot be null because the country calling code is valid.
        val metadata = getMetadataForRegionOrCallingCode(countryCode, regionCode)
        return testNumberLength(nationalNumber, metadata, type)
    }

    /**
     * Check whether a phone number is a possible number given a number in the form of a string, and
     * the region where the number could be dialed from. It provides a more lenient check than
     * [.isValidNumber]. See [.isPossibleNumber] for details.
     *
     *
     * This method first parses the number, then invokes [.isPossibleNumber]
     * with the resultant PhoneNumber object.
     *
     * @param number  the number that needs to be checked
     * @param regionDialingFrom  the region that we are expecting the number to be dialed from.
     * Note this is different from the region where the number belongs.  For example, the number
     * +1 650 253 0000 is a number that belongs to US. When written in this form, it can be
     * dialed from any region. When it is written as 00 1 650 253 0000, it can be dialed from any
     * region which uses an international dialling prefix of 00. When it is written as
     * 650 253 0000, it can only be dialed from within the US, and when written as 253 0000, it
     * can only be dialed from within a smaller area in the US (Mountain View, CA, to be more
     * specific).
     * @return  true if the number is possible
     */
    fun isPossibleNumber(number: CharSequence?, regionDialingFrom: String?): Boolean {
        return try {
            isPossibleNumber(parse(number, regionDialingFrom))
        } catch (e: NumberParseException) {
            false
        }
    }

    /**
     * Attempts to extract a valid number from a phone number that is too long to be valid, and resets
     * the PhoneNumber object passed in to that valid version. If no valid number could be extracted,
     * the PhoneNumber object passed in will not be modified.
     * @param number  a PhoneNumber object which contains a number that is too long to be valid
     * @return  true if a valid phone number can be successfully extracted
     */
    fun truncateTooLongNumber(number: PhoneNumber): Boolean {
        if (isValidNumber(number)) {
            return true
        }
        val numberCopy = PhoneNumber()
        numberCopy.mergeFrom(number)
        var nationalNumber = number.nationalNumber
        do {
            nationalNumber /= 10
            numberCopy.setNationalNumber(nationalNumber)
            if (isPossibleNumberWithReason(numberCopy) == ValidationResult.TOO_SHORT || nationalNumber == 0L) {
                return false
            }
        } while (!isValidNumber(numberCopy))
        number.setNationalNumber(nationalNumber)
        return true
    }

    /**
     * Gets an [io.michaelrocks.libphonenumber.android.AsYouTypeFormatter] for the specific region.
     *
     * @param regionCode  the region where the phone number is being entered
     * @return  an [io.michaelrocks.libphonenumber.android.AsYouTypeFormatter] object, which can be used
     * to format phone numbers in the specific region "as you type"
     */
    fun getAsYouTypeFormatter(regionCode: String): AsYouTypeFormatter {
        return AsYouTypeFormatter(this, regionCode)
    }

    // Extracts country calling code from fullNumber, returns it and places the remaining number in
    // nationalNumber. It assumes that the leading plus sign or IDD has already been removed. Returns
    // 0 if fullNumber doesn't start with a valid country calling code, and leaves nationalNumber
    // unmodified.
    fun extractCountryCode(fullNumber: InplaceStringBuilder, nationalNumber: InplaceStringBuilder): Int {
        if (fullNumber.isEmpty() || fullNumber[0] == '0') {
            // Country codes do not begin with a '0'.
            return 0
        }
        var potentialCountryCode: Int
        val numberLength = fullNumber.length
        var i = 1
        while (i <= MAX_LENGTH_COUNTRY_CODE && i <= numberLength) {
            potentialCountryCode = fullNumber.substring(0, i).toInt()
            if (countryCallingCodeToRegionCodeMap.containsKey(potentialCountryCode)) {
                nationalNumber.append(fullNumber.substring(i))
                return potentialCountryCode
            }
            i++
        }
        return 0
    }

    /**
     * Tries to extract a country calling code from a number. This method will return zero if no
     * country calling code is considered to be present. Country calling codes are extracted in the
     * following ways:
     *
     *  *  by stripping the international dialing prefix of the region the person is dialing from,
     * if this is present in the number, and looking at the next digits
     *  *  by stripping the '+' sign if present and then looking at the next digits
     *  *  by comparing the start of the number and the country calling code of the default region.
     * If the number is not considered possible for the numbering plan of the default region
     * initially, but starts with the country calling code of this region, validation will be
     * reattempted after stripping this country calling code. If this number is considered a
     * possible number, then the first digits will be considered the country calling code and
     * removed as such.
     *
     * It will throw a NumberParseException if the number starts with a '+' but the country calling
     * code supplied after this does not match that of any known region.
     *
     * @param number  non-normalized telephone number that we wish to extract a country calling
     * code from - may begin with '+'
     * @param defaultRegionMetadata  metadata about the region this number may be from
     * @param nationalNumber  a string buffer to store the national significant number in, in the case
     * that a country calling code was extracted. The number is appended to any existing contents.
     * If no country calling code was extracted, this will be left unchanged.
     * @param keepRawInput  true if the country_code_source and preferred_carrier_code fields of
     * phoneNumber should be populated.
     * @param phoneNumber  the PhoneNumber object where the country_code and country_code_source need
     * to be populated. Note the country_code is always populated, whereas country_code_source is
     * only populated when keepCountryCodeSource is true.
     * @return  the country calling code extracted or 0 if none could be extracted
     */
    // @VisibleForTesting
    @Throws(NumberParseException::class)
    fun maybeExtractCountryCode(
        number: CharSequence,
        defaultRegionMetadata: PhoneMetadata?,
        nationalNumber: InplaceStringBuilder,
        keepRawInput: Boolean,
        phoneNumber: PhoneNumber
    ): Int {
        if (number.length == 0) {
            return 0
        }
        val fullNumber = InplaceStringBuilder(number)
        // Set the default prefix to be something that will never match.
        var possibleCountryIddPrefix: String? = "NonMatch"
        if (defaultRegionMetadata != null) {
            possibleCountryIddPrefix = defaultRegionMetadata.internationalPrefix
        }
        val countryCodeSource = maybeStripInternationalPrefixAndNormalize(fullNumber, possibleCountryIddPrefix)
        if (keepRawInput) {
            phoneNumber.setCountryCodeSource(countryCodeSource)
        }
        if (countryCodeSource !== CountryCodeSource.FROM_DEFAULT_COUNTRY) {
            if (fullNumber.length <= MIN_LENGTH_FOR_NSN) {
                throw NumberParseException(
                    NumberParseException.ErrorType.TOO_SHORT_AFTER_IDD,
                    "Phone number had an IDD, but after this was not " + "long enough to be a viable phone number."
                )
            }
            val potentialCountryCode = extractCountryCode(fullNumber, nationalNumber)
            if (potentialCountryCode != 0) {
                phoneNumber.setCountryCode(potentialCountryCode)
                return potentialCountryCode
            }

            // If this fails, they must be using a strange country calling code that we don't recognize,
            // or that doesn't exist.
            throw NumberParseException(
                NumberParseException.ErrorType.INVALID_COUNTRY_CODE, "Country calling code supplied was not recognised."
            )
        } else if (defaultRegionMetadata != null) {
            // Check to see if the number starts with the country calling code for the default region. If
            // so, we remove the country calling code, and do some checks on the validity of the number
            // before and after.
            val defaultCountryCode = defaultRegionMetadata.countryCode
            val defaultCountryCodeString = defaultCountryCode.toString()
            val normalizedNumber = fullNumber.toString()
            if (normalizedNumber.startsWith(defaultCountryCodeString)) {
                val potentialNationalNumber =
                    InplaceStringBuilder(normalizedNumber.substring(defaultCountryCodeString.length))
                val generalDesc = defaultRegionMetadata.generalDesc
                maybeStripNationalPrefixAndCarrierCode(
                    potentialNationalNumber, defaultRegionMetadata, null /* Don't need the carrier code */
                )
                // If the number was not valid before but is valid now, or if it was too long before, we
                // consider the number with the country calling code stripped to be a better result and
                // keep that instead.
                if ((!matcherApi.matchNationalNumber(
                        fullNumber, generalDesc!!, false
                    ) && matcherApi.matchNationalNumber(
                        potentialNationalNumber, generalDesc, false
                    )) || testNumberLength(fullNumber, defaultRegionMetadata) == ValidationResult.TOO_LONG
                ) {
                    nationalNumber.append(potentialNationalNumber)
                    if (keepRawInput) {
                        phoneNumber.setCountryCodeSource(CountryCodeSource.FROM_NUMBER_WITHOUT_PLUS_SIGN)
                    }
                    phoneNumber.setCountryCode(defaultCountryCode)
                    return defaultCountryCode
                }
            }
        }
        // No country calling code present.
        phoneNumber.setCountryCode(0)
        return 0
    }

    /**
     * Strips the IDD from the start of the number if present. Helper function used by
     * maybeStripInternationalPrefixAndNormalize.
     */
    private fun parsePrefixAsIdd(iddPattern: Regex, number: InplaceStringBuilder): Boolean {
        val m = iddPattern.matchAt(number, 0)
        if (m != null) {
            val matchEnd = m.range.last + 1
            // Only strip this if the first digit after the match is not a 0, since country calling codes
            // cannot begin with 0.
            val digitMatcher = CAPTURING_DIGIT_PATTERN.find(number.substring(matchEnd))
            if (digitMatcher != null) {
                val normalizedGroup = normalizeDigitsOnly(digitMatcher.groupValues[1])
                if (normalizedGroup == "0") {
                    return false
                }
            }
            number.removeRange(0, matchEnd)
            return true
        }
        return false
    }

    /**
     * Strips any international prefix (such as +, 00, 011) present in the number provided, normalizes
     * the resulting number, and indicates if an international prefix was present.
     *
     * @param number  the non-normalized telephone number that we wish to strip any international
     * dialing prefix from
     * @param possibleIddPrefix  the international direct dialing prefix from the region we
     * think this number may be dialed in
     * @return  the corresponding CountryCodeSource if an international dialing prefix could be
     * removed from the number, otherwise CountryCodeSource.FROM_DEFAULT_COUNTRY if the number did
     * not seem to be in international format
     */
    // @VisibleForTesting
    fun maybeStripInternationalPrefixAndNormalize(
        number: InplaceStringBuilder, possibleIddPrefix: String?
    ): CountryCodeSource {
        if (number.length == 0) {
            return CountryCodeSource.FROM_DEFAULT_COUNTRY
        }
        // Check to see if the number begins with one or more plus signs.
        val m = PLUS_CHARS_PATTERN.matchAt(number, 0)
        if (m != null) {
            number.removeRange(0, m.range.last + 1)
            // Can now normalize the rest of the number since we've consumed the "+" sign at the start.
            normalize(number)
            return CountryCodeSource.FROM_NUMBER_WITH_PLUS_SIGN
        }
        // Attempt to parse the first digits as an international prefix.
        val iddPattern = regexCache.getRegexForPattern(possibleIddPrefix!!)
        normalize(number)
        return if (parsePrefixAsIdd(
                iddPattern, number
            )
        ) CountryCodeSource.FROM_NUMBER_WITH_IDD else CountryCodeSource.FROM_DEFAULT_COUNTRY
    }

    /**
     * Strips any national prefix (such as 0, 1) present in the number provided.
     *
     * @param number  the normalized telephone number that we wish to strip any national
     * dialing prefix from
     * @param metadata  the metadata for the region that we think this number is from
     * @param carrierCode  a place to insert the carrier code if one is extracted
     * @return true if a national prefix or carrier code (or both) could be extracted
     */
    // @VisibleForTesting
    fun maybeStripNationalPrefixAndCarrierCode(
        number: InplaceStringBuilder, metadata: PhoneMetadata, carrierCode: InplaceStringBuilder?
    ): Boolean {
        val numberLength = number.length
        val possibleNationalPrefix = metadata.nationalPrefixForParsing
        if (numberLength == 0 || possibleNationalPrefix.length == 0) {
            // Early return for numbers of zero length.
            return false
        }
        // Attempt to parse the first digits as a national prefix.
        val prefixMatchRegex = regexCache.getRegexForPattern(possibleNationalPrefix)
        val prefixMatchResult = prefixMatchRegex.matchAt(number, 0)
        if (prefixMatchResult != null) {
            val generalDesc = metadata.generalDesc
            // Check if the original number is viable.
            val isViableOriginalNumber = matcherApi.matchNationalNumber(number, generalDesc!!, false)
            // prefixMatcher.group(numOfGroups) == null implies nothing was captured by the capturing
            // groups in possibleNationalPrefix; therefore, no transformation is necessary, and we just
            // remove the national prefix.
            // prefixMatchResult.groups has size of groupCount + 1 where groupCount is the count of groups in the regular expression
            val numOfGroups = prefixMatchResult.groups.size - 1
            val transformRule = metadata.nationalPrefixTransformRule
            return if (transformRule.isEmpty() || prefixMatchResult.groups[numOfGroups] == null) {
                // If the original number was viable, and the resultant number is not, we return.
                if (isViableOriginalNumber && !matcherApi.matchNationalNumber(
                        number.substring(prefixMatchResult.range.last + 1), generalDesc, false
                    )
                ) {
                    return false
                }
                if (carrierCode != null && numOfGroups > 0 && prefixMatchResult.groups[numOfGroups] != null) {
                    carrierCode.append(prefixMatchResult.groups[1]?.value)
                }
                number.removeRange(0, prefixMatchResult.range.last + 1)
                true
            } else {
                // Check that the resultant number is still viable. If not, return. Check this by copying
                // the string buffer and making the transformation on the copy first.
                val transformedNumber = InplaceStringBuilder(number)
                transformedNumber.replaceRange(0, numberLength, number.replaceFirst(prefixMatchRegex, transformRule))
                if (isViableOriginalNumber && !matcherApi.matchNationalNumber(
                        transformedNumber.toString(), generalDesc, false
                    )
                ) {
                    return false
                }
                if (carrierCode != null && numOfGroups > 1) {
                    carrierCode.append(prefixMatchResult.groups[1]?.value)
                }
                number.replaceRange(0, number.length, transformedNumber.toString())
                true
            }
        }
        return false
    }

    /**
     * Strips any extension (as in, the part of the number dialled after the call is connected,
     * usually indicated with extn, ext, x or similar) from the end of the number, and returns it.
     *
     * @param number  the non-normalized telephone number that we wish to strip the extension from
     * @return  the phone extension
     */
    // @VisibleForTesting
    fun maybeStripExtension(number: InplaceStringBuilder): String {
        val m = EXTN_PATTERN.find(number)
        // If we find a potential extension, and the number preceding this is a viable number, we assume
        // it is an extension.
        if (m != null && isViablePhoneNumber(number.substring(0, m.range.first))) {
            // The numbers are captured into groups in the regular expression.
            var i = 1
            val length = m.groupValues.size
            while (i <= length) {
                if (m.groups[i] != null) {
                    // We go through the capturing groups until we find one that captured some digits. If none
                    // did, then we will return the empty string.
                    val extension = m.groups[i]
                    number.removeRange(m.range.first, number.length)
                    return extension.toString()
                }
                i++
            }
        }
        return ""
    }

    /**
     * Checks to see that the region code used is valid, or if it is not valid, that the number to
     * parse starts with a + symbol so that we can attempt to infer the region from the number.
     * Returns false if it cannot use the region provided and the region cannot be inferred.
     */
    private fun checkRegionForParsing(numberToParse: CharSequence?, defaultRegion: String?): Boolean {
        if (!isValidRegionCode(defaultRegion)) {
            // If the number is null or empty, we can't infer the region.
            if (numberToParse == null || numberToParse.length == 0 || !PLUS_CHARS_PATTERN.matchesAt(numberToParse, 0)) {
                return false
            }
        }
        return true
    }

    /**
     * Parses a string and returns it as a phone number in proto buffer format. The method is quite
     * lenient and looks for a number in the input text (raw input) and does not check whether the
     * string is definitely only a phone number. To do this, it ignores punctuation and white-space,
     * as well as any text before the number (e.g. a leading "Tel: ") and trims the non-number bits.
     * It will accept a number in any format (E164, national, international etc), assuming it can be
     * interpreted with the defaultRegion supplied. It also attempts to convert any alpha characters
     * into digits if it thinks this is a vanity number of the type "1800 MICROSOFT".
     *
     *
     *  This method will throw a [NumberParseException] if the
     * number is not considered to be a possible number. Note that validation of whether the number
     * is actually a valid number for a particular region is not performed. This can be done
     * separately with [.isValidNumber].
     *
     *
     *  Note this method canonicalizes the phone number such that different representations can be
     * easily compared, no matter what form it was originally entered in (e.g. national,
     * international). If you want to record context about the number being parsed, such as the raw
     * input that was entered, how the country code was derived etc. then call [ ][.parseAndKeepRawInput] instead.
     *
     * @param numberToParse  number that we are attempting to parse. This can contain formatting such
     * as +, ( and -, as well as a phone number extension. It can also be provided in RFC3966
     * format.
     * @param defaultRegion  region that we are expecting the number to be from. This is only used if
     * the number being parsed is not written in international format. The country_code for the
     * number in this case would be stored as that of the default region supplied. If the number
     * is guaranteed to start with a '+' followed by the country calling code, then RegionCode.ZZ
     * or null can be supplied.
     * @return  a phone number proto buffer filled with the parsed number
     * @throws NumberParseException  if the string is not considered to be a viable phone number (e.g.
     * too few or too many digits) or if no default region was supplied and the number is not in
     * international format (does not start with +)
     */
    @Throws(NumberParseException::class)
    fun parse(numberToParse: CharSequence?, defaultRegion: String?): PhoneNumber {
        val phoneNumber = PhoneNumber()
        parse(numberToParse, defaultRegion, phoneNumber)
        return phoneNumber
    }

    /**
     * Same as [.parse], but accepts mutable PhoneNumber as a
     * parameter to decrease object creation when invoked many times.
     */
    @Throws(NumberParseException::class)
    fun parse(numberToParse: CharSequence?, defaultRegion: String?, phoneNumber: PhoneNumber) {
        parseHelper(numberToParse, defaultRegion, false, true, phoneNumber)
    }

    /**
     * Parses a string and returns it in proto buffer format. This method differs from [.parse]
     * in that it always populates the raw_input field of the protocol buffer with numberToParse as
     * well as the country_code_source field.
     *
     * @param numberToParse  number that we are attempting to parse. This can contain formatting such
     * as +, ( and -, as well as a phone number extension.
     * @param defaultRegion  region that we are expecting the number to be from. This is only used if
     * the number being parsed is not written in international format. The country calling code
     * for the number in this case would be stored as that of the default region supplied.
     * @return  a phone number proto buffer filled with the parsed number
     * @throws NumberParseException  if the string is not considered to be a viable phone number or if
     * no default region was supplied
     */
    @Throws(NumberParseException::class)
    fun parseAndKeepRawInput(numberToParse: CharSequence?, defaultRegion: String?): PhoneNumber {
        val phoneNumber = PhoneNumber()
        parseAndKeepRawInput(numberToParse, defaultRegion, phoneNumber)
        return phoneNumber
    }

    /**
     * Same as[.parseAndKeepRawInput], but accepts a mutable
     * PhoneNumber as a parameter to decrease object creation when invoked many times.
     */
    @Throws(NumberParseException::class)
    fun parseAndKeepRawInput(
        numberToParse: CharSequence?, defaultRegion: String?, phoneNumber: PhoneNumber
    ) {
        parseHelper(numberToParse, defaultRegion, true, true, phoneNumber)
    }
    /**
     * Returns an iterable over all [PhoneNumberMatches][PhoneNumberMatch] in `text`.
     *
     * @param text  the text to search for phone numbers, null for no text
     * @param defaultRegion  region that we are expecting the number to be from. This is only used if
     * the number being parsed is not written in international format. The country_code for the
     * number in this case would be stored as that of the default region supplied. May be null if
     * only international numbers are expected.
     * @param leniency  the leniency to use when evaluating candidate phone numbers
     * @param maxTries  the maximum number of invalid numbers to try before giving up on the text.
     * This is to cover degenerate cases where the text has a lot of false positives in it. Must
     * be `>= 0`.
     */
    /**
     * Returns an iterable over all [PhoneNumberMatches][PhoneNumberMatch] in `text`. This
     * is a shortcut for [ getMatcher(text, defaultRegion, Leniency.VALID, Long.MAX_VALUE)][.findNumbers].
     *
     * @param text  the text to search for phone numbers, null for no text
     * @param defaultRegion  region that we are expecting the number to be from. This is only used if
     * the number being parsed is not written in international format. The country_code for the
     * number in this case would be stored as that of the default region supplied. May be null if
     * only international numbers are expected.
     */
    @JvmOverloads
    fun findNumbers(
        text: CharSequence?,
        defaultRegion: String?,
        leniency: Leniency? = Leniency.VALID,
        maxTries: Long = Long.MAX_VALUE
    ): Iterable<PhoneNumberMatch> {
        return object : Iterable<PhoneNumberMatch> {
            override fun iterator(): Iterator<PhoneNumberMatch> {
                return PhoneNumberMatcher(this@PhoneNumberUtil, text, defaultRegion, leniency, maxTries)
            }
        }
    }

    /**
     * Parses a string and fills up the phoneNumber. This method is the same as the public
     * parse() method, with the exception that it allows the default region to be null, for use by
     * isNumberMatch(). checkRegion should be set to false if it is permitted for the default region
     * to be null or unknown ("ZZ").
     *
     * Note if any new field is added to this method that should always be filled in, even when
     * keepRawInput is false, it should also be handled in the copyCoreFieldsOnly() method.
     */
    @Throws(NumberParseException::class)
    private fun parseHelper(
        numberToParse: CharSequence?,
        defaultRegion: String?,
        keepRawInput: Boolean,
        checkRegion: Boolean,
        phoneNumber: PhoneNumber
    ) {
        if (numberToParse == null) {
            throw NumberParseException(
                NumberParseException.ErrorType.NOT_A_NUMBER, "The phone number supplied was null."
            )
        } else if (numberToParse.length > MAX_INPUT_STRING_LENGTH) {
            throw NumberParseException(
                NumberParseException.ErrorType.TOO_LONG, "The string supplied was too long to parse."
            )
        }
        val nationalNumber = InplaceStringBuilder()
        val numberBeingParsed = numberToParse.toString()
        buildNationalNumberForParsing(numberBeingParsed, nationalNumber)
        if (!isViablePhoneNumber(nationalNumber)) {
            throw NumberParseException(
                NumberParseException.ErrorType.NOT_A_NUMBER, "The string supplied did not seem to be a phone number."
            )
        }

        // Check the region supplied is valid, or that the extracted number starts with some sort of +
        // sign so the number's region can be determined.
        if (checkRegion && !checkRegionForParsing(nationalNumber, defaultRegion)) {
            throw NumberParseException(
                NumberParseException.ErrorType.INVALID_COUNTRY_CODE, "Missing or invalid default region."
            )
        }
        if (keepRawInput) {
            phoneNumber.setRawInput(numberBeingParsed)
        }
        // Attempt to parse extension first, since it doesn't require region-specific data and we want
        // to have the non-normalised number here.
        val extension = maybeStripExtension(nationalNumber)
        if (extension.length > 0) {
            phoneNumber.setExtension(extension)
        }
        var regionMetadata = getMetadataForRegion(defaultRegion)
        // Check to see if the number is given in international format so we know whether this number is
        // from the default region or not.
        var normalizedNationalNumber = InplaceStringBuilder()
        var countryCode = 0
        try {
            // TODO: This method should really just take in the string buffer that has already
            // been created, and just remove the prefix, rather than taking in a string and then
            // outputting a string buffer.
            countryCode = maybeExtractCountryCode(
                nationalNumber, regionMetadata, normalizedNationalNumber, keepRawInput, phoneNumber
            )
        } catch (e: NumberParseException) {
            val matchResult = PLUS_CHARS_PATTERN.find(nationalNumber, 0)
            if (e.errorType === NumberParseException.ErrorType.INVALID_COUNTRY_CODE && matchResult != null) {
                // Strip the plus-char, and try again.
                countryCode = maybeExtractCountryCode(
                    nationalNumber.substring(matchResult.range.last + 1),
                    regionMetadata,
                    normalizedNationalNumber,
                    keepRawInput,
                    phoneNumber
                )
                if (countryCode == 0) {
                    throw NumberParseException(
                        NumberParseException.ErrorType.INVALID_COUNTRY_CODE,
                        "Could not interpret numbers after plus-sign."
                    )
                }
            } else {
                throw NumberParseException(e.errorType, e.message)
            }
        }
        if (countryCode != 0) {
            val phoneNumberRegion = getRegionCodeForCountryCode(countryCode)
            if (phoneNumberRegion != defaultRegion) {
                // Metadata cannot be null because the country calling code is valid.
                regionMetadata = getMetadataForRegionOrCallingCode(countryCode, phoneNumberRegion)
            }
        } else {
            // If no extracted country calling code, use the region supplied instead. The national number
            // is just the normalized version of the number we were given to parse.
            normalizedNationalNumber.append(normalize(nationalNumber))
            if (defaultRegion != null) {
                countryCode = regionMetadata!!.countryCode
                phoneNumber.setCountryCode(countryCode)
            } else if (keepRawInput) {
                phoneNumber.clearCountryCodeSource()
            }
        }
        if (normalizedNationalNumber.length < MIN_LENGTH_FOR_NSN) {
            throw NumberParseException(
                NumberParseException.ErrorType.TOO_SHORT_NSN, "The string supplied is too short to be a phone number."
            )
        }
        if (regionMetadata != null) {
            val carrierCode = InplaceStringBuilder()
            val potentialNationalNumber = InplaceStringBuilder(normalizedNationalNumber)
            maybeStripNationalPrefixAndCarrierCode(potentialNationalNumber, regionMetadata, carrierCode)
            // We require that the NSN remaining after stripping the national prefix and carrier code be
            // long enough to be a possible length for the region. Otherwise, we don't do the stripping,
            // since the original number could be a valid short number.
            val validationResult = testNumberLength(potentialNationalNumber, regionMetadata)
            if (validationResult != ValidationResult.TOO_SHORT && validationResult != ValidationResult.IS_POSSIBLE_LOCAL_ONLY && validationResult != ValidationResult.INVALID_LENGTH) {
                normalizedNationalNumber = potentialNationalNumber
                if (keepRawInput && carrierCode.length > 0) {
                    phoneNumber.setPreferredDomesticCarrierCode(carrierCode.toString())
                }
            }
        }
        val lengthOfNationalNumber = normalizedNationalNumber.length
        if (lengthOfNationalNumber < MIN_LENGTH_FOR_NSN) {
            throw NumberParseException(
                NumberParseException.ErrorType.TOO_SHORT_NSN, "The string supplied is too short to be a phone number."
            )
        }
        if (lengthOfNationalNumber > MAX_LENGTH_FOR_NSN) {
            throw NumberParseException(
                NumberParseException.ErrorType.TOO_LONG, "The string supplied is too long to be a phone number."
            )
        }
        setItalianLeadingZerosForPhoneNumber(normalizedNationalNumber, phoneNumber)
        phoneNumber.setNationalNumber(normalizedNationalNumber.toString().toLong())
    }

    /**
     * Extracts the value of the phone-context parameter of numberToExtractFrom where the index of
     * ";phone-context=" is the parameter indexOfPhoneContext, following the syntax defined in
     * RFC3966.
     *
     * @return the extracted string (possibly empty), or null if no phone-context parameter is found.
     */
    private fun extractPhoneContext(numberToExtractFrom: String, indexOfPhoneContext: Int): String? {
        // If no phone-context parameter is present
        if (indexOfPhoneContext == -1) {
            return null
        }
        val phoneContextStart = indexOfPhoneContext + RFC3966_PHONE_CONTEXT.length
        // If phone-context parameter is empty
        if (phoneContextStart >= numberToExtractFrom.length) {
            return ""
        }
        val phoneContextEnd = numberToExtractFrom.indexOf(';', phoneContextStart)
        // If phone-context is not the last parameter
        return if (phoneContextEnd != -1) {
            numberToExtractFrom.substring(phoneContextStart, phoneContextEnd)
        } else {
            numberToExtractFrom.substring(phoneContextStart)
        }
    }

    /**
     * Returns whether the value of phoneContext follows the syntax defined in RFC3966.
     */
    private fun isPhoneContextValid(phoneContext: String?): Boolean {
        if (phoneContext == null) {
            return true
        }
        return if (phoneContext.length == 0) {
            false
        } else (RFC3966_GLOBAL_NUMBER_DIGITS_PATTERN.matches(phoneContext) || RFC3966_DOMAINNAME_PATTERN.matches(
            phoneContext
        ))

        // Does phone-context value match pattern of global-number-digits or domainname
    }

    /**
     * Converts numberToParse to a form that we can parse and write it to nationalNumber if it is
     * written in RFC3966; otherwise extract a possible number out of it and write to nationalNumber.
     */
    @Throws(NumberParseException::class)
    private fun buildNationalNumberForParsing(numberToParse: String, nationalNumber: InplaceStringBuilder) {
        val indexOfPhoneContext = numberToParse.indexOf(RFC3966_PHONE_CONTEXT)
        val phoneContext = extractPhoneContext(numberToParse, indexOfPhoneContext)
        if (!isPhoneContextValid(phoneContext)) {
            throw NumberParseException(
                NumberParseException.ErrorType.NOT_A_NUMBER, "The phone-context value is invalid."
            )
        }
        if (phoneContext != null) {
            // If the phone context contains a phone number prefix, we need to capture it, whereas domains
            // will be ignored.
            if (phoneContext[0] == PLUS_SIGN) {
                // Additional parameters might follow the phone context. If so, we will remove them here
                // because the parameters after phone context are not important for parsing the phone
                // number.
                nationalNumber.append(phoneContext)
            }

            // Now append everything between the "tel:" prefix and the phone-context. This should include
            // the national number, an optional extension or isdn-subaddress component. Note we also
            // handle the case when "tel:" is missing, as we have seen in some of the phone number inputs.
            // In that case, we append everything from the beginning.
            val indexOfRfc3966Prefix = numberToParse.indexOf(RFC3966_PREFIX)
            val indexOfNationalNumber =
                if (indexOfRfc3966Prefix >= 0) indexOfRfc3966Prefix + RFC3966_PREFIX.length else 0
            nationalNumber.append(numberToParse.substring(indexOfNationalNumber, indexOfPhoneContext))
        } else {
            // Extract a possible number from the string passed in (this strips leading characters that
            // could not be the start of a phone number.)
            nationalNumber.append(extractPossibleNumber(numberToParse))
        }

        // Delete the isdn-subaddress and everything after it if it is present. Note extension won't
        // appear at the same time with isdn-subaddress according to paragraph 5.3 of the RFC3966 spec,
        val indexOfIsdn = nationalNumber.indexOf(RFC3966_ISDN_SUBADDRESS)
        if (indexOfIsdn > 0) {
            nationalNumber.removeRange(indexOfIsdn, indexOfIsdn + nationalNumber.length)
        }
        // If both phone context and isdn-subaddress are absent but other parameters are present, the
        // parameters are left in nationalNumber. This is because we are concerned about deleting
        // content from a potential number string when there is no strong evidence that the number is
        // actually written in RFC3966.
    }

    /**
     * Takes two phone numbers and compares them for equality.
     *
     *
     * Returns EXACT_MATCH if the country_code, NSN, presence of a leading zero for Italian numbers
     * and any extension present are the same.
     * Returns NSN_MATCH if either or both has no region specified, and the NSNs and extensions are
     * the same.
     * Returns SHORT_NSN_MATCH if either or both has no region specified, or the region specified is
     * the same, and one NSN could be a shorter version of the other number. This includes the case
     * where one has an extension specified, and the other does not.
     * Returns NO_MATCH otherwise.
     * For example, the numbers +1 345 657 1234 and 657 1234 are a SHORT_NSN_MATCH.
     * The numbers +1 345 657 1234 and 345 657 are a NO_MATCH.
     *
     * @param firstNumberIn  first number to compare
     * @param secondNumberIn  second number to compare
     *
     * @return  NO_MATCH, SHORT_NSN_MATCH, NSN_MATCH or EXACT_MATCH depending on the level of equality
     * of the two numbers, described in the method definition.
     */
    fun isNumberMatch(firstNumberIn: PhoneNumber, secondNumberIn: PhoneNumber): MatchType {
        // We only care about the fields that uniquely define a number, so we copy these across
        // explicitly.
        val firstNumber = copyCoreFieldsOnly(firstNumberIn)
        val secondNumber = copyCoreFieldsOnly(secondNumberIn)
        // Early exit if both had extensions and these are different.
        if (firstNumber.hasExtension() && secondNumber.hasExtension() && firstNumber.extension != secondNumber.extension) {
            return MatchType.NO_MATCH
        }
        val firstNumberCountryCode = firstNumber.countryCode
        val secondNumberCountryCode = secondNumber.countryCode
        // Both had country_code specified.
        if (firstNumberCountryCode != 0 && secondNumberCountryCode != 0) {
            if (firstNumber.exactlySameAs(secondNumber)) {
                return MatchType.EXACT_MATCH
            } else if (firstNumberCountryCode == secondNumberCountryCode && isNationalNumberSuffixOfTheOther(
                    firstNumber, secondNumber
                )
            ) {
                // A SHORT_NSN_MATCH occurs if there is a difference because of the presence or absence of
                // an 'Italian leading zero', the presence or absence of an extension, or one NSN being a
                // shorter variant of the other.
                return MatchType.SHORT_NSN_MATCH
            }
            // This is not a match.
            return MatchType.NO_MATCH
        }
        // Checks cases where one or both country_code fields were not specified. To make equality
        // checks easier, we first set the country_code fields to be equal.
        firstNumber.setCountryCode(secondNumberCountryCode)
        // If all else was the same, then this is an NSN_MATCH.
        if (firstNumber.exactlySameAs(secondNumber)) {
            return MatchType.NSN_MATCH
        }
        return if (isNationalNumberSuffixOfTheOther(firstNumber, secondNumber)) {
            MatchType.SHORT_NSN_MATCH
        } else MatchType.NO_MATCH
    }

    // Returns true when one national number is the suffix of the other or both are the same.
    private fun isNationalNumberSuffixOfTheOther(
        firstNumber: PhoneNumber, secondNumber: PhoneNumber
    ): Boolean {
        val firstNumberNationalNumber = firstNumber.nationalNumber.toString()
        val secondNumberNationalNumber = secondNumber.nationalNumber.toString()
        // Note that endsWith returns true if the numbers are equal.
        return (firstNumberNationalNumber.endsWith(secondNumberNationalNumber) || secondNumberNationalNumber.endsWith(
            firstNumberNationalNumber
        ))
    }

    /**
     * Takes two phone numbers as strings and compares them for equality. This is a convenience
     * wrapper for [.isNumberMatch]. No default region is known.
     *
     * @param firstNumber  first number to compare. Can contain formatting, and can have country
     * calling code specified with + at the start.
     * @param secondNumber  second number to compare. Can contain formatting, and can have country
     * calling code specified with + at the start.
     * @return  NOT_A_NUMBER, NO_MATCH, SHORT_NSN_MATCH, NSN_MATCH, EXACT_MATCH. See
     * [.isNumberMatch] for more details.
     */
    fun isNumberMatch(firstNumber: CharSequence?, secondNumber: CharSequence?): MatchType {
        try {
            val firstNumberAsProto = parse(firstNumber, UNKNOWN_REGION)
            return isNumberMatch(firstNumberAsProto, secondNumber)
        } catch (e: NumberParseException) {
            if (e.errorType === NumberParseException.ErrorType.INVALID_COUNTRY_CODE) {
                try {
                    val secondNumberAsProto = parse(secondNumber, UNKNOWN_REGION)
                    return isNumberMatch(secondNumberAsProto, firstNumber)
                } catch (e2: NumberParseException) {
                    if (e2.errorType === NumberParseException.ErrorType.INVALID_COUNTRY_CODE) {
                        try {
                            val firstNumberProto = PhoneNumber()
                            val secondNumberProto = PhoneNumber()
                            parseHelper(firstNumber, null, false, false, firstNumberProto)
                            parseHelper(secondNumber, null, false, false, secondNumberProto)
                            return isNumberMatch(firstNumberProto, secondNumberProto)
                        } catch (e3: NumberParseException) {
                            // Fall through and return MatchType.NOT_A_NUMBER.
                        }
                    }
                }
            }
        }
        // One or more of the phone numbers we are trying to match is not a viable phone number.
        return MatchType.NOT_A_NUMBER
    }

    /**
     * Takes two phone numbers and compares them for equality. This is a convenience wrapper for
     * [.isNumberMatch]. No default region is known.
     *
     * @param firstNumber  first number to compare in proto buffer format
     * @param secondNumber  second number to compare. Can contain formatting, and can have country
     * calling code specified with + at the start.
     * @return  NOT_A_NUMBER, NO_MATCH, SHORT_NSN_MATCH, NSN_MATCH, EXACT_MATCH. See
     * [.isNumberMatch] for more details.
     */
    fun isNumberMatch(firstNumber: PhoneNumber, secondNumber: CharSequence?): MatchType {
        // First see if the second number has an implicit country calling code, by attempting to parse
        // it.
        try {
            val secondNumberAsProto = parse(secondNumber, UNKNOWN_REGION)
            return isNumberMatch(firstNumber, secondNumberAsProto)
        } catch (e: NumberParseException) {
            if (e.errorType === NumberParseException.ErrorType.INVALID_COUNTRY_CODE) {
                // The second number has no country calling code. EXACT_MATCH is no longer possible.
                // We parse it as if the region was the same as that for the first number, and if
                // EXACT_MATCH is returned, we replace this with NSN_MATCH.
                val firstNumberRegion = getRegionCodeForCountryCode(firstNumber.countryCode)
                try {
                    return if (firstNumberRegion != UNKNOWN_REGION) {
                        val secondNumberWithFirstNumberRegion = parse(secondNumber, firstNumberRegion)
                        val match = isNumberMatch(firstNumber, secondNumberWithFirstNumberRegion)
                        if (match == MatchType.EXACT_MATCH) {
                            MatchType.NSN_MATCH
                        } else match
                    } else {
                        // If the first number didn't have a valid country calling code, then we parse the
                        // second number without one as well.
                        val secondNumberProto = PhoneNumber()
                        parseHelper(secondNumber, null, false, false, secondNumberProto)
                        isNumberMatch(firstNumber, secondNumberProto)
                    }
                } catch (e2: NumberParseException) {
                    // Fall-through to return NOT_A_NUMBER.
                }
            }
        }
        // One or more of the phone numbers we are trying to match is not a viable phone number.
        return MatchType.NOT_A_NUMBER
    }

    /**
     * Returns true if the number can be dialled from outside the region, or unknown. If the number
     * can only be dialled from within the region, returns false. Does not check the number is a valid
     * number. Note that, at the moment, this method does not handle short numbers (which are
     * currently all presumed to not be diallable from outside their country).
     *
     * @param number  the phone-number for which we want to know whether it is diallable from
     * outside the region
     */
    fun canBeInternationallyDialled(number: PhoneNumber): Boolean {
        val metadata = getMetadataForRegion(getRegionCodeForNumber(number))
            ?: // Note numbers belonging to non-geographical entities (e.g. +800 numbers) are always
// internationally diallable, and will be caught here.
            return true
        val nationalSignificantNumber = getNationalSignificantNumber(number)
        return !isNumberMatchingDesc(nationalSignificantNumber, metadata.noInternationalDialling)
    }

    /**
     * Returns true if the supplied region supports mobile number portability. Returns false for
     * invalid, unknown or regions that don't support mobile number portability.
     *
     * @param regionCode  the region for which we want to know whether it supports mobile number
     * portability or not
     */
    fun isMobileNumberPortableRegion(regionCode: String): Boolean {
        val metadata = getMetadataForRegion(regionCode)
        if (metadata == null) {
            logger.w("Invalid or unknown region code provided: $regionCode")
            return false
        }
        return metadata.getMobileNumberPortableRegion()
    }

    companion object {
        private val logger = Logger.withTag(PhoneNumberUtil::class.simpleName.toString())

        /** Flags to use when compiling regular expressions for phone numbers.  */
        val REGEX_FLAGS = setOf(RegexOption.IGNORE_CASE)

        // The minimum and maximum length of the national significant number.
        private const val MIN_LENGTH_FOR_NSN = 2

        // The ITU says the maximum length should be 15, but we have found longer numbers in Germany.
        const val MAX_LENGTH_FOR_NSN = 17

        // The maximum length of the country calling code.
        const val MAX_LENGTH_COUNTRY_CODE = 3

        // We don't allow input strings for parsing to be longer than 250 chars. This prevents malicious
        // input from overflowing the regular-expression engine.
        private const val MAX_INPUT_STRING_LENGTH = 250

        // Region-code for the unknown region.
        private const val UNKNOWN_REGION = "ZZ"
        private const val NANPA_COUNTRY_CODE = 1

        // Map of country calling codes that use a mobile token before the area code. One example of when
        // this is relevant is when determining the length of the national destination code, which should
        // be the length of the area code plus the length of the mobile token.
        private var MOBILE_TOKEN_MAPPINGS: Map<Int, String>? = null

        // Set of country codes that have geographically assigned mobile numbers (see GEO_MOBILE_COUNTRIES
        // below) which are not based on *area codes*. For example, in China mobile numbers start with a
        // carrier indicator, and beyond that are geographically assigned: this carrier indicator is not
        // considered to be an area code.
        private var GEO_MOBILE_COUNTRIES_WITHOUT_MOBILE_AREA_CODES: Set<Int>? = null

        // Set of country calling codes that have geographically assigned mobile numbers. This may not be
        // complete; we add calling codes case by case, as we find geographical mobile numbers or hear
        // from user reports. Note that countries like the US, where we can't distinguish between
        // fixed-line or mobile numbers, are not listed here, since we consider FIXED_LINE_OR_MOBILE to be
        // a possibly geographically-related type anyway (like FIXED_LINE).
        private var GEO_MOBILE_COUNTRIES: Set<Int>? = null

        // The PLUS_SIGN signifies the international prefix.
        const val PLUS_SIGN = '+'
        private const val STAR_SIGN = '*'
        private const val RFC3966_EXTN_PREFIX = ";ext="
        private const val RFC3966_PREFIX = "tel:"
        private const val RFC3966_PHONE_CONTEXT = ";phone-context="
        private const val RFC3966_ISDN_SUBADDRESS = ";isub="

        // A map that contains characters that are essential when dialling. That means any of the
        // characters in this map must not be removed from a number when dialling, otherwise the call
        // will not reach the intended destination.
        private var DIALLABLE_CHAR_MAPPINGS: Map<Char, Char>? = null

        // Only upper-case variants of alpha characters are stored.
        private val ALPHA_MAPPINGS: Map<Char, Char> = hashMapOf(
            'A' to '2',
            'B' to '2',
            'C' to '2',
            'D' to '3',
            'E' to '3',
            'F' to '3',
            'G' to '4',
            'H' to '4',
            'I' to '4',
            'J' to '5',
            'K' to '5',
            'L' to '5',
            'M' to '6',
            'N' to '6',
            'O' to '6',
            'P' to '7',
            'Q' to '7',
            'R' to '7',
            'S' to '7',
            'T' to '8',
            'U' to '8',
            'V' to '8',
            'W' to '9',
            'X' to '9',
            'Y' to '9',
            'Z' to '9'
        )


        // For performance reasons, amalgamate both into one map.
        private var ALPHA_PHONE_MAPPINGS: Map<Char, Char>? = null

        // Separate map of all symbols that we wish to retain when formatting alpha numbers. This
        // includes digits, ASCII letters and number grouping symbols such as "-" and " ".
        private var ALL_PLUS_NUMBER_GROUPING_SYMBOLS: Map<Char, Char>? = null

        init {
            val mobileTokenMap = HashMap<Int, String>()
            mobileTokenMap[54] = "9"
            MOBILE_TOKEN_MAPPINGS = mobileTokenMap
            val geoMobileCountriesWithoutMobileAreaCodes = HashSet<Int>()
            geoMobileCountriesWithoutMobileAreaCodes.add(86) // China
            GEO_MOBILE_COUNTRIES_WITHOUT_MOBILE_AREA_CODES = geoMobileCountriesWithoutMobileAreaCodes
            val geoMobileCountries = HashSet<Int>()
            geoMobileCountries.add(52) // Mexico
            geoMobileCountries.add(54) // Argentina
            geoMobileCountries.add(55) // Brazil
            geoMobileCountries.add(62) // Indonesia: some prefixes only (fixed CMDA wireless)
            geoMobileCountries.addAll(geoMobileCountriesWithoutMobileAreaCodes)
            GEO_MOBILE_COUNTRIES = geoMobileCountries

            // Simple ASCII digits map used to populate ALPHA_PHONE_MAPPINGS and
            // ALL_PLUS_NUMBER_GROUPING_SYMBOLS.
            val asciiDigitMappings = HashMap<Char, Char>()
            asciiDigitMappings['0'] = '0'
            asciiDigitMappings['1'] = '1'
            asciiDigitMappings['2'] = '2'
            asciiDigitMappings['3'] = '3'
            asciiDigitMappings['4'] = '4'
            asciiDigitMappings['5'] = '5'
            asciiDigitMappings['6'] = '6'
            asciiDigitMappings['7'] = '7'
            asciiDigitMappings['8'] = '8'
            asciiDigitMappings['9'] = '9'
            val combinedMap = HashMap<Char, Char>(100)
            combinedMap.putAll(ALPHA_MAPPINGS as HashMap<Char, Char>)
            combinedMap.putAll(asciiDigitMappings)
            ALPHA_PHONE_MAPPINGS = combinedMap
            val diallableCharMap = HashMap<Char, Char>()
            diallableCharMap.putAll(asciiDigitMappings)
            diallableCharMap[PLUS_SIGN] = PLUS_SIGN
            diallableCharMap['*'] = '*'
            diallableCharMap['#'] = '#'
            DIALLABLE_CHAR_MAPPINGS = diallableCharMap
            val allPlusNumberGroupings = HashMap<Char, Char>()
            // Put (lower letter -> upper letter) and (upper letter -> upper letter) mappings.
            for (c in (ALPHA_MAPPINGS as HashMap<Char, Char>).keys) {
                allPlusNumberGroupings[c.lowercaseChar()] = c
                allPlusNumberGroupings[c] = c
            }
            allPlusNumberGroupings.putAll(asciiDigitMappings)
            // Put grouping symbols.
            allPlusNumberGroupings['-'] = '-'
            allPlusNumberGroupings['\uFF0D'] = '-'
            allPlusNumberGroupings['\u2010'] = '-'
            allPlusNumberGroupings['\u2011'] = '-'
            allPlusNumberGroupings['\u2012'] = '-'
            allPlusNumberGroupings['\u2013'] = '-'
            allPlusNumberGroupings['\u2014'] = '-'
            allPlusNumberGroupings['\u2015'] = '-'
            allPlusNumberGroupings['\u2212'] = '-'
            allPlusNumberGroupings['/'] = '/'
            allPlusNumberGroupings['\uFF0F'] = '/'
            allPlusNumberGroupings[' '] = ' '
            allPlusNumberGroupings['\u3000'] = ' '
            allPlusNumberGroupings['\u2060'] = ' '
            allPlusNumberGroupings['.'] = '.'
            allPlusNumberGroupings['\uFF0E'] = '.'
            ALL_PLUS_NUMBER_GROUPING_SYMBOLS = allPlusNumberGroupings
        }

        // Pattern that makes it easy to distinguish whether a region has a single international dialing
        // prefix or not. If a region has a single international prefix (e.g. 011 in USA), it will be
        // represented as a string that contains a sequence of ASCII digits, and possibly a tilde, which
        // signals waiting for the tone. If there are multiple available international prefixes in a
        // region, they will be represented as a regex string that always contains one or more characters
        // that are not ASCII digits or a tilde.
        private val SINGLE_INTERNATIONAL_PREFIX = Regex("[\\d]+(?:[~\u2053\u223C\uFF5E][\\d]+)?")

        // Regular expression of acceptable punctuation found in phone numbers, used to find numbers in
        // text and to decide what is a viable phone number. This excludes diallable characters.
        // This consists of dash characters, white space characters, full stops, slashes,
        // square brackets, parentheses and tildes. It also includes the letter 'x' as that is found as a
        // placeholder for carrier information in some phone numbers. Full-width variants are also
        // present.
        const val VALID_PUNCTUATION =
            ("-x\u2010-\u2015\u2212\u30FC\uFF0D-\uFF0F " + "\u00A0\u00AD\u200B\u2060\u3000()\uFF08\uFF09\uFF3B\uFF3D.\\[\\]/~\u2053\u223C\uFF5E")

        // partly taken from js due to issues with the regex statement not working on web
        // https://github.com/google/libphonenumber/blob/e3b0e10884bcb64b052508530bfaa72b53add3ff/javascript/i18n/phonenumbers/phonenumberutil.js#L547-L556
        /*
            ASCII: 0-9
            Full-width (East Asian): \uFF10-\uFF19
            Arabic-Indic: \u0660-\u0669
            Extended Arabic-Indic (Persian/Urdu): \u06F0-\u06F9
            Bengali: \u09E6-\u09EF
            Devanagari: \u0966-\u096F
            Thai: \u0E50-\u0E59
            Mongolian: \u1810-\u1819
         */
        private const val DIGITS = """[0-9\uFF10-\uFF19\u0660-\u0669\u06F0-\u06F9\u09E6-\u09EF\u0966-\u096F\u0E50-\u0E59\u1810-\u1819]"""
        // TODO: Use expect/actuals
        // the original regex statement from java
        //private const val DIGITS = "\\p{Nd}"

        // We accept alpha characters in phone numbers, ASCII only, upper and lower case.
        private val VALID_ALPHA = (ALPHA_MAPPINGS!!.keys.toTypedArray().contentToString()
            .replace("[, \\[\\]]".toRegex(), "") + ALPHA_MAPPINGS!!.keys.toTypedArray().contentToString().lowercase()
            .replace("[, \\[\\]]".toRegex(), ""))
        const val PLUS_CHARS = "+\uFF0B"

        @JvmField
        val PLUS_CHARS_PATTERN = Regex("[" + PLUS_CHARS + "]+")
        private val SEPARATOR_PATTERN = Regex("[" + VALID_PUNCTUATION + "]+")
        private val CAPTURING_DIGIT_PATTERN = Regex("(" + DIGITS + ")")

        // Regular expression of acceptable characters that may start a phone number for the purposes of
        // parsing. This allows us to strip away meaningless prefixes to phone numbers that may be
        // mistakenly given to us. This consists of digits, the plus symbol and arabic-indic digits. This
        // does not contain alpha characters, although they may be used later in the number. It also does
        // not include other punctuation, as this will be stripped later during parsing and is of no
        // information value when parsing a number.
        private val VALID_START_CHAR = "[" + PLUS_CHARS + DIGITS + "]"
        private val VALID_START_CHAR_PATTERN = Regex(VALID_START_CHAR)

        // Regular expression of characters typically used to start a second phone number for the purposes
        // of parsing. This allows us to strip off parts of the number that are actually the start of
        // another number, such as for: (530) 583-6985 x302/x2303 -> the second extension here makes this
        // actually two phone numbers, (530) 583-6985 x302 and (530) 583-6985 x2303. We remove the second
        // extension so that the first number is parsed correctly.
        private const val SECOND_NUMBER_START = "[\\\\/] *x"

        @JvmField
        val SECOND_NUMBER_START_PATTERN = Regex(SECOND_NUMBER_START)

        // Regular expression of trailing characters that we want to remove. We remove all characters that
        // are not alpha or numerical characters. The hash character is retained here, as it may signify
        // the previous block was an extension.
        // https://youtrack.jetbrains.com/issue/KT-58678/Native-Regex-inconsistency-with-JVM-Native-Regex
//        private const val UNWANTED_END_CHARS = "[[\\P{N}&&\\P{L}]&&[^#]]+$"
        private const val UNWANTED_END_CHARS = "[^$DIGITS#A-Za-z]+$"

        @JvmField
        val UNWANTED_END_CHAR_PATTERN = Regex(UNWANTED_END_CHARS)

        // We use this pattern to check if the phone number has at least three letters in it - if so, then
        // we treat it as a number where some phone-number digits are represented by letters.
        private val VALID_ALPHA_PHONE_PATTERN = Regex("(?:.*?[A-Za-z]){3}.*")

        // Regular expression of viable phone numbers. This is location independent. Checks we have at
        // least three leading digits, and only valid punctuation, alpha characters and
        // digits in the phone number. Does not include extension data.
        // The symbol 'x' is allowed here as valid punctuation since it is often used as a placeholder for
        // carrier codes, for example in Brazilian phone numbers. We also allow multiple "+" characters at
        // the start.
        // Corresponds to the following:
        // [digits]{minLengthNsn}|
        // plus_sign*(([punctuation]|[star])*[digits]){3,}([punctuation]|[star]|[digits]|[alpha])*
        //
        // The first reg-ex is to allow short numbers (two digits long) to be parsed if they are entered
        // as "15" etc, but only if there is no punctuation in them. The second expression restricts the
        // number of digits to three or more, but then allows them to be in international form, and to
        // have alpha-characters and punctuation.
        //
        // Note VALID_PUNCTUATION starts with a -, so must be the first in the range.
        private val VALID_PHONE_NUMBER =
            "[$PLUS_CHARS]*(?:[$VALID_PUNCTUATION$STAR_SIGN]*[$DIGITS]){3,}[$VALID_PUNCTUATION$STAR_SIGN$VALID_ALPHA$DIGITS]*|[$DIGITS]{$MIN_LENGTH_FOR_NSN}"

        // Default extension prefix to use when formatting. This will be put in front of any extension
        // component of the number, after the main national number is formatted. For example, if you wish
        // the default extension formatting to be " extn: 3456", then you should specify " extn: " here
        // as the default extension prefix. This can be overridden by region-specific preferences.
        private const val DEFAULT_EXTN_PREFIX = " ext. "

        // Regexp of all possible ways to write extensions, for use when parsing. This will be run as a
        // case-insensitive regexp match. Wide character versions are also provided after each ASCII
        // version.
        private val EXTN_PATTERNS_FOR_PARSING = createExtnPattern(true)

        @JvmField
        val EXTN_PATTERNS_FOR_MATCHING = createExtnPattern(false)

        // Regular expression of valid global-number-digits for the phone-context parameter, following the
        // syntax defined in RFC3966.
        private const val RFC3966_VISUAL_SEPARATOR = "[\\-\\.\\(\\)]?"
        private val RFC3966_PHONE_DIGIT = "(" + DIGITS + "|" + RFC3966_VISUAL_SEPARATOR + ")"
        private val RFC3966_GLOBAL_NUMBER_DIGITS =
            "^\\" + PLUS_SIGN + RFC3966_PHONE_DIGIT + "*" + DIGITS + RFC3966_PHONE_DIGIT + "*$"
        val RFC3966_GLOBAL_NUMBER_DIGITS_PATTERN = Regex(RFC3966_GLOBAL_NUMBER_DIGITS)

        // Regular expression of valid domainname for the phone-context parameter, following the syntax
        // defined in RFC3966.
        private val ALPHANUM = VALID_ALPHA + DIGITS
        private val RFC3966_DOMAINLABEL = "[$ALPHANUM]+((-)*[$ALPHANUM])*"
        private val RFC3966_TOPLABEL = "[$VALID_ALPHA]+((-)*[$ALPHANUM])*"
        private val RFC3966_DOMAINNAME = "^($RFC3966_DOMAINLABEL\\.)*$RFC3966_TOPLABEL\\.?$"
        val RFC3966_DOMAINNAME_PATTERN = Regex(RFC3966_DOMAINNAME)

        /**
         * Helper method for constructing regular expressions for parsing. Creates an expression that
         * captures up to maxLength digits.
         */
        private fun extnDigits(maxLength: Int): String {
            return "(" + DIGITS + "{1," + maxLength + "})"
        }

        /**
         * Helper initialiser method to create the regular-expression pattern to match extensions.
         * Note that there are currently six capturing groups for the extension itself. If this number is
         * changed, MaybeStripExtension needs to be updated.
         */
        private fun createExtnPattern(forParsing: Boolean): String {
            // We cap the maximum length of an extension based on the ambiguity of the way the extension is
            // prefixed. As per ITU, the officially allowed length for extensions is actually 40, but we
            // don't support this since we haven't seen real examples and this introduces many false
            // interpretations as the extension labels are not standardized.
            val extLimitAfterExplicitLabel = 20
            val extLimitAfterLikelyLabel = 15
            val extLimitAfterAmbiguousChar = 9
            val extLimitWhenNotSure = 6
            val possibleSeparatorsBetweenNumberAndExtLabel = "[ \u00A0\\t,]*"
            // Optional full stop (.) or colon, followed by zero or more spaces/tabs/commas.
            val possibleCharsAfterExtLabel = "[:\\.\uFF0E]?[ \u00A0\\t,-]*"
            val optionalExtnSuffix = "#?"

            // Here the extension is called out in more explicit way, i.e mentioning it obvious patterns
            // like "ext.". Canonical-equivalence doesn't seem to be an option with Android java, so we
            // allow two options for representing the accented o - the character itself, and one in the
            // unicode decomposed form with the combining acute accent.
            val explicitExtLabels =
                "(?:e?xt(?:ensi(?:o\u0301?|\u00F3))?n?|\uFF45?\uFF58\uFF54\uFF4E?|\u0434\u043E\u0431|anexo)"
            // One-character symbols that can be used to indicate an extension, and less commonly used
            // or more ambiguous extension labels.
            val ambiguousExtLabels = "(?:[x\uFF58#\uFF03~\uFF5E]|int|\uFF49\uFF4E\uFF54)"
            // When extension is not separated clearly.
            val ambiguousSeparator = "[- ]+"
            val rfcExtn = RFC3966_EXTN_PREFIX + extnDigits(extLimitAfterExplicitLabel)
            val explicitExtn =
                (possibleSeparatorsBetweenNumberAndExtLabel + explicitExtLabels + possibleCharsAfterExtLabel + extnDigits(
                    extLimitAfterExplicitLabel
                ) + optionalExtnSuffix)
            val ambiguousExtn =
                (possibleSeparatorsBetweenNumberAndExtLabel + ambiguousExtLabels + possibleCharsAfterExtLabel + extnDigits(
                    extLimitAfterAmbiguousChar
                ) + optionalExtnSuffix)
            val americanStyleExtnWithSuffix = ambiguousSeparator + extnDigits(extLimitWhenNotSure) + "#"

            // The first regular expression covers RFC 3966 format, where the extension is added using
            // ";ext=". The second more generic where extension is mentioned with explicit labels like
            // "ext:". In both the above cases we allow more numbers in extension than any other extension
            // labels. The third one captures when single character extension labels or less commonly used
            // labels are used. In such cases we capture fewer extension digits in order to reduce the
            // chance of falsely interpreting two numbers beside each other as a number + extension. The
            // fourth one covers the special case of American numbers where the extension is written with a
            // hash at the end, such as "- 503#".
            val extensionPattern =
                (rfcExtn + "|" + explicitExtn + "|" + ambiguousExtn + "|" + americanStyleExtnWithSuffix)
            // Additional pattern that is supported when parsing extensions, not when matching.
            if (forParsing) {
                // This is same as possibleSeparatorsBetweenNumberAndExtLabel, but not matching comma as
                // extension label may have it.
                val possibleSeparatorsNumberExtLabelNoComma = "[ \u00A0\\t]*"
                // ",," is commonly used for auto dialling the extension when connected. First comma is matched
                // through possibleSeparatorsBetweenNumberAndExtLabel, so we do not repeat it here. Semi-colon
                // works in Iphone and Android also to pop up a button with the extension number following.
                val autoDiallingAndExtLabelsFound = "(?:,{2}|;)"
                val autoDiallingExtn =
                    (possibleSeparatorsNumberExtLabelNoComma + autoDiallingAndExtLabelsFound + possibleCharsAfterExtLabel + extnDigits(
                        extLimitAfterLikelyLabel
                    ) + optionalExtnSuffix)
                val onlyCommasExtn =
                    (possibleSeparatorsNumberExtLabelNoComma + "(?:,)+" + possibleCharsAfterExtLabel + extnDigits(
                        extLimitAfterAmbiguousChar
                    ) + optionalExtnSuffix)
                // Here the first pattern is exclusively for extension autodialling formats which are used
                // when dialling and in this case we accept longer extensions. However, the second pattern
                // is more liberal on the number of commas that acts as extension labels, so we have a strict
                // cap on the number of digits in such extensions.
                return (extensionPattern + "|" + autoDiallingExtn + "|" + onlyCommasExtn)
            }
            return extensionPattern
        }

        // Regexp of all known extension prefixes used by different regions followed by 1 or more valid
        // digits, for use when parsing.
        private val EXTN_PATTERN = Regex("(?:" + EXTN_PATTERNS_FOR_PARSING + ")$", REGEX_FLAGS)

        // We append optionally the extension pattern to the end here, as a valid phone number may
        // have an extension prefix appended, followed by 1 or more digits.
        private val VALID_PHONE_NUMBER_PATTERN =
            Regex("$VALID_PHONE_NUMBER(?:$EXTN_PATTERNS_FOR_PARSING)?", REGEX_FLAGS)

        @JvmField
        val NON_DIGITS_PATTERN = Regex("(\\D+)")

        // The FIRST_GROUP_PATTERN was originally set to $1 but there are some countries for which the
        // first group is not used in the national pattern (e.g. Argentina) so the $1 group does not match
        // correctly.  Therefore, we use \d, so that the first group actually used in the pattern will be
        // matched.
        private val FIRST_GROUP_PATTERN = Regex("(\\$\\d)")

        // Constants used in the formatting rules to represent the national prefix, first group and
        // carrier code respectively.
        private const val NP_STRING = "\$NP"
        private const val FG_STRING = "\$FG"
        private const val CC_STRING = "\$CC"

        // A pattern that is used to determine if the national prefix formatting rule has the first group
        // only, i.e., does not start with the national prefix. Note that the pattern explicitly allows
        // for unbalanced parentheses.
        private val FIRST_GROUP_ONLY_PREFIX_PATTERN = Regex("\\(?\\$1\\)?")
        const val REGION_CODE_FOR_NON_GEO_ENTITY = "001"

        /**
         * Attempts to extract a possible number from the string passed in. This currently strips all
         * leading characters that cannot be used to start a phone number. Characters that can be used to
         * start a phone number are defined in the VALID_START_CHAR_PATTERN. If none of these characters
         * are found in the number passed in, an empty string is returned. This function also attempts to
         * strip off any alternative extensions or endings if two or more are present, such as in the case
         * of: (530) 583-6985 x302/x2303. The second extension here makes this actually two phone numbers,
         * (530) 583-6985 x302 and (530) 583-6985 x2303. We remove the second extension so that the first
         * number is parsed correctly.
         *
         * @param number  the string that might contain a phone number
         * @return  the number, stripped of any non-phone-number prefix (such as "Tel:") or an empty
         * string if no character used to start phone numbers (such as + or any digit) is found in the
         * number
         */
        @JvmStatic
        fun extractPossibleNumber(number: CharSequence): CharSequence {
            var number = number
            val m = VALID_START_CHAR_PATTERN.find(number)
            return if (m != null) {
                number = number.subSequence(m.range.first, number.length)
                // Remove trailing non-alpha non-numerical characters.
                val trailingCharsMatchResult = UNWANTED_END_CHAR_PATTERN.find(number)
                if (trailingCharsMatchResult != null) {
                    number = number.subSequence(0, trailingCharsMatchResult.range.first)
                }
                // Check for extra numbers at the end.
                val secondNumberMatchResult = SECOND_NUMBER_START_PATTERN.find(number)
                if (secondNumberMatchResult != null) {
                    number = number.subSequence(0, secondNumberMatchResult.range.first)
                }
                number
            } else {
                ""
            }
        }

        /**
         * Checks to see if the string of characters could possibly be a phone number at all. At the
         * moment, checks to see that the string begins with at least 2 digits, ignoring any punctuation
         * commonly found in phone numbers.
         * This method does not require the number to be normalized in advance - but does assume that
         * leading non-number symbols have been removed, such as by the method extractPossibleNumber.
         *
         * @param number  string to be checked for viability as a phone number
         * @return  true if the number could be a phone number of some sort, otherwise false
         */
        // @VisibleForTesting
        fun isViablePhoneNumber(number: CharSequence): Boolean {
            if (number.length < MIN_LENGTH_FOR_NSN) {
                return false
            }
            return VALID_PHONE_NUMBER_PATTERN.matches(number)
        }

        /**
         * Normalizes a string of characters representing a phone number. This performs the following
         * conversions:
         * - Punctuation is stripped.
         * For ALPHA/VANITY numbers:
         * - Letters are converted to their numeric representation on a telephone keypad. The keypad
         * used here is the one defined in ITU Recommendation E.161. This is only done if there are 3
         * or more letters in the number, to lessen the risk that such letters are typos.
         * For other numbers:
         * - Wide-ascii digits are converted to normal ASCII (European) digits.
         * - Arabic-Indic numerals are converted to European numerals.
         * - Spurious alpha characters are stripped.
         *
         * @param number  a InplaceStringBuilder of characters representing a phone number that will be
         * normalized in place
         */
        fun normalize(number: InplaceStringBuilder): InplaceStringBuilder {
            if (VALID_ALPHA_PHONE_PATTERN.matches(number)) {
                number.replaceRange(0, number.length, normalizeHelper(number, ALPHA_PHONE_MAPPINGS, true))
            } else {
                number.replaceRange(0, number.length, normalizeDigitsOnly(number))
            }
            return number
        }

        /**
         * Normalizes a string of characters representing a phone number. This converts wide-ascii and
         * arabic-indic numerals to European numerals, and strips punctuation and alpha characters.
         *
         * @param number  a string of characters representing a phone number
         * @return  the normalized string version of the phone number
         */
        @JvmStatic
        fun normalizeDigitsOnly(number: CharSequence): String {
            return normalizeDigits(number, false /* strip non-digits */).toString()
        }

        @JvmStatic
        fun normalizeDigits(number: CharSequence, keepNonDigits: Boolean): InplaceStringBuilder {
            val normalizedDigits = InplaceStringBuilder(number.length)
            for (i in 0..<number.length) {
                val c = number[i]
                val digit = c.digitToIntOrNull() ?: -1
                if (digit != -1) {
                    normalizedDigits.append(digit)
                } else if (keepNonDigits) {
                    normalizedDigits.append(c)
                }
            }
            return normalizedDigits
        }

        /**
         * Normalizes a string of characters representing a phone number. This strips all characters which
         * are not diallable on a mobile phone keypad (including all non-ASCII digits).
         *
         * @param number  a string of characters representing a phone number
         * @return  the normalized string version of the phone number
         */
        @JvmStatic
        fun normalizeDiallableCharsOnly(number: CharSequence): String {
            return normalizeHelper(number, DIALLABLE_CHAR_MAPPINGS, true /* remove non matches */)
        }

        /**
         * Converts all alpha characters in a number to their respective digits on a keypad, but retains
         * existing formatting.
         */
        fun convertAlphaCharactersInNumber(number: CharSequence): String {
            return normalizeHelper(number, ALPHA_PHONE_MAPPINGS, false)
        }

        /**
         * Returns the mobile token for the provided country calling code if it has one, otherwise
         * returns an empty string. A mobile token is a number inserted before the area code when dialing
         * a mobile number from that country from abroad.
         *
         * @param countryCallingCode  the country calling code for which we want the mobile token
         * @return  the mobile token, as a string, for the given country calling code
         */
        @JvmStatic
        fun getCountryMobileToken(countryCallingCode: Int): String? {
            return if (MOBILE_TOKEN_MAPPINGS!!.containsKey(countryCallingCode)) {
                MOBILE_TOKEN_MAPPINGS!![countryCallingCode]
            } else ""
        }

        /**
         * Normalizes a string of characters representing a phone number by replacing all characters found
         * in the accompanying map with the values therein, and stripping all other characters if
         * removeNonMatches is true.
         *
         * @param number  a string of characters representing a phone number
         * @param normalizationReplacements  a mapping of characters to what they should be replaced by in
         * the normalized version of the phone number
         * @param removeNonMatches  indicates whether characters that are not able to be replaced should
         * be stripped from the number. If this is false, they will be left unchanged in the number.
         * @return  the normalized string version of the phone number
         */
        private fun normalizeHelper(
            number: CharSequence, normalizationReplacements: Map<Char, Char>?, removeNonMatches: Boolean
        ): String {
            val normalizedNumber = InplaceStringBuilder(number.length)
            for (i in 0..<number.length) {
                val character = number[i]
                val newDigit = normalizationReplacements!![character.uppercaseChar()]
                if (newDigit != null) {
                    normalizedNumber.append(newDigit)
                } else if (!removeNonMatches) {
                    normalizedNumber.append(character)
                }
                // If neither of the above are true, we remove this character.
            }
            return normalizedNumber.toString()
        }

        /**
         * Returns true if there is any possible number data set for a particular PhoneNumberDesc.
         */
        private fun descHasPossibleNumberData(desc: PhoneNumberDesc?): Boolean {
            // If this is empty, it means numbers of this type inherit from the "general desc" -> the value
            // "-1" means that no numbers exist for this type.
            return desc!!.possibleLengthCount != 1 || desc.getPossibleLength(0) != -1
        }
        // Note: descHasData must account for any of MetadataFilter's excludableChildFields potentially
        // being absent from the metadata. It must check them all. For any changes in descHasData, ensure
        // that all the excludableChildFields are still being checked. If your change is safe simply
        // mention why during a review without needing to change MetadataFilter.
        /**
         * Returns true if there is any data set for a particular PhoneNumberDesc.
         */
        private fun descHasData(desc: PhoneNumberDesc?): Boolean {
            // Checking most properties since we don't know what's present, since a custom build may have
            // stripped just one of them (e.g. liteBuild strips exampleNumber). We don't bother checking the
            // possibleLengthsLocalOnly, since if this is the only thing that's present we don't really
            // support the type at all: no type-specific methods will work with only this data.
            return (desc!!.hasExampleNumber() || descHasPossibleNumberData(desc) || desc.hasNationalNumberPattern())
        }

        /**
         * Create a new [PhoneNumberUtil] instance to carry out international phone number
         * formatting, parsing, or validation. The instance is loaded with all metadata by
         * using the metadataLoader specified.
         *
         *
         * This method should only be used in the rare case in which you want to manage your own
         * metadata loading. Calling this method multiple times is very expensive, as each time
         * a new instance is created from scratch.
         *
         * @param metadataLoader  customized metadata loader. This should not be null
         * @return  a PhoneNumberUtil instance
         */
        @JvmStatic
        fun createInstance(metadataLoader: MetadataLoader): PhoneNumberUtil {
            val metadataDependenciesProvider = DefaultMetadataDependenciesProvider(metadataLoader)
            val metadataSource: MetadataSource = MetadataSourceImpl(
                metadataDependenciesProvider.phoneNumberMetadataFileNameProvider,
                metadataLoader,
                metadataDependenciesProvider.metadataParser
            )
            return createInstance(metadataSource, metadataDependenciesProvider)
        }


        /**
         * Create a new [PhoneNumberUtil] instance to carry out international phone number
         * formatting, parsing, or validation. The instance is loaded with all metadata by
         * using the metadataSource specified.
         *
         *
         * This method should only be used in the rare case in which you want to manage your own
         * metadata loading. Calling this method multiple times is very expensive, as each time
         * a new instance is created from scratch.
         *
         * @param metadataSource  customized metadata source. This should not be null
         * @param metadataDependenciesProvider  customized metadata dependency provider.
         * This should not be null
         * @return  a PhoneNumberUtil instance
         */
        private fun createInstance(
            metadataSource: MetadataSource, metadataDependenciesProvider: DefaultMetadataDependenciesProvider
        ): PhoneNumberUtil {
            return PhoneNumberUtil(
                metadataSource, metadataDependenciesProvider, countryCodeToRegionCodeMap
            )
        }

        /**
         * Helper function to check if the national prefix formatting rule has the first group only, i.e.,
         * does not start with the national prefix.
         */
        @JvmStatic
        fun formattingRuleHasFirstGroupOnly(nationalPrefixFormattingRule: String): Boolean {
            return (nationalPrefixFormattingRule.length == 0 || FIRST_GROUP_ONLY_PREFIX_PATTERN.matches(
                nationalPrefixFormattingRule
            ))
        }

        private fun ensureMetadataIsNonNull(phoneMetadata: PhoneMetadata?, message: String) {
            if (phoneMetadata == null) {
                throw MissingMetadataException(message)
            }
        }

        /**
         * A helper function to set the values related to leading zeros in a PhoneNumber.
         */
        fun setItalianLeadingZerosForPhoneNumber(
            nationalNumber: CharSequence, phoneNumber: PhoneNumber
        ) {
            if (nationalNumber.length > 1 && nationalNumber[0] == '0') {
                phoneNumber.setItalianLeadingZero(true)
                var numberOfLeadingZeros = 1
                // Note that if the national number is all "0"s, the last "0" is not counted as a leading
                // zero.
                while (numberOfLeadingZeros < nationalNumber.length - 1 && nationalNumber[numberOfLeadingZeros] == '0') {
                    numberOfLeadingZeros++
                }
                if (numberOfLeadingZeros != 1) {
                    phoneNumber.setNumberOfLeadingZeros(numberOfLeadingZeros)
                }
            }
        }

        /**
         * Returns a new phone number containing only the fields needed to uniquely identify a phone
         * number, rather than any fields that capture the context in which the phone number was created.
         * These fields correspond to those set in parse() rather than parseAndKeepRawInput().
         */
        private fun copyCoreFieldsOnly(phoneNumberIn: PhoneNumber): PhoneNumber {
            val phoneNumber = PhoneNumber()
            phoneNumber.setCountryCode(phoneNumberIn.countryCode)
            phoneNumber.setNationalNumber(phoneNumberIn.nationalNumber)
            if (phoneNumberIn.extension.length > 0) {
                phoneNumber.setExtension(phoneNumberIn.extension)
            }
            if (phoneNumberIn.isItalianLeadingZero) {
                phoneNumber.setItalianLeadingZero(true)
                // This field is only relevant if there are leading zeros at all.
                phoneNumber.setNumberOfLeadingZeros(phoneNumberIn.numberOfLeadingZeros)
            }
            return phoneNumber
        }
    }
}
