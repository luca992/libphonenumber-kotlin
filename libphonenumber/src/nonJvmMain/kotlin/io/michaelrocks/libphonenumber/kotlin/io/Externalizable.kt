package io.michaelrocks.libphonenumber.kotlin.io


actual interface Externalizable: Serializable {
    actual fun writeExternal(out: ObjectOutput)

    actual fun readExternal(input: ObjectInput)
}