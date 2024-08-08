package io.michaelrocks.libphonenumber.kotlin.io

interface ObjectInputStream : InputStream, ObjectInput, ObjectStreamConstants


expect fun getPlatformObjectInputStream(inputStream: InputStream): ObjectInputStream