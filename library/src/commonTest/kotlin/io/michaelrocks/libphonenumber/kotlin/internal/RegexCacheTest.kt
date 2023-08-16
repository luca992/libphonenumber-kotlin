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
package io.michaelrocks.libphonenumber.kotlin.internal

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unittests for LRU Cache for compiled regular expressions used by the libphonenumbers libary.
 *
 * @author Shaopeng Jia
 */
class RegexCacheTest {
    private val regexCache: RegexCache = RegexCache(2)

    @Test
    fun testRegexInsertion() {
        val regex1 = "[1-5]"
        val regex2 = "(?:12|34)"
        val regex3 = "[1-3][58]"
        regexCache.getRegexForPattern(regex1)
        assertTrue(regexCache.containsRegex(regex1))
        regexCache.getRegexForPattern(regex2)
        assertTrue(regexCache.containsRegex(regex2))
        assertTrue(regexCache.containsRegex(regex1))
        regexCache.getRegexForPattern(regex1)
        assertTrue(regexCache.containsRegex(regex1))
        regexCache.getRegexForPattern(regex3)
        assertTrue(regexCache.containsRegex(regex3))
        assertFalse(regexCache.containsRegex(regex2))
        assertTrue(regexCache.containsRegex(regex1))
    }
}
