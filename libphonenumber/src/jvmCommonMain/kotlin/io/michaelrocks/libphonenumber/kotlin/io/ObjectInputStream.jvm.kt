package io.michaelrocks.libphonenumber.kotlin.io


actual fun getPlatformObjectInputStream(inputStream: InputStream): ObjectInputStream {
    return JavaObjectInputStream(java.io.ObjectInputStream((inputStream as JavaInputStream).javaInputStream))
}

class JavaObjectInputStream(val objectInputStream: java.io.ObjectInputStream) : ObjectInputStream {
    override fun readObject(): Any? {
        return objectInputStream.readObject()
    }

    override fun read(): Int {
        return objectInputStream.read()
    }

    override fun read(b: ByteArray?): Int {
        return objectInputStream.read(b)
    }

    override fun read(b: ByteArray?, off: Int, len: Int): Int {
        return objectInputStream.read(b, off, len)
    }

    override fun skip(n: Long): Long {
        return objectInputStream.skip(n)
    }

    override fun available(): Int {
        return objectInputStream.available()
    }

    override fun close() {
        objectInputStream.close()
    }

    override fun readFully(b: ByteArray?) {
        objectInputStream.readFully(b)
    }

    override fun readFully(b: ByteArray?, off: Int, len: Int) {
        objectInputStream.readFully(b, off, len)
    }

    override fun skipBytes(n: Int): Int {
        return objectInputStream.skipBytes(n)
    }

    override fun readBoolean(): Boolean {
        return objectInputStream.readBoolean()
    }

    override fun readByte(): Byte {
        return objectInputStream.readByte()
    }

    override fun readUnsignedByte(): Int {
        return objectInputStream.readUnsignedByte()
    }

    override fun readShort(): Short {
        return objectInputStream.readShort()
    }

    override fun readUnsignedShort(): Int {
        return objectInputStream.readUnsignedShort()
    }

    override fun readChar(): Char {
        return objectInputStream.readChar()
    }

    override fun readInt(): Int {
        return objectInputStream.readInt()
    }

    override fun readLong(): Long {
        return objectInputStream.readLong()
    }

    override fun readFloat(): Float {
        return objectInputStream.readFloat()
    }

    override fun readDouble(): Double {
        return objectInputStream.readDouble()
    }

    override fun readLine(): String? {
        return objectInputStream.readLine()
    }

    override fun readUTF(): String? {
        return objectInputStream.readUTF()
    }
}