package io.michaelrocks.libphonenumber.kotlin.io


actual interface Externalizable {
    actual fun writeExternal(out: ObjectOutput)

    actual fun readExternal(input: ObjectInput)
}