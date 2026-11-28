package com.nuvio.tv.core.util

/**
 * Deprecated ISO 639-1 codes still emitted by older Java/Android APIs and some addons
 * (`Locale.getLanguage()` returns `iw`, `in`, `ji` on many Android versions) mapped to the current codes.
 */
private val LEGACY_LANGUAGE_CODES = mapOf("iw" to "he", "in" to "id", "ji" to "yi")

private val REGION_OR_SCRIPT_SUBTAG = Regex("[a-z]{2}|[a-z]{4}|\\d{3}")

/** Replaces a deprecated primary language subtag, keeping any region: `iw-IL` -> `he-IL`, `in` -> `id`. */
fun String.withModernLanguageCode(): String {
    val primary = substringBefore('-')
    val modern = LEGACY_LANGUAGE_CODES[primary.lowercase()] ?: return this
    return modern + substring(primary.length)
}

/**
 * Canonicalises a language tag that carries a region or script suffix by mapping only its primary
 * subtag through [aliases] (e.g. ISO 639-2 or legacy codes): `heb-il` -> `he-il`, `ara-sa` -> `ar-sa`.
 *
 * [lowercaseTag] must already be lower-case with `_` replaced by `-`. Returns null when the tag has no
 * suffix, the primary subtag has no plain alias, or the suffix is not a valid region/script, so
 * callers keep their own direct-match handling (such as `pt-br`) first.
 */
fun canonicalizeRegionalLanguageTag(lowercaseTag: String, aliases: Map<String, String>): String? {
    if (lowercaseTag in aliases) return null
    val parts = lowercaseTag.split('-')
    if (parts.size < 2) return null
    val canonical = (aliases[parts[0]] ?: LEGACY_LANGUAGE_CODES[parts[0]])?.lowercase() ?: return null
    if ('-' in canonical) return null
    val suffix = parts.drop(1)
    if (!suffix.all { REGION_OR_SCRIPT_SUBTAG.matches(it) }) return null
    return (listOf(canonical) + suffix).joinToString("-")
}

/** ISO 639-1 codes of languages that are normally written right-to-left. */
private val RTL_LANGUAGE_CODES = setOf("ar", "he", "fa", "ur", "yi", "ps", "sd", "ug", "dv", "ckb")

/** True when this language code (`he`, `iw`, `ar-EG`, `fa_IR`, ...) is a right-to-left language. */
fun String?.isRtlLanguageCode(): Boolean {
    val primary = this?.trim()?.lowercase()?.replace('_', '-')?.substringBefore('-')
        ?.takeIf { it.isNotEmpty() } ?: return false
    return primary.withModernLanguageCode() in RTL_LANGUAGE_CODES
}
