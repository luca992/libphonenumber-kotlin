package io.michaelrocks.libphonenumber.kotlin.io

class JavaInputStream(val javaInputStream: java.io.InputStream) : InputStream {

    override fun close() {
        javaInputStream.close()
    }
}
