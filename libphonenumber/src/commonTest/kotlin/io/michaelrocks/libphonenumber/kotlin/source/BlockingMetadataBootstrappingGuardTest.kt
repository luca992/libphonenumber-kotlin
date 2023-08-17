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
//package io.michaelrocks.libphonenumber.android.metadata.source
//
//import io.michaelrocks.libphonenumber.android.metadata.PhoneMetadataCollectionUtil.toInputStream
//import io.michaelrocks.libphonenumber.kotlin.MetadataLoader
//import io.michaelrocks.libphonenumber.kotlin.Phonemetadata.PhoneMetadata
//import io.michaelrocks.libphonenumber.kotlin.Phonemetadata.PhoneMetadataCollection
//import io.michaelrocks.libphonenumber.kotlin.metadata.init.MetadataParser
//import io.michaelrocks.libphonenumber.kotlin.metadata.source.BlockingMetadataBootstrappingGuard
//import io.michaelrocks.libphonenumber.kotlin.metadata.source.MetadataContainer
//import junit.framework.TestCase
//import org.junit.Assert
//import org.junit.function.ThrowingRunnable
//import org.mockito.Mockito
//import java.io.IOException
//import java.util.concurrent.Callable
//import java.util.concurrent.Executors
//
//class BlockingMetadataBootstrappingGuardTest : TestCase() {
//    private val metadataLoader = Mockito.mock(
//        MetadataLoader::class.java
//    )
//    private val metadataContainer = Mockito.mock(MetadataContainer::class.java)
//    private var bootstrappingGuard: BlockingMetadataBootstrappingGuard<MetadataContainer>? = null
//    @Throws(IOException::class)
//    public override fun setUp() {
//        Mockito.`when`<Any?>(metadataLoader.loadMetadata(PHONE_METADATA_ASSET_RESOURCE))
//            .thenReturn(toInputStream(PHONE_METADATA))
//        bootstrappingGuard = BlockingMetadataBootstrappingGuard(
//            metadataLoader, MetadataParser.newStrictParser(), metadataContainer
//        )
//    }
//
//    fun test_getOrBootstrap_shouldInvokeBootstrappingOnlyOnce() {
//        bootstrappingGuard!!.getOrBootstrap(PHONE_METADATA_ASSET_RESOURCE)
//        bootstrappingGuard!!.getOrBootstrap(PHONE_METADATA_ASSET_RESOURCE)
//        Mockito.verify(metadataLoader, Mockito.times(1)).loadMetadata(PHONE_METADATA_ASSET_RESOURCE)
//    }
//
//    fun test_getOrBootstrap_shouldIncludeFileNameInExceptionOnFailure() {
//        Mockito.`when`<Any?>(metadataLoader.loadMetadata(PHONE_METADATA_ASSET_RESOURCE)).thenReturn(null)
//        val throwingRunnable = ThrowingRunnable { bootstrappingGuard!!.getOrBootstrap(PHONE_METADATA_ASSET_RESOURCE) }
//        val exception = Assert.assertThrows(
//            IllegalStateException::class.java, throwingRunnable
//        )
//        Assert.assertTrue(exception.message!!.contains(PHONE_METADATA_ASSET_RESOURCE))
//    }
//
//    @Throws(InterruptedException::class)
//    fun test_getOrBootstrap_shouldInvokeBootstrappingOnlyOnceWhenThreadsCallItAtTheSameTime() {
//        val executorService = Executors.newFixedThreadPool(2)
//        val runnables: MutableList<BootstrappingRunnable> = ArrayList()
//        runnables.add(BootstrappingRunnable())
//        runnables.add(BootstrappingRunnable())
//        executorService.invokeAll(runnables)
//        Mockito.verify(metadataLoader, Mockito.times(1)).loadMetadata(PHONE_METADATA_ASSET_RESOURCE)
//    }
//
//    private inner class BootstrappingRunnable : Callable<MetadataContainer> {
//        override fun call(): MetadataContainer {
//            return bootstrappingGuard!!.getOrBootstrap(PHONE_METADATA_ASSET_RESOURCE)
//        }
//    }
//
//    companion object {
//        private const val PHONE_METADATA_ASSET_RESOURCE = createFileResourceMock()
//        private val PHONE_METADATA = PhoneMetadataCollection.newBuilder()
//            .addMetadata(PhoneMetadata.newBuilder().setId("id").build())
//    }
//}
