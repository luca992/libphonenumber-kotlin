package io.michaelrocks.libphonenumber.kotlin.io


actual interface ObjectOutput : DataOutput, AutoCloseable {
    actual fun writeObject(obj: Any?)
    actual fun flush()
}