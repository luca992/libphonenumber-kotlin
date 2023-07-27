package io.michaelrocks.libphonenumber.kotlin.io

actual interface ObjectStreamConstants {
    /**
     * Magic number that is written to the stream header.
     */
    val STREAM_MAGIC: Short
        get() = 0xaced.toShort()

    /**
     * Version number that is written to the stream header.
     */
    val STREAM_VERSION: Short
        get() = 5

    /* Each item in the stream is preceded by a tag
     */

    /* Each item in the stream is preceded by a tag
     */
    /**
     * First tag value.
     */
    val TC_BASE: Byte
        get() = 0x70

    /**
     * Null object reference.
     */
    val TC_NULL: Byte
        get() = 0x70.toByte()

    /**
     * Reference to an object already written into the stream.
     */
    val TC_REFERENCE: Byte
        get() = 0x71.toByte()

    /**
     * new Class Descriptor.
     */
    val TC_CLASSDESC: Byte
        get() = 0x72.toByte()

    /**
     * new Object.
     */
    val TC_OBJECT: Byte
        get() = 0x73.toByte()

    /**
     * new String.
     */
    val TC_STRING: Byte
        get() = 0x74.toByte()

    /**
     * new Array.
     */
    val TC_ARRAY: Byte
        get() = 0x75.toByte()

    /**
     * Reference to Class.
     */
    val TC_CLASS: Byte
        get() = 0x76.toByte()

    /**
     * Block of optional data. Byte following tag indicates number
     * of bytes in this block data.
     */
    val TC_BLOCKDATA: Byte
        get() = 0x77.toByte()

    /**
     * End of optional block data blocks for an object.
     */
    val TC_ENDBLOCKDATA: Byte
        get() = 0x78.toByte()

    /**
     * Reset stream context. All handles written into stream are reset.
     */
    val TC_RESET: Byte
        get() = 0x79.toByte()

    /**
     * long Block data. The long following the tag indicates the
     * number of bytes in this block data.
     */
    val TC_BLOCKDATALONG: Byte
        get() = 0x7A.toByte()

    /**
     * Exception during write.
     */
    val TC_EXCEPTION: Byte
        get() = 0x7B.toByte()

    /**
     * Long string.
     */
    val TC_LONGSTRING: Byte
        get() = 0x7C.toByte()

    /**
     * new Proxy Class Descriptor.
     */
    val TC_PROXYCLASSDESC: Byte
        get() = 0x7D.toByte()

    /**
     * new Enum constant.
     * @since 1.5
     */
    val TC_ENUM: Byte
        get() = 0x7E.toByte()

    /**
     * Last tag value.
     */
    val TC_MAX: Byte
        get() = 0x7E.toByte()

    /**
     * First wire handle to be assigned.
     */
    val baseWireHandle: Int
        get() = 0x7e0000

}
