package com.anga9.seller.utils

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

/**
 * Utility object for managing app-wide locale changes.
 * Persists language selection to SharedPrefs and applies it to every Activity context.
 */
object LocaleHelper {

    private const val PREFS_NAME = "anga_seller_prefs"
    private const val KEY_LANGUAGE = "app_language"
    private const val DEFAULT_LANGUAGE = "en"

    /**
     * Save language code and return a new context with the locale applied.
     * Call this when user selects a language.
     */
    fun setLocale(context: Context, languageCode: String): Context {
        saveLanguage(context, languageCode)
        return applyLocale(context, languageCode)
    }

    /**
     * Read saved language and return a new context with the locale applied.
     * Call this in attachBaseContext() of every Activity and Application.
     */
    fun applyPersistedLocale(context: Context): Context {
        val lang = getSavedLanguage(context)
        return applyLocale(context, lang)
    }

    /** Returns the currently saved language code (default: "en") */
    fun getSavedLanguage(context: Context): String {
        return context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LANGUAGE, DEFAULT_LANGUAGE) ?: DEFAULT_LANGUAGE
    }

    /** Returns true if the user has already selected a language */
    fun isLanguageSelected(context: Context): Boolean {
        return context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .contains(KEY_LANGUAGE)
    }

    private fun saveLanguage(context: Context, languageCode: String) {
        context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LANGUAGE, languageCode)
            .commit()
    }

    private fun applyLocale(context: Context, languageCode: String): Context {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val res = context.resources
        val config = Configuration(res.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        @Suppress("DEPRECATION")
        res.updateConfiguration(config, res.displayMetrics)
        return context.createConfigurationContext(config)
    }
}
