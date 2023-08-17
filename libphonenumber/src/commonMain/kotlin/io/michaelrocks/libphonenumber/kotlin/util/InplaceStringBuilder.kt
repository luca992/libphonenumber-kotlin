package io.michaelrocks.libphonenumber.kotlin.util

class InplaceStringBuilder(private var builder: StringBuilder) : Appendable, CharSequence {

    constructor(charSequence: CharSequence) : this(StringBuilder(charSequence))

    constructor(text: String) : this(StringBuilder(text))

    constructor(count: Int) : this(StringBuilder(count))

    constructor() : this(StringBuilder())

    override val length: Int
        get() = builder.length

    override fun get(index: Int): Char {
        return builder[index]
    }


    override fun subSequence(startIndex: Int, endIndex: Int): CharSequence {
        return builder.subSequence(startIndex, endIndex)
    }

    override fun append(value: Char): StringBuilder {
        builder = builder.append(value)
        return builder
    }

    override fun append(value: CharSequence?): StringBuilder {
        builder = builder.append(value)
        return builder
    }

    override fun append(value: CharSequence?, startIndex: Int, endIndex: Int): StringBuilder {
        builder = builder.append(value, startIndex, endIndex)
        return builder
    }

    fun append(value: Boolean): StringBuilder {
        builder = builder.append(value)
        return builder
    }

    fun append(value: CharArray): StringBuilder {
        builder = builder.append(value)
        return builder
    }

    fun append(value: String?): StringBuilder {
        builder = builder.append(value)
        return builder
    }

    fun append(value: Any?): StringBuilder {
        builder = builder.append(value)
        return builder
    }

    fun append(vararg values: Any?): StringBuilder {
        for (item in values)
            builder.append(item)
        return builder
    }


    fun removeRange(startIndex: Int, endIndex: Int): CharSequence {
        builder = StringBuilder(builder.removeRange(startIndex, endIndex))
        return builder
    }

    fun replaceRange(startIndex: Int, endIndex: Int, replacement: CharSequence): CharSequence {
        builder = StringBuilder(builder.replaceRange(startIndex, endIndex, replacement))
        return builder
    }

    fun insert(index: Int, value: Boolean): StringBuilder {
        builder = builder.insert(index, value)
        return builder
    }

    fun insert(index: Int, value: Char): StringBuilder {
        builder = builder.insert(index, value)
        return builder
    }

    fun insert(index: Int, value: CharArray): StringBuilder {
        builder = builder.insert(index, value)
        return builder
    }

    fun insert(index: Int, value: CharSequence?): StringBuilder {
        builder = builder.insert(index, value)
        return builder
    }

    fun insert(index: Int, value: Any?): StringBuilder {
        builder = builder.insert(index, value)
        return builder
    }

    fun insert(index: Int, value: String?): StringBuilder {
        builder = builder.insert(index, value)
        return builder
    }

    fun setLength(newLength: Int) {
        builder.setLength(newLength)
    }

    override fun toString(): String {
        return builder.toString()
    }
}