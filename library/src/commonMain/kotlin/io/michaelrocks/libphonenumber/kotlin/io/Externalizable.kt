package io.michaelrocks.libphonenumber.kotlin.io


expect interface Externalizable: Serializable {

    fun writeExternal(out: ObjectOutput)

    fun readExternal(input: ObjectInput)
}