package io.luca992.libphonenumber.sample

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.intl.Locale
import io.michaelrocks.libphonenumber.kotlin.PhoneNumberUtil
import io.michaelrocks.libphonenumber.kotlin.metadata.defaultMetadataLoader

@Composable
fun App(platform: String) {
    val phoneNumberUtil = phoneNumberUtil()
    var text by remember { mutableStateOf("Hello, World!") }
    var examplePhoneNumberToFormat by remember { mutableStateOf("8005551212") }
    var examplePhoneNumberFormatted by remember { mutableStateOf(false) }
    var asYouTypeFormatterText by remember { mutableStateOf("") }
    val region = remember {
        try {
            Locale.current.region
        } catch (e: Exception) {
            // as of compose 1.4.3, js fails to get the region so default to US
            "US"
        }
    }
    MaterialTheme {
        Column(Modifier.fillMaxWidth()) {
            Button(onClick = {
                text = "Hello, $platform!"
            }) {
                Text(text)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = {
                    val phoneNumber = phoneNumberUtil.parse(examplePhoneNumberToFormat, region)
                    examplePhoneNumberToFormat =
                        phoneNumberUtil.format(phoneNumber, PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL)
                    examplePhoneNumberFormatted = true
                }) {
                    Text(if (!examplePhoneNumberFormatted) "Click to format" else "Formatted")
                }
                Text("Phone number: $examplePhoneNumberToFormat")
            }
            Row {
                OutlinedTextField(
                    value = asYouTypeFormatterText,
                    onValueChange = { text: String ->
                        asYouTypeFormatterText = text
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("AsYouTypeFormatter Input") },
                    singleLine = true,
                    visualTransformation = PhoneNumberVisualTransformation(phoneNumberUtil, region),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )
            }
        }
    }
}
