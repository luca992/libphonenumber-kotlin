package io.michaelrocks.libphonenumber.kotlin.io

import okio.BufferedSource


class OkioInputStream(val bufferedSource: BufferedSource) : InputStream {

    override fun close() {
        bufferedSource.close()
    }
}
