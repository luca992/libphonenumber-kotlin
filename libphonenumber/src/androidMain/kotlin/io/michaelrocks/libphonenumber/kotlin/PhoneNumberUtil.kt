package io.michaelrocks.libphonenumber.kotlin

import android.content.Context
import io.michaelrocks.libphonenumber.kotlin.metadata.source.AssetsMetadataLoader

/**
 * Create a new [PhoneNumberUtil] instance to carry out international phone number
 * formatting, parsing, or validation. The instance is loaded with all metadata by
 * using the context specified.
 *
 *
 * This method should only be used in the rare case in which you want to manage your own
 * metadata loading. Calling this method multiple times is very expensive, as each time
 * a new instance is created from scratch.
 *
 * @param context  Android [Context] used to load metadata. This should not be null.
 * @return  a PhoneNumberUtil instance
 */
fun PhoneNumberUtil.Companion.createInstance(context: Context): PhoneNumberUtil {
    return createInstance(AssetsMetadataLoader(context.assets))
}