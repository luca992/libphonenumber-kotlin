package io.luca992.libphonenumber.sample

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.intl.Locale
import io.michaelrocks.libphonenumber.kotlin.PhoneNumberUtil

const val WILD: Char = 'N'
const val WAIT: Char = ';'
const val PAUSE: Char = ','

class PhoneNumberVisualTransformation(
    phoneNumberUtil: PhoneNumberUtil, countryCode: String = Locale.current.region
) : VisualTransformation {

    private val phoneNumberFormatter = phoneNumberUtil.getAsYouTypeFormatter(countryCode)

    override fun filter(text: AnnotatedString): TransformedText {
        val transformation = reformat(text, text.length)

        return TransformedText(
            AnnotatedString(transformation.formatted), object : OffsetMapping {
                override fun originalToTransformed(offset: Int): Int {
                    // safe lookup, fall back to last mapped index
                    return transformation.originalToTransformed.getOrElse(offset) {
                        transformation.originalToTransformed.last()
                    }
                }

                override fun transformedToOriginal(offset: Int): Int {
                    // safe lookup, fall back to last mapped index
                    return transformation.transformedToOriginal.getOrElse(offset) {
                        transformation.transformedToOriginal.last()
                    }
                }
            })
    }

    private fun isNonSeparator(c: Char): Boolean {
        return (c in '0'..'9') || c == '*' || c == '#' || c == '+' || c == WILD || c == WAIT || c == PAUSE
    }

    private fun reformat(s: CharSequence, cursor: Int): Transformation {
        phoneNumberFormatter.clear()

        val curIndex = cursor - 1
        var formatted: String? = null
        var lastNonSeparator = 0.toChar()
        var hasCursor = false

        s.forEachIndexed { index, char ->
            if (isNonSeparator(char)) {
                if (lastNonSeparator.code != 0) {
                    formatted = getFormattedNumber(lastNonSeparator, hasCursor)
                    hasCursor = false
                }
                lastNonSeparator = char
            }
            if (index == curIndex) {
                hasCursor = true
            }
        }

        if (lastNonSeparator.code != 0) {
            formatted = getFormattedNumber(lastNonSeparator, hasCursor)
        }
        val raw = formatted.orEmpty()
        val originalToTransformed = mutableListOf<Int>()
        val transformedToOriginal = mutableListOf<Int>()
        var originalIndex = 0

        raw.forEachIndexed { index, char ->
            if (isNonSeparator(char)) {
                // map each digit position
                if (originalIndex < s.length) {
                    originalToTransformed.add(index)
                    transformedToOriginal.add(originalIndex)
                    originalIndex++
                } else {
                    // fallback if somehow past end
                    originalToTransformed.add(index)
                    transformedToOriginal.add(s.length)
                }
            } else {
                // for separators, map back to the next original index
                transformedToOriginal.add(originalIndex.coerceAtMost(s.length))
            }
        }

        // map end-of-text
        originalToTransformed.add(raw.length)
        transformedToOriginal.add(s.length)

        return Transformation(raw, originalToTransformed, transformedToOriginal)
    }

    private fun getFormattedNumber(lastNonSeparator: Char, hasCursor: Boolean): String {
        return if (hasCursor) {
            phoneNumberFormatter.inputDigitAndRememberPosition(lastNonSeparator)
        } else {
            phoneNumberFormatter.inputDigit(lastNonSeparator)
        }
    }

    private data class Transformation(
        val formatted: String, val originalToTransformed: List<Int>, val transformedToOriginal: List<Int>
    )
}
