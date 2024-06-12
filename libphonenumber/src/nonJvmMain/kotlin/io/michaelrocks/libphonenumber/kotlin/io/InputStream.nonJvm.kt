package io.michaelrocks.libphonenumber.kotlin.io

import okio.BufferedSource

actual abstract class InputStream : AutoCloseable {
    actual abstract override fun close()
}

class OkioInputStream(val bufferedSource: BufferedSource) : InputStream() {

    override fun close() {
        bufferedSource.close()
    }
}
