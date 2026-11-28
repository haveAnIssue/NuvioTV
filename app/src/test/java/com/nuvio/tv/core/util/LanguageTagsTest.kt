package com.nuvio.tv.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LanguageTagsTest {
    private val aliases = mapOf("heb" to "he", "iw" to "he", "ara" to "ar", "por" to "pt", "pt-pt" to "pt", "pt-br" to "pt-BR")

    @Test
    fun `withModernLanguageCode replaces deprecated codes and keeps region`() {
        assertEquals("he", "iw".withModernLanguageCode())
        assertEquals("he-IL", "iw-IL".withModernLanguageCode())
        assertEquals("id", "in".withModernLanguageCode())
        assertEquals("yi", "ji".withModernLanguageCode())
        assertEquals("en-US", "en-US".withModernLanguageCode())
        assertEquals("", "".withModernLanguageCode())
    }

    @Test
    fun `canonicalize maps primary subtag and keeps region`() {
        assertEquals("he-il", canonicalizeRegionalLanguageTag("heb-il", aliases))
        assertEquals("he-il", canonicalizeRegionalLanguageTag("iw-il", aliases))
        assertEquals("ar-sa", canonicalizeRegionalLanguageTag("ara-sa", aliases))
        assertEquals("ar-latn", canonicalizeRegionalLanguageTag("ara-latn", aliases))
    }

    @Test
    fun `canonicalize leaves direct matches and unknown tags alone`() {
        assertNull(canonicalizeRegionalLanguageTag("pt-pt", aliases))
        assertNull(canonicalizeRegionalLanguageTag("pt-br", aliases))
        assertNull(canonicalizeRegionalLanguageTag("heb", aliases))
        assertNull(canonicalizeRegionalLanguageTag("en-us", aliases))
        assertNull(canonicalizeRegionalLanguageTag("heb-foo-bar", aliases))
    }

    @Test
    fun `isRtlLanguageCode recognises rtl languages and legacy codes`() {
        listOf("he", "iw", "HE", "ar", "ar-EG", "fa_IR", "ur", "yi").forEach {
            org.junit.Assert.assertTrue(it, it.isRtlLanguageCode())
        }
        listOf("en", "pt-BR", "hi", "", null).forEach {
            org.junit.Assert.assertFalse(it ?: "null", it.isRtlLanguageCode())
        }
    }
}
