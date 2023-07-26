package io.michaelrocks.libphonenumber.android.sample

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.widget.TextView
import io.michaelrocks.libphonenumber.kotlin.NumberParseException
import io.michaelrocks.libphonenumber.kotlin.PhoneNumberUtil
import io.michaelrocks.libphonenumber.kotlin.createInstance

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val textView = findViewById<TextView>(R.id.textView)
        findViewById<View>(R.id.button).setOnClickListener(object : View.OnClickListener {
            private var util: PhoneNumberUtil? = null
            override fun onClick(v: View) {
                if (util == null) {
                    util = PhoneNumberUtil.Companion.createInstance(applicationContext)
                }
                try {
                    val phoneNumber = util!!.parse("8005551212", "US")
                    textView.text = util!!.format(phoneNumber, PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL)
                } catch (e: NumberParseException) {
                    e.printStackTrace()
                }
            }
        })
    }
}
