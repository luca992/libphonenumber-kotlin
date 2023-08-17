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

import io.michaelrocks.libphonenumber.kotlin.Phonenumber.PhoneNumber

/**
 * The immutable match of a phone number within a piece of text. Matches may be found using
 * [PhoneNumberUtil.findNumbers].
 *
 *
 * A match consists of the [phone number][.number] as well as the
 * [start][.start] and [end][.end] offsets of the corresponding subsequence
 * of the searched text. Use [.rawString] to obtain a copy of the matched subsequence.
 *
 *
 * The following annotated example clarifies the relationship between the searched text, the
 * match offsets, and the parsed number:
 *
 * <pre>
 * CharSequence text = "Call me at +1 425 882-8080 for details.";
 * String country = "US";
 * PhoneNumberUtil util = PhoneNumberUtil.getInstance();
 *
 * // Find the first phone number match:
 * PhoneNumberMatch m = util.findNumbers(text, country).iterator().next();
 *
 * // rawString() contains the phone number as it appears in the text.
 * "+1 425 882-8080".equals(m.rawString());
 *
 * // start() and end() define the range of the matched subsequence.
 * CharSequence subsequence = text.subSequence(m.start(), m.end());
 * "+1 425 882-8080".contentEquals(subsequence);
 *
 * // number() returns the the same result as PhoneNumberUtil.[parse()][PhoneNumberUtil.parse]
 * // invoked on rawString().
 * util.parse(m.rawString(), country).equals(m.number());
</pre> *
 */
class PhoneNumberMatch internal constructor(start: Int, rawString: String?, number: PhoneNumber?) {
    /** The start index into the text.  */
    private val start: Int

    /** The raw substring matched.  */
    private val rawString: String

    /** The matched phone number.  */
    private val number: PhoneNumber

    /**
     * Creates a new match.
     *
     * @param start  the start index into the target text
     * @param rawString  the matched substring of the target text
     * @param number  the matched phone number
     */
    init {
        require(start >= 0) { "Start index must be >= 0." }
        if (rawString == null || number == null) {
            throw NullPointerException()
        }
        this.start = start
        this.rawString = rawString
        this.number = number
    }

    /** Returns the phone number matched by the receiver.  */
    fun number(): PhoneNumber {
        return number
    }

    /** Returns the start index of the matched phone number within the searched text.  */
    fun start(): Int {
        return start
    }

    /** Returns the exclusive end index of the matched phone number within the searched text.  */
    fun end(): Int {
        return start + rawString.length
    }

    /** Returns the raw string matched as a phone number in the searched text.  */
    fun rawString(): String {
        return rawString
    }

    override fun hashCode(): Int {
        return arrayOf<Any>(start, rawString, number).contentHashCode()
    }

    override fun equals(obj: Any?): Boolean {
        if (this === obj) {
            return true
        }
        if (obj !is PhoneNumberMatch) {
            return false
        }
        val other = obj
        return (rawString == other.rawString && start == other.start
                && number.equals(other.number))
    }

    override fun toString(): String {
        return "PhoneNumberMatch [" + start() + "," + end() + ") " + rawString
    }
}
