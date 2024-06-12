package io.michaelrocks.libphonenumber.kotlin.io

expect abstract class InputStream: AutoCloseable {
    abstract override fun close(): kotlin.Unit
}
