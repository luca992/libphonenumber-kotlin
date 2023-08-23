package io.luca992.libphonenumber.sample

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import io.michaelrocks.libphonenumber.kotlin.PhoneNumberUtil
import io.michaelrocks.libphonenumber.kotlin.metadata.defaultMetadataLoader

@Composable
internal expect fun context(): Any?

@Composable
fun phoneNumberUtil(): PhoneNumberUtil {
    val context = context()
    val phoneNumberUtil: PhoneNumberUtil by remember {
        mutableStateOf(
            PhoneNumberUtil.createInstance(
                defaultMetadataLoader(context)
            )
        )
    }
    return phoneNumberUtil
}