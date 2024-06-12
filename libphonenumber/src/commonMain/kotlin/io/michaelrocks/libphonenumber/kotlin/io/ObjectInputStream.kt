package io.michaelrocks.libphonenumber.kotlin.io

expect open class ObjectInputStream(inputStream: InputStream) : InputStream, ObjectInput, ObjectStreamConstants {
    override fun readFully(b: ByteArray?): Unit
    override fun readFully(b: ByteArray?, off: Int, len: Int): Unit
    override fun skipBytes(n: Int): Int
    override fun readBoolean(): Boolean
    override fun readByte(): Byte
    override fun readUnsignedByte(): Int
    override fun readShort(): Short
    override fun readUnsignedShort(): Int
    override fun readChar(): Char
    override fun readInt(): Int
    override fun readLong(): Long
    override fun readFloat(): Float
    override fun readDouble(): Double
    override fun readLine(): String
    override fun readUTF(): String
    override fun close()
}
