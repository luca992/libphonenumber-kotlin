/*
 * Copyright (c) 1995, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package io.michaelrocks.libphonenumber.kotlin.io

import okio.IOException


/**
 * The `DataOutput` interface provides
 * for converting data from any of the Java
 * primitive types to a series of bytes and
 * writing these bytes to a binary stream.
 * There is  also a facility for converting
 * a `String` into
 * [modified UTF-8](DataInput.html#modified-utf-8)
 * format and writing the resulting series
 * of bytes.
 *
 *
 * For all the methods in this interface that
 * write bytes, it is generally true that if
 * a byte cannot be written for any reason,
 * an `IOException` is thrown.
 *
 * @author  Frank Yellin
 * @see java.io.DataInput
 *
 * @see java.io.DataOutputStream
 *
 * @since   1.0
 */
actual interface DataOutput {
    actual fun write(b: Int)
    actual fun write(b: ByteArray?)
    actual fun write(b: ByteArray?, off: Int, len: Int)
    actual fun writeBoolean(v: Boolean)
    actual fun writeByte(v: Int)
    actual fun writeShort(v: Int)
    actual fun writeChar(v: Int)
    actual fun writeInt(v: Int)
    actual fun writeLong(v: Long)
    actual fun writeFloat(v: Float)
    actual fun writeDouble(v: Double)
    actual fun writeBytes(s: String?)
    actual fun writeChars(s: String?)
    actual fun writeUTF(s: String?)

}