package com.anga9.seller.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.anga9.seller.data.model.LegalDataSource
import com.anga9.seller.data.model.LegalResult
import com.anga9.seller.data.model.PrivacyLangContent
import com.anga9.seller.data.model.TermsLangContent
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * LegalRepository - Phase 1 (Seller App)
 *
 * Strategy (priority order):
 *   1. SharedPreferences cache (valid for 24 hours)
 *   2. CDN fetch
 *   3. Bundled assets fallback
 *
 * Replace CDN_BASE_URL when web team provides actual domain.
 */
class LegalRepository(private val context: Context) {

    companion object {
        private const val TAG = "SellerLegalRepo"

        private const val CDN_BASE_URL = "https://anga9.com/assets/legal"
        private const val CDN_PRIVACY_URL = "$CDN_BASE_URL/privacy_android.json"
        private const val CDN_TERMS_URL   = "$CDN_BASE_URL/terms_android.json"

        private const val ASSET_PRIVACY = "legal/privacy_android.json"
        private const val ASSET_TERMS   = "legal/terms_android.json"

        private const val PREFS_LEGAL_CACHE  = "anga_seller_legal_cache"
        private const val CACHE_KEY_PRIVACY  = "cached_privacy_json"
        private const val CACHE_KEY_TERMS    = "cached_terms_json"
        private const val CACHE_TIME_PRIVACY = "cached_privacy_time"
        private const val CACHE_TIME_TERMS   = "cached_terms_time"

        private const val CACHE_TTL_MS = 24L * 60L * 60L * 1000L

        val SUPPORTED_LANGS = listOf(
            "en", "hi", "bn", "ta", "te", "mr", "kn", "pa", "gu", "ml", "ur"
        )

        val LANG_DISPLAY_NAMES = listOf(
            "English", "हिंदी", "বাংলা", "தமிழ்", "తెలుగు",
            "मराठी", "ಕನ್ನಡ", "ਪੰਜਾਬੀ", "ગુજરાતી", "മലയാളം", "اردو"
        )
    }

    private val gson = Gson()

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_LEGAL_CACHE, Context.MODE_PRIVATE)
    }

    fun getDeviceLang(): String {
        val raw = Locale.getDefault().language
        return if (raw in SUPPORTED_LANGS) raw else "en"
    }

    suspend fun getPrivacyContent(
        lang: String = getDeviceLang()
    ): LegalResult<PrivacyLangContent> = withContext(Dispatchers.IO) {
        val resolvedLang = if (lang in SUPPORTED_LANGS) lang else "en"

        val cachedJson = getCachedJson(CACHE_KEY_PRIVACY, CACHE_TIME_PRIVACY)
        if (cachedJson != null) {
            val parsed = parsePrivacy(cachedJson, resolvedLang)
            if (parsed != null) {
                Log.d(TAG, "Privacy from cache (lang=$resolvedLang)")
                return@withContext LegalResult(parsed, LegalDataSource.CACHE)
            }
        }

        val cdnJson = fetchFromCdn(CDN_PRIVACY_URL)
        if (cdnJson != null) {
            saveCachedJson(CACHE_KEY_PRIVACY, CACHE_TIME_PRIVACY, cdnJson)
            val parsed = parsePrivacy(cdnJson, resolvedLang)
            if (parsed != null) {
                Log.d(TAG, "Privacy from CDN (lang=$resolvedLang)")
                return@withContext LegalResult(parsed, LegalDataSource.CDN)
            }
        }

        val assetJson = loadAsset(ASSET_PRIVACY)
        if (assetJson != null) {
            val parsed = parsePrivacy(assetJson, resolvedLang)
            if (parsed != null) {
                Log.d(TAG, "Privacy from assets (lang=$resolvedLang)")
                return@withContext LegalResult(parsed, LegalDataSource.ASSETS)
            }
        }

        Log.e(TAG, "Privacy: all sources failed for lang=$resolvedLang")
        LegalResult(null, LegalDataSource.NONE, "Could not load Privacy Policy")
    }

    suspend fun getTermsContent(
        lang: String = getDeviceLang()
    ): LegalResult<TermsLangContent> = withContext(Dispatchers.IO) {
        val resolvedLang = if (lang in SUPPORTED_LANGS) lang else "en"

        val cachedJson = getCachedJson(CACHE_KEY_TERMS, CACHE_TIME_TERMS)
        if (cachedJson != null) {
            val parsed = parseTerms(cachedJson, resolvedLang)
            if (parsed != null) {
                Log.d(TAG, "Terms from cache (lang=$resolvedLang)")
                return@withContext LegalResult(parsed, LegalDataSource.CACHE)
            }
        }

        val cdnJson = fetchFromCdn(CDN_TERMS_URL)
        if (cdnJson != null) {
            saveCachedJson(CACHE_KEY_TERMS, CACHE_TIME_TERMS, cdnJson)
            val parsed = parseTerms(cdnJson, resolvedLang)
            if (parsed != null) {
                Log.d(TAG, "Terms from CDN (lang=$resolvedLang)")
                return@withContext LegalResult(parsed, LegalDataSource.CDN)
            }
        }

        val assetJson = loadAsset(ASSET_TERMS)
        if (assetJson != null) {
            val parsed = parseTerms(assetJson, resolvedLang)
            if (parsed != null) {
                Log.d(TAG, "Terms from assets (lang=$resolvedLang)")
                return@withContext LegalResult(parsed, LegalDataSource.ASSETS)
            }
        }

        Log.e(TAG, "Terms: all sources failed for lang=$resolvedLang")
        LegalResult(null, LegalDataSource.NONE, "Could not load Terms of Service")
    }

    suspend fun refreshFromCdn(): Boolean = withContext(Dispatchers.IO) {
        var success = false
        fetchFromCdn(CDN_PRIVACY_URL)?.let {
            saveCachedJson(CACHE_KEY_PRIVACY, CACHE_TIME_PRIVACY, it)
            success = true
        }
        fetchFromCdn(CDN_TERMS_URL)?.let {
            saveCachedJson(CACHE_KEY_TERMS, CACHE_TIME_TERMS, it)
            success = true
        }
        success
    }

    fun clearCache() {
        prefs.edit()
            .remove(CACHE_KEY_PRIVACY).remove(CACHE_TIME_PRIVACY)
            .remove(CACHE_KEY_TERMS).remove(CACHE_TIME_TERMS)
            .apply()
    }

    private fun fetchFromCdn(url: String): String? {
        return try {
            val request = Request.Builder()
                .url(url)
                .addHeader("Accept", "application/json")
                .addHeader("Cache-Control", "no-cache")
                .build()
            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) response.body?.string() else null
        } catch (e: Exception) {
            Log.w(TAG, "CDN fetch failed: ${e.message}")
            null
        }
    }

    private fun loadAsset(fileName: String): String? {
        return try {
            context.assets.open(fileName).bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            Log.w(TAG, "Asset load failed: $fileName")
            null
        }
    }

    private fun parsePrivacy(json: String, lang: String): PrivacyLangContent? {
        return try {
            val root = gson.fromJson(json, JsonObject::class.java)
            val langObj = root.get(lang) ?: root.get("en") ?: return null
            gson.fromJson(langObj, PrivacyLangContent::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Privacy parse error: ${e.message}")
            null
        }
    }

    private fun parseTerms(json: String, lang: String): TermsLangContent? {
        return try {
            val root = gson.fromJson(json, JsonObject::class.java)
            val langObj = root.get(lang) ?: root.get("en") ?: return null
            gson.fromJson(langObj, TermsLangContent::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Terms parse error: ${e.message}")
            null
        }
    }

    private fun getCachedJson(key: String, timeKey: String): String? {
        val cachedAt = prefs.getLong(timeKey, 0L)
        val isExpired = System.currentTimeMillis() - cachedAt > CACHE_TTL_MS
        if (isExpired) return null
        return prefs.getString(key, null)
    }

    private fun saveCachedJson(key: String, timeKey: String, json: String) {
        prefs.edit()
            .putString(key, json)
            .putLong(timeKey, System.currentTimeMillis())
            .apply()
    }
}
