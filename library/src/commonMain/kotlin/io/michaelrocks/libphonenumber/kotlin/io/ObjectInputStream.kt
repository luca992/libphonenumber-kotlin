package io.michaelrocks.libphonenumber.kotlin.io

expect open class ObjectInputStream(inputStream: InputStream) : InputStream, ObjectInput, ObjectStreamConstants
