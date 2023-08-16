package io.michaelrocks.libphonenumber.kotlin.utils

import kotlin.reflect.KClass
import kotlin.test.assertEquals

fun <T : Throwable> assertThrows(exceptionClass: KClass<T>, block: () -> Unit): Throwable? {
    var thrown: Throwable? = null
    try {
        block()
    } catch (e: Throwable) {
        thrown = e
    }

    assertEquals(
        exceptionClass,
        thrown?.let { it::class },
        "Expected exception of type ${exceptionClass.simpleName} but got ${thrown?.let { it::class.simpleName }}"
    )
    return thrown
}