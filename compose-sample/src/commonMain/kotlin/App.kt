package io.luca992.libphonenumber.sample

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import io.michaelrocks.libphonenumber.kotlin.PhoneNumberUtil

@Composable
fun App(util: PhoneNumberUtil, platform: String) {
    var text by remember { mutableStateOf("Hello, World!") }
    var examplePhoneNumberToFormat by remember { mutableStateOf("8005551212") }
    var examplePhoneNumberFormatted by remember { mutableStateOf(false) }
    MaterialTheme {
        Column(Modifier.fillMaxWidth()) {
            Button(onClick = {
                text = "Hello, $platform!"
            }) {
                Text(text)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = {
                    val phoneNumber = util.parse(examplePhoneNumberToFormat, "US")
                    examplePhoneNumberToFormat =
                        util.format(phoneNumber, PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL)
                    examplePhoneNumberFormatted = true
                }) {
                    Text(if (!examplePhoneNumberFormatted) "Click to format" else "Formatted")
                }
                Text("Phone number: $examplePhoneNumberToFormat")
            }
        }
    }
}
