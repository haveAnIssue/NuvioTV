package com.nuvio.tv.core.server

import android.content.Context
import android.text.TextUtils
import android.view.View
import java.util.Locale

/**
 * The phone configuration pages are served in the app's language, so their `<html>` element must carry
 * that language and reading direction: Hebrew/Arabic pages render right-to-left (text alignment, flex rows,
 * list order) instead of as a left-to-right English document.
 */
internal object WebPageDirection {
    private fun locale(context: Context?): Locale {
        if (context == null) return Locale.ENGLISH
        val tag = context.getSharedPreferences("app_locale", Context.MODE_PRIVATE)
            .getString("locale_tag", null)
        return if (!tag.isNullOrEmpty()) {
            Locale.forLanguageTag(tag)
        } else {
            context.resources.configuration.locales[0]
        }
    }

    /** Attributes for the `<html>` tag, e.g. `lang="he" dir="rtl"`. */
    fun htmlAttributes(context: Context?): String {
        val locale = locale(context)
        val rtl = TextUtils.getLayoutDirectionFromLocale(locale) == View.LAYOUT_DIRECTION_RTL
        return "lang=\"${locale.toLanguageTag()}\" dir=\"${if (rtl) "rtl" else "ltr"}\""
    }

    /**
     * Lets every text field pick its own direction from what is typed, so a URL or an English name stays
     * left-to-right inside a right-to-left page and Hebrew text aligns to the right. Also covers fields
     * that the pages create later.
     */
    const val AUTO_DIRECTION_SCRIPT = """<script>
(function () {
  var SEL = 'input:not([type=checkbox]):not([type=radio]):not([type=range]):not([type=file]):not([type=color]),textarea';
  function mark(root) {
    if (!root.querySelectorAll) return;
    root.querySelectorAll(SEL).forEach(function (e) {
      if (!e.hasAttribute('dir')) e.setAttribute('dir', 'auto');
    });
  }
  mark(document);
  new MutationObserver(function (records) {
    records.forEach(function (r) {
      r.addedNodes.forEach(function (n) {
        if (n.nodeType !== 1) return;
        if (n.matches && n.matches(SEL) && !n.hasAttribute('dir')) n.setAttribute('dir', 'auto');
        mark(n);
      });
    });
  }).observe(document.documentElement, { childList: true, subtree: true });
})();
</script>"""
}
