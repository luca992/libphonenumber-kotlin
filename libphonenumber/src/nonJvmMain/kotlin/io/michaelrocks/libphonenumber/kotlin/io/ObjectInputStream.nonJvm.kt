package io.michaelrocks.libphonenumber.kotlin.io

import io.michaelrocks.libphonenumber.kotlin.util.InplaceStringBuilder
import okio.BufferedSource
import okio.EOFException
import okio.IOException
import kotlin.math.min

// Based on BlockDataInputStream
actual open class ObjectInputStream actual constructor(val inputStream: InputStream) : InputStream(), ObjectInput,
    ObjectStreamConstants {

    val bufferedSource: BufferedSource
        get() = (inputStream as OkioInputStream).bufferedSource

    private val MAX_BLOCK_SIZE = 1024
    private val MAX_HEADER_SIZE = 5
    private val CHAR_BUF_SIZE = 256

    private val buf = ByteArray(MAX_BLOCK_SIZE)
    private val hbuf = ByteArray(MAX_HEADER_SIZE)
    private val cbuf = CharArray(CHAR_BUF_SIZE)

    /** current offset into buf  */
    private var pos = 0

    /** end offset of valid data in buf, or -1 if no more block data  */
    private var end = 0

    /** number of bytes in current block yet to be read from stream  */
    private var unread = 0

    init {
        readStreamHeader()
    }

    private fun readStreamHeader() {
        val s0: Short = (inputStream as OkioInputStream).bufferedSource.readShort()
        val s1: Short = inputStream.bufferedSource.readShort()
        if (s0 != STREAM_MAGIC || s1 != STREAM_VERSION) {
            throw Error("invalid stream header:  $s0, $s1")
        }
    }

    private fun getShortFromByteArray(byteArray: ByteArray, startIndex: Int): Short {
        return ((byteArray[startIndex].toInt() and 0xFF) shl 8 or
                (byteArray[startIndex + 1].toInt() and 0xFF)).toShort()
    }

    private fun getIntFromByteArray(byteArray: ByteArray, startIndex: Int): Int {
        return ((byteArray[startIndex].toInt() and 0xFF) shl 24) or ((byteArray[startIndex + 1].toInt() and 0xFF) shl 16) or ((byteArray[startIndex + 2].toInt() and 0xFF) shl 8) or (byteArray[startIndex + 3].toInt() and 0xFF)
    }

    /**
     * Attempts to read in the next block data header (if any).  If
     * canBlock is false and a full header cannot be read without possibly
     * blocking, returns HEADER_BLOCKED, else if the next element in the
     * stream is a block data header, returns the block data length
     * specified by the header, else returns -1.
     */
    private fun readBlockHeader(): Int {
        try {
            while (true) {
                val tc = bufferedSource.peek().readByte()
                when (tc) {
                    TC_BLOCKDATA -> {
                        bufferedSource.read(hbuf, 0, 2)
                        return hbuf[1].toInt() and 0xFF
                    }

                    TC_BLOCKDATALONG -> {
                        bufferedSource.read(hbuf, 0, 5)
                        val len = getIntFromByteArray(hbuf, 1)
                        if (len < 0) {
                            throw Exception("illegal block data header length: " + len)
                        }
                        return len
                    }

                    TC_RESET -> {
                        bufferedSource.readByte()
//                        handleReset()
                    }

                    else -> {
                        if (tc >= 0 && (tc < TC_BASE || tc > TC_MAX)) {
                            throw Exception("invalid type code: $tc")
                        }
                        return -1
                    }
                }
            }
        } catch (ex: EOFException) {
            throw EOFException(
                "unexpected EOF while reading block data header"
            )
        }
    }

    /**
     * Refills internal buffer buf with block data.  Any data in buf at the
     * time of the call is considered consumed.  Sets the pos, end, and
     * unread fields to reflect the new amount of available block data; if
     * the next element in the stream is not a data block, sets pos and
     * unread to 0 and end to -1.
     */
    private fun refill() {
        try {
            do {
                pos = 0
                if (unread > 0) {
                    val n: Int = bufferedSource.read(
                        buf, 0, min(unread.toDouble(), MAX_BLOCK_SIZE.toDouble()).toInt()
                    )
                    if (n >= 0) {
                        end = n
                        unread -= n
                    } else {
                        throw Error("unexpected EOF in middle of data block")
                    }
                } else {
                    val n: Int = readBlockHeader()
                    if (n >= 0) {
                        end = 0
                        unread = n
                    } else {
                        end = -1
                        unread = 0
                    }
                }
            } while (pos == end)
        } catch (ex: IOException) {
            pos = 0
            end = -1
            unread = 0
            throw ex
        }
    }


    fun read(b: ByteArray, off: Int, len: Int, copy: Boolean): Int {
        return if (len == 0) {
            0
        } else {
            if (pos == end) {
                refill()
            }
            if (end < 0) {
                return -1
            }
            val nread = min(len.toDouble(), (end - pos).toDouble()).toInt()
            buf.copyInto(b, off, pos, pos + nread)
            pos += nread
            nread
        }
    }


    fun read(): Int {
        if (pos == end) {
            refill()
        }
        return if (end >= 0) buf[pos++].toInt() and 0xFF else -1
    }

    actual override fun readFully(b: ByteArray?) {
        b?.let { bufferedSource.readFully(it) }
    }

    actual override fun readFully(b: ByteArray?, off: Int, len: Int) {
        throw Exception("readFully: Not yet implemented")
    }

    actual override fun skipBytes(n: Int): Int {
        throw Exception("skipBytes: Not yet implemented")
    }

    actual override fun readBoolean(): Boolean {
        val v = read()
        if (v < 0) {
            throw EOFException()
        }
        return v != 0
    }

    actual override fun readByte(): Byte {
        val v = read()
        if (v < 0) {
            throw EOFException()
        }
        return v.toByte()
    }

    actual override fun readUnsignedByte(): Int {
        throw Exception("readUnsignedByte: Not yet implemented")
    }

    actual override fun readShort(): Short {
        throw Exception("readShort: Not yet implemented")
    }

    actual override fun readUnsignedShort(): Int {
        if (end - pos < 2) {
            val ch1 = read()
            val ch2 = read()
            if (ch1 or ch2 < 0) throw EOFException()
            return (ch1 shl 8) + (ch2 shl 0)
        }
        val v: Int = getShortFromByteArray(buf, pos).toInt() and 0xFFFF
        pos += 2
        return v
    }

    actual override fun readChar(): Char {
        throw Exception("readChar: Not yet implemented")
    }

    actual override fun readInt(): Int {
        if (end - pos < 4) {
            val ch1 = read()
            val ch2 = read()
            val ch3 = read()
            val ch4 = read()
            if (ch1 or ch2 or ch3 or ch4 < 0) throw EOFException()
            return (ch1 shl 24) + (ch2 shl 16) + (ch3 shl 8) + (ch4 shl 0)
        }
        val v: Int = getIntFromByteArray(buf, pos)
        pos += 4
        return v
    }

    actual override fun readLong(): Long {
        throw Exception("readLong: Not yet implemented")
    }

    actual override fun readFloat(): Float {
        throw Exception("readFloat: Not yet implemented")
    }

    actual override fun readDouble(): Double {
        throw Exception("readDouble: Not yet implemented")
    }

    actual override fun readLine(): String {
        throw Exception("readLine: Not yet implemented")
    }

    actual override fun readUTF(): String {
        val length = readUnsignedShort().toLong()
        return readUTFBody(length)
    }

    private fun readUTFBody(utflen: Long): String {
        var utflen = utflen
        val sbuf: InplaceStringBuilder = if (utflen > 0 && utflen < Int.MAX_VALUE) {
            // a reasonable initial capacity based on the UTF length
            val initialCapacity = min(utflen.toInt().toDouble(), 0xFFFF.toDouble()).toInt()
            InplaceStringBuilder(initialCapacity)
        } else {
            InplaceStringBuilder()
        }
        while (utflen > 0) {
            val avail = end - pos
            if (avail >= 3 || avail.toLong() == utflen) {
                utflen -= readUTFSpan(sbuf, utflen)
            } else {
                // near block boundary, read one byte at a time
                utflen -= readUTFChar(sbuf, utflen).toLong()
            }
        }
        return sbuf.toString()
    }

    private fun readUTFSpan(sbuf: InplaceStringBuilder, utflen: Long): Long {
        var cpos = 0
        val start = pos
        val avail = min((end - pos).toDouble(), CHAR_BUF_SIZE.toDouble()).toInt()
        // stop short of last char unless all of utf bytes in buffer
        val stop = pos + if (utflen > avail) avail - 2 else utflen.toInt()
        var outOfBounds = false
        try {
            while (pos < stop) {
                var b1: Int
                var b2: Int
                var b3: Int
                b1 = buf[pos++].toInt() and 0xFF
                when (b1 shr 4) {
                    0, 1, 2, 3, 4, 5, 6, 7 -> {
                        // 1 byte format: 0xxxxxxx
                        cbuf[cpos++] = b1.toChar()
                    }

                    12, 13 -> {
                        // 2 byte format: 110xxxxx 10xxxxxx
                        b2 = buf[pos++].toInt()
                        if (b2 and 0xC0 != 0x80) {
                            throw Exception("UTFDataFormatException")
                        }
                        cbuf[cpos++] = (b1 and 0x1F shl 6 or (b2 and 0x3F shl 0)).toChar()
                    }

                    14 -> {
                        // 3 byte format: 1110xxxx 10xxxxxx 10xxxxxx
                        b3 = buf[pos + 1].toInt()
                        b2 = buf[pos + 0].toInt()
                        pos += 2
                        if (b2 and 0xC0 != 0x80 || b3 and 0xC0 != 0x80) {
                            440123
                            throw Exception("UTFDataFormatException")
                        }
                        cbuf[cpos++] = (b1 and 0x0F shl 12 or
                                (b2 and 0x3F shl 6) or
                                (b3 and 0x3F shl 0)).toChar()
                    }

                    else -> throw Exception("UTFDataFormatException")// 10xx xxxx, 1111 xxxx
                }
            }
        } catch (ex: IndexOutOfBoundsException) {
            outOfBounds = true
        } finally {
            if (outOfBounds || pos - start > utflen) {
                /*
                     * Fix for 4450867: if a malformed utf char causes the
                     * conversion loop to scan past the expected end of the utf
                     * string, only consume the expected number of utf bytes.
                     */
                pos = start + utflen.toInt()
                throw Exception("UTFDataFormatException")
            }
        }
        sbuf.append(cbuf.joinToString(""), 0, cpos)
        return (pos - start).toLong()
    }

    private fun readUTFChar(sbuf: InplaceStringBuilder, utflen: Long): Int {
        val b1: Int
        val b2: Int
        val b3: Int
        b1 = readByte().toInt() and 0xFF
        return when (b1 shr 4) {
            0, 1, 2, 3, 4, 5, 6, 7 -> {
                // 1 byte format: 0xxxxxxx
                sbuf.append(b1.toChar())
                1
            }

            12, 13 -> {
                // 2 byte format: 110xxxxx 10xxxxxx
                if (utflen < 2) {
                    throw Exception("UTFDataFormatException")
                }
                b2 = readByte().toInt()
                if (b2 and 0xC0 != 0x80) {
                    throw Exception("UTFDataFormatException")
                }
                sbuf.append((b1 and 0x1F shl 6 or (b2 and 0x3F shl 0)).toChar())
                2
            }

            14 -> {
                // 3 byte format: 1110xxxx 10xxxxxx 10xxxxxx
                if (utflen < 3) {
                    if (utflen == 2L) {
                        readByte() // consume remaining byte
                    }
                    throw Exception("UTFDataFormatException")
                }
                b2 = readByte().toInt()
                b3 = readByte().toInt()
                if (b2 and 0xC0 != 0x80 || b3 and 0xC0 != 0x80) {
                    throw Exception("UTFDataFormatException")
                }
                sbuf.append(
                    (b1 and 0x0F shl 12 or (b2 and 0x3F shl 6) or (b3 and 0x3F shl 0)).toChar()
                )
                3
            }

            else -> throw Exception("UTFDataFormatException") // 10xx xxxx, 1111 xxxx
        }
    }

    actual override fun close() {
        inputStream.close()
    }

}
