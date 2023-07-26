package io.luca992.libphonenumber.sample

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.remember
import io.michaelrocks.libphonenumber.kotlin.PhoneNumberUtil
import io.michaelrocks.libphonenumber.kotlin.createInstance

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val util = remember { PhoneNumberUtil.createInstance(applicationContext) }
            App(util, "Android")
        }
    }
}
