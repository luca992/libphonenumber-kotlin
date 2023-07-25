package io.michaelrocks.libphonenumber.kotlin.io

expect interface ObjectOutput : DataOutput, AutoCloseable {

    fun writeObject(obj: Any?)

    override fun write(b: Int)

    override fun write(b: ByteArray?)

    override fun write(b: ByteArray?, off: Int, len: Int)

    fun flush()

    override fun close()
}