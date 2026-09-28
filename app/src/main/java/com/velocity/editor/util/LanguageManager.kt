package com.velocity.editor.util

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

object LanguageManager {
    const val EN = "en"
    const val AR = "ar"

    fun current(): String {
        val tags = AppCompatDelegate.getApplicationLocales().toLanguageTags()
        val lang = if (tags.isNotEmpty()) tags else Locale.getDefault().language
        return if (lang.startsWith(AR)) AR else EN
    }

    fun set(language: String) {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language))
    }
}
