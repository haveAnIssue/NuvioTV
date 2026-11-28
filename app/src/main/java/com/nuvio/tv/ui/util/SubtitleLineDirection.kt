package com.nuvio.tv.ui.util

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDirection

private const val RIGHT_TO_LEFT_MARK = '\u200F'

/**
 * Makes every mostly-RTL line of a multi-line text lay out right-to-left, even when it starts with a Latin
 * word, a name or a brand ("John, בוא הנה"). A paragraph's direction normally follows its first strong
 * letter, which flows such a Hebrew sentence left-to-right in the wrong visual order. An invisible
 * right-to-left mark at the start of the line fixes that for that line only, so an English dialogue line
 * in the same subtitle ("- Hi") keeps its left-to-right layout. Styling spans are preserved.
 */
fun AnnotatedString.withRtlLineMarks(): AnnotatedString {
    // Fast path: nothing to do unless the text contains Hebrew/Arabic-range letters.
    if (text.none { it.code in 0x0590..0x08FF || it.code in 0xFB1D..0xFEFC }) return this
    return buildAnnotatedString {
        var start = 0
        while (start <= text.length) {
            val newline = text.indexOf('\n', start)
            val end = if (newline < 0) text.length else newline
            val line = subSequence(start, end)
            if (line.text.contentTextDirection() == TextDirection.Rtl) append(RIGHT_TO_LEFT_MARK)
            append(line)
            if (newline >= 0) append('\n')
            start = end + 1
        }
    }
}
