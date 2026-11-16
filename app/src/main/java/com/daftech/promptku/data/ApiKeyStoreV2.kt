package com.daftech.promptku.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.daftech.promptku.engine.QuotaCounts
import com.daftech.promptku.engine.V2Provider

/**
 * Penyimpanan kunci API v2.0 (BYOK) + kuota harian.
 * Pola SAMA PERSIS dengan UnduhKu ApiKeyStore (terbukti): EncryptedSharedPreferences
 * (AES256-SIV kunci, AES256-GCM nilai, MasterKey di AndroidKeyStore).
 * Kalau KeyStore bermasalah -> fallback SharedPreferences biasa + isEncrypted=false
 * (UI menampilkan peringatan, jangan pernah crash).
 *
 * ATURAN KEAMANAN: kunci TIDAK PERNAH hardcode, TIDAK PERNAH di-log
 * (bahkan sebagian), TIDAK PERNAH dikirim kecuali ke provider yang
 * dipilih user (Gemini / Groq) lewat HTTPS.
 */
class ApiKeyStoreV2(context: Context) {

    companion object {
        private const val TAG = "PromptKu"
        private const val PREFS_NAME = "promptku_api_keys"
        private const val KEY_GEMINI = "gemini_api_key"
        private const val KEY_GROQ = "groq_api_key"
        private const val KEY_GEMINI_MODEL = "gemini_model_id"
        private const val KEY_GROQ_MODEL = "groq_model_id"
        private const val KEY_QUOTA_DATE = "quota_date"
        private const val QUOTA_PREFIX = "quota_"

        const val DEFAULT_GEMINI_MODEL = "gemini-3.8-flash"
        const val DEFAULT_GROQ_MODEL = "openai/gpt-oss-120b"
        const val DEFAULT_VISION_MODEL = "qwen/qwen3.8-27b"
    }

    private val prefs: SharedPreferences

    /** TRUE kalau penyimpanan benar-benar terenkripsi. */
    var isEncrypted: Boolean = false
        private set

    init {
        val loaded: SharedPreferences = try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            isEncrypted = true
            EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Log.w(TAG, "ApiKeyStoreV2: EncryptedSharedPreferences gagal, fallback biasa", e)
            isEncrypted = false
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
        prefs = loaded
    }

    var geminiApiKey: String
        get() = prefs.getString(KEY_GEMINI, "") ?: ""
        set(value) = prefs.edit().putString(KEY_GEMINI, value.trim()).apply()

    var groqApiKey: String
        get() = prefs.getString(KEY_GROQ, "") ?: ""
        set(value) = prefs.edit().putString(KEY_GROQ, value.trim()).apply()

    var geminiModelId: String
        get() = prefs.getString(KEY_GEMINI_MODEL, DEFAULT_GEMINI_MODEL)
            ?: DEFAULT_GEMINI_MODEL
        set(value) = prefs.edit().putString(
            KEY_GEMINI_MODEL,
            value.trim().ifEmpty { DEFAULT_GEMINI_MODEL }
        ).apply()

    var groqModelId: String
        get() = prefs.getString(KEY_GROQ_MODEL, DEFAULT_GROQ_MODEL)
            ?: DEFAULT_GROQ_MODEL
        set(value) = prefs.edit().putString(
            KEY_GROQ_MODEL,
            value.trim().ifEmpty { DEFAULT_GROQ_MODEL }
        ).apply()

    fun hasGeminiKey(): Boolean = geminiApiKey.isNotBlank()
    fun hasGroqKey(): Boolean = groqApiKey.isNotBlank()
    fun hasAnyKey(): Boolean = hasGeminiKey() || hasGroqKey()

    /** Hitungan panggilan hari ini (tanggal WIB "yyyy-MM-dd"). */
    fun getCounts(today: String): QuotaCounts {
        if (prefs.getString(KEY_QUOTA_DATE, "") != today) return emptyMap()
        return V2Provider.values().associateWith { provider ->
            prefs.getInt(QUOTA_PREFIX + provider.name, 0)
        }.filterValues { it > 0 }
    }

    fun recordCall(provider: V2Provider, today: String) {
        val editor = prefs.edit()
        if (prefs.getString(KEY_QUOTA_DATE, "") != today) {
            editor.putString(KEY_QUOTA_DATE, today)
            for (p in V2Provider.values()) editor.putInt(QUOTA_PREFIX + p.name, 0)
        }
        val key = QUOTA_PREFIX + provider.name
        editor.putInt(key, prefs.getInt(key, 0) + 1)
        editor.apply()
    }
}
