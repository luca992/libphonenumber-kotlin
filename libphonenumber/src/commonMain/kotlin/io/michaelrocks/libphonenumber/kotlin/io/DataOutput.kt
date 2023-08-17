package io.michaelrocks.libphonenumber.kotlin.io

expect interface DataOutput {
    fun write(b: Int)

    fun write(b: ByteArray?)

    fun write(b: ByteArray?, off: Int, len: Int)

    fun writeBoolean(v: Boolean)

    fun writeByte(v: Int)

    fun writeShort(v: Int)

    fun writeChar(v: Int)

    fun writeInt(v: Int)

    fun writeLong(v: Long)

    fun writeFloat(v: Float)

    fun writeDouble(v: Double)

    fun writeBytes(s: String?)

    fun writeChars(s: String?)

    fun writeUTF(s: String?)
}