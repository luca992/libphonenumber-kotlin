package io.michaelrocks.libphonenumber.kotlin.metadata

import io.michaelrocks.libphonenumber.kotlin.Phonemetadata.PhoneMetadataCollection
import java.io.*

object PhoneMetadataCollectionUtil {
    @Throws(IOException::class)
    fun toInputStream(metadata: PhoneMetadataCollection): InputStream {
        val outputStream = ByteArrayOutputStream()
        val objectOutputStream = ObjectOutputStream(outputStream)
        metadata.writeExternal(objectOutputStream)
        objectOutputStream.flush()
        val inputStream: InputStream = ByteArrayInputStream(outputStream.toByteArray())
        objectOutputStream.close()
        return inputStream
    }
}
