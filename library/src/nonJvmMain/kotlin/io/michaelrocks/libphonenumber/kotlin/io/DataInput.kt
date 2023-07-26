package io.michaelrocks.libphonenumber.kotlin.io

actual interface DataInput {

    actual fun readFully(b: ByteArray?)

    actual fun readFully(b: ByteArray?, off: Int, len: Int)

    actual fun skipBytes(n: Int): Int

    actual fun readBoolean(): Boolean

    actual fun readByte(): Byte

    actual fun readUnsignedByte(): Int

    actual fun readShort(): Short


    actual fun readUnsignedShort(): Int

    actual fun readChar(): Char

    actual fun readInt(): Int

    actual fun readLong(): Long

    actual fun readFloat(): Float

    actual fun readDouble(): Double

    actual fun readLine(): String

    actual fun readUTF(): String
}