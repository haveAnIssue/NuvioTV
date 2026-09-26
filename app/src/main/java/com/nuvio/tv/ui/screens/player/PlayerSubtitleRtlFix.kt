@file:OptIn(androidx.media3.common.util.UnstableApi::class)

package com.nuvio.tv.ui.screens.player

import android.text.BidiFormatter
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextDirectionHeuristics
import android.text.TextUtils
import androidx.media3.common.C
import androidx.media3.common.text.Cue
import androidx.media3.common.util.UnstableApi
import androidx.media3.extractor.text.CuesWithTiming

internal object PlayerSubtitleRtlFix {

    private fun bidiFormatter(isContainerRtl: Boolean) = BidiFormatter.getInstance(isContainerRtl)

    fun fixCueText(cue: Cue, isBuiltInSubtitle: Boolean, isContainerRtl: Boolean = false): Cue {
        val text = cue.text ?: return cue
        if (!hasAnyStrongRtlCharacter(text)) return cue

        val fixed = wrapLinesForLtrContainer(text, isContainerRtl) ?: return cue
        return cue.buildUpon().setText(fixed).build()
    }

    fun fixTimedCues(
        cues: List<CuesWithTiming>,
        isBuiltInSubtitle: Boolean = false,
        isContainerRtl: Boolean = false
    ): List<CuesWithTiming> {
        if (cues.isEmpty()) return cues
        var anyChanged = false
        val out = ArrayList<CuesWithTiming>(cues.size)
        for (entry in cues) {
            val entryCues = entry.cues
            var modified: ArrayList<Cue>? = null
            for (i in entryCues.indices) {
                val original = entryCues[i]
                val fixed = fixCueText(original, isBuiltInSubtitle, isContainerRtl)
                if (fixed !== original) {
                    if (modified == null) {
                        modified = ArrayList(entryCues.size)
                        for (j in 0 until i) {
                            modified.add(entryCues[j])
                        }
                    }
                    modified.add(fixed)
                } else {
                    modified?.add(original)
                }
            }
            if (modified != null) {
                anyChanged = true
                out.add(copyTimedCues(entry, modified))
            } else {
                out.add(entry)
            }
        }
        return if (anyChanged) out else cues
    }

    private fun copyTimedCues(entry: CuesWithTiming, cues: List<Cue>): CuesWithTiming {
        val durationUs = when {
            entry.durationUs != C.TIME_UNSET -> entry.durationUs
            entry.endTimeUs != C.TIME_UNSET && entry.startTimeUs != C.TIME_UNSET ->
                (entry.endTimeUs - entry.startTimeUs).coerceAtLeast(1L)
            else -> 5_000_000L
        }
        return CuesWithTiming(cues, entry.startTimeUs, durationUs)
    }

    private fun wrapLinesForLtrContainer(text: CharSequence, isContainerRtl: Boolean): CharSequence? {
        val preserveSpans = text is Spanned
        val lines = text.splitByNewlines()
        var changed = false
        val formatter = bidiFormatter(isContainerRtl)
        val out: Appendable =
            if (preserveSpans) SpannableStringBuilder() else StringBuilder(text.length + 8)
        for (i in lines.indices) {
            if (i > 0) out.append('\n')
            var line = lines[i]
            if (line.isEmpty()) continue
            val fixed = fixBoundaryPunctuation(line)
            if (fixed != null) {
                line = fixed
                changed = true
            }
            val wrapped = formatter.unicodeWrap(line, TextDirectionHeuristics.ANYRTL_LTR, true) ?: line
            if (wrapped !== line) changed = true
            out.append(wrapped)
        }
        if (!changed) return null
        return finishBuilder(out)
    }

    private val TERMINAL_PUNCTUATION = charArrayOf('.', ',', ':', ';', '!', '?')
    private fun isTerminalPunctuationChar(c: Char) = TERMINAL_PUNCTUATION.contains(c)

    private val DASH_CHARS = charArrayOf('-', '\u2013', '\u2014')
    private fun isDashChar(c: Char) = DASH_CHARS.contains(c)

    private fun fixBoundaryPunctuation(line: CharSequence): CharSequence? {
        val firstRtl = indexOfFirstStrongRtl(line) ?: return null
        val lastRtl = indexOfLastStrongRtl(line)
        val leading = line.subSequence(0, firstRtl)
        val trailing = line.subSequence(lastRtl + 1, line.length)
        val middle = line.subSequence(firstRtl, lastRtl + 1)

        val leadingTrimmed = leading.trim()
        val leadingQualifies = leadingTrimmed.isNotEmpty() &&
            leadingTrimmed.length <= 2 &&
            leadingTrimmed.all { isTerminalPunctuationChar(it) } &&
            !(leadingTrimmed.all { it == '.' } && leadingTrimmed.length >= 2)

        val trailingTrimmed = trailing.trim()
        val trailingQualifies = trailingTrimmed.length == 1 && isDashChar(trailingTrimmed[0])

        if (!leadingQualifies && !trailingQualifies) return null

        val newTrailingSuffix: CharSequence = if (leadingQualifies) reverseCharSequence(leadingTrimmed) else ""
        val newLeadingPrefix: CharSequence = if (trailingQualifies) reverseCharSequence(trailing) else ""
        val keptLeading: CharSequence = if (leadingQualifies) "" else leading
        val keptTrailing: CharSequence = if (trailingQualifies) "" else trailing

        return TextUtils.concat(newLeadingPrefix, keptLeading, middle, keptTrailing, newTrailingSuffix)
    }

    private fun indexOfFirstStrongRtl(text: CharSequence): Int? {
        var i = 0
        while (i < text.length) {
            val codePoint = Character.codePointAt(text, i)
            val d = Character.getDirectionality(codePoint)
            if (d == Character.DIRECTIONALITY_RIGHT_TO_LEFT || d == Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC) {
                return i
            }
            i += Character.charCount(codePoint)
        }
        return null
    }

    private fun indexOfLastStrongRtl(text: CharSequence): Int {
        var i = 0
        var last = -1
        while (i < text.length) {
            val codePoint = Character.codePointAt(text, i)
            val d = Character.getDirectionality(codePoint)
            if (d == Character.DIRECTIONALITY_RIGHT_TO_LEFT || d == Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC) {
                last = i
            }
            i += Character.charCount(codePoint)
        }
        return last
    }

    private fun reverseCharSequence(cs: CharSequence): CharSequence {
        if (cs.isEmpty()) return cs
        return if (cs is Spanned) {
            val builder = SpannableStringBuilder()
            for (i in cs.length - 1 downTo 0) builder.append(cs.subSequence(i, i + 1))
            builder
        } else {
            StringBuilder(cs).reverse().toString()
        }
    }

    private fun finishBuilder(builder: Appendable): CharSequence = when (builder) {
        is SpannableStringBuilder -> builder
        is StringBuilder -> builder.toString()
        else -> builder.toString()
    }

    private fun CharSequence.splitByNewlines(): List<CharSequence> {
        val result = mutableListOf<CharSequence>()
        var start = 0
        var i = 0
        while (i < this.length) {
            if (this[i] == '\n') {
                result.add(this.subSequence(start, i))
                start = i + 1
            }
            i++
        }
        result.add(this.subSequence(start, this.length))
        return result
    }

    private fun hasAnyStrongRtlCharacter(text: CharSequence): Boolean {
        var i = 0
        val len = text.length
        while (i < len) {
            val codePoint = Character.codePointAt(text, i)
            if (codePoint >= 0x0590) {
                if (codePoint in 0x0590..0x08FF ||
                    codePoint in 0xFB1D..0xFEFF
                ) {
                    return true
                }
                val d = Character.getDirectionality(codePoint)
                if (d == Character.DIRECTIONALITY_RIGHT_TO_LEFT ||
                    d == Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC ||
                    d == Character.DIRECTIONALITY_ARABIC_NUMBER
                ) {
                    return true
                }
            }
            i += Character.charCount(codePoint)
        }
        return false
    }
}
