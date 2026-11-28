package com.nuvio.tv.ui.util

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import org.junit.Assert.assertEquals
import org.junit.Test

class SubtitleLineDirectionTest {
    private val rlm = "\u200F"

    @Test
    fun `latin only text is unchanged`() {
        val text = AnnotatedString("- Hi there\n- How are you?")
        assertEquals(text.text, text.withRtlLineMarks().text)
    }

    @Test
    fun `mostly hebrew line that starts with latin gets a mark`() {
        val result = AnnotatedString("John, בוא הנה").withRtlLineMarks().text
        assertEquals(rlm + "John, בוא הנה", result)
    }

    @Test
    fun `lines are handled independently`() {
        val result = AnnotatedString("- Hi\n- John, בוא הנה").withRtlLineMarks().text
        assertEquals("- Hi\n" + rlm + "- John, בוא הנה", result)
    }

    @Test
    fun `spans move with their text`() {
        val text = buildAnnotatedString {
            append("John, ")
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("בוא הנה") }
        }
        val result = text.withRtlLineMarks()
        val span = result.spanStyles.single()
        assertEquals("בוא הנה", result.text.substring(span.start, span.end))
    }
}
