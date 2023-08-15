///*
// * Copyright (C) 2022 The Libphonenumber Authors
// * Copyright (C) 2022 Michael Rozumyanskiy
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// * http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//package io.michaelrocks.libphonenumber.android.metadata.init
//
//import io.michaelrocks.libphonenumber.android.metadata.PhoneMetadataCollectionUtil
//import io.michaelrocks.libphonenumber.kotlin.Phonemetadata
//import io.michaelrocks.libphonenumber.kotlin.metadata.init.MetadataParser
//import junit.framework.TestCase
//import org.junit.Assert
//import java.io.ByteArrayInputStream
//import java.io.IOException
//import java.io.InputStream
//import java.nio.charset.StandardCharsets
//
//class MetadataParserTest : TestCase() {
//    fun test_parse_shouldThrowExceptionForNullInput() {
//        Assert.assertThrows(
//            IllegalArgumentException::class.java
//        ) { metadataParser.parse(null) }
//    }
//
//    fun test_parse_shouldThrowExceptionForEmptyInput() {
//        val emptyInput: InputStream = ByteArrayInputStream(ByteArray(0))
//        Assert.assertThrows(
//            IllegalStateException::class.java
//        ) { metadataParser.parse(emptyInput) }
//    }
//
//    fun test_parse_shouldThrowExceptionForInvalidInput() {
//        val invalidInput: InputStream = ByteArrayInputStream("Some random input".toByteArray(StandardCharsets.UTF_8))
//        Assert.assertThrows(
//            IllegalStateException::class.java
//        ) { metadataParser.parse(invalidInput) }
//    }
//
//    @Throws(IOException::class)
//    fun test_parse_shouldParseValidInput() {
//        val input: InputStream = PhoneMetadataCollectionUtil.toInputStream(
//            Phonemetadata.PhoneMetadataCollection.newBuilder()
//                .addMetadata(Phonemetadata.PhoneMetadata.newBuilder().setId("id").build())
//        )
//        val actual: Collection<Phonemetadata.PhoneMetadata> = metadataParser.parse(input)
//        assertEquals(1, actual.size)
//    }
//
//    fun test_parse_shouldReturnEmptyCollectionForNullInput() {
//        val actual: Collection<Phonemetadata.PhoneMetadata> = MetadataParser.newLenientParser().parse(null)
//        assertTrue(actual.isEmpty())
//    }
//
//    companion object {
//        private val metadataParser = MetadataParser.newStrictParser()
//    }
//}
