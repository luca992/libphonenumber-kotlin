package io.luca992.libphonenumber.sample

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
internal actual fun context(): Any? {
    return LocalContext.current
}