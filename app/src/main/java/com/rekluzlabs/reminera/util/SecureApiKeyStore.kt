package com.rekluzlabs.reminera.util

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.security.GeneralSecurityException

class SecureApiKeyStore(context: Context) {

    private val prefs: SharedPreferences by lazy { createPrefs(context) }

    fun saveApiKey(key: String) {
        prefs.edit().putString(KEY_API_KEY, key).apply()
    }

    fun getApiKey(): String? {
        return prefs.getString(KEY_API_KEY, null)
    }

    fun clearApiKey() {
        prefs.edit().remove(KEY_API_KEY).apply()
    }

    fun hasApiKey(): Boolean {
        return !getApiKey().isNullOrBlank()
    }

    fun saveSelectedModel(modelId: String) {
        prefs.edit().putString(KEY_SELECTED_MODEL, modelId).apply()
    }

    fun getSelectedModel(): String {
        return prefs.getString(KEY_SELECTED_MODEL, DEFAULT_MODEL_ID) ?: DEFAULT_MODEL_ID
    }

    fun saveVerified(verified: Boolean) {
        prefs.edit().putBoolean(KEY_VERIFIED, verified).apply()
    }

    fun isVerified(): Boolean {
        return prefs.getBoolean(KEY_VERIFIED, false)
    }

    suspend fun verifyApiKey(apiKey: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val urlString = "$GEMINI_BASE_URL/models/$VERIFY_MODEL_ID:generateContent"
                Log.d("SecureApiKeyStore", "Verifying key against: $urlString")
                val url = URL(urlString)
                val connection = url.openConnection() as HttpURLConnection

                try {
                    connection.requestMethod = "POST"
                    connection.setRequestProperty("Content-Type", "application/json")
                    connection.setRequestProperty("X-Goog-Api-Key", apiKey)
                    connection.connectTimeout = 15_000
                    connection.readTimeout = 15_000
                    connection.doOutput = true

                    val body = JSONObject().apply {
                        put("contents", JSONArray().apply {
                            put(JSONObject().apply {
                                put("parts", JSONArray().apply {
                                    put(JSONObject().apply {
                                        put("text", "hi")
                                    })
                                })
                            })
                        })
                        put("generationConfig", JSONObject().apply {
                            put("maxOutputTokens", 1)
                        })
                    }

                    val bodyStr = body.toString()
                    Log.d("SecureApiKeyStore", "Request body: $bodyStr")
                    connection.outputStream.use { os ->
                        os.write(bodyStr.toByteArray(Charsets.UTF_8))
                    }

                    val responseCode = connection.responseCode
                    Log.d("SecureApiKeyStore", "Response code: $responseCode")
                    val responseBody = if (responseCode in 200..299) {
                        connection.inputStream?.bufferedReader()?.use { it.readText() } ?: ""
                    } else {
                        connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                    }
                    Log.d("SecureApiKeyStore", "Response body: ${responseBody.take(500)}")

                    if (responseCode !in 200..299) {
                        Log.e("SecureApiKeyStore", "HTTP error $responseCode")
                        return@withContext Result.failure(Exception("Invalid API key (HTTP $responseCode)"))
                    }

                    val resultJson = JSONObject(responseBody)
                    if (resultJson.has("error")) {
                        val error = resultJson.getJSONObject("error")
                        val msg = error.optString("message", "Invalid API key")
                        Log.e("SecureApiKeyStore", "API error: $msg")
                        return@withContext Result.failure(Exception(msg))
                    }

                    Log.d("SecureApiKeyStore", "Verification successful")
                    Result.success(Unit)
                } finally {
                    connection.disconnect()
                }
            } catch (e: Exception) {
                Log.e("SecureApiKeyStore", "Verification exception: ${e.javaClass.simpleName}: ${e.message}", e)
                Result.failure(Exception("Verification failed: ${e.message}"))
            }
        }
    }

    private fun createPrefs(context: Context): SharedPreferences {
        return try {
            createEncryptedPrefs(context)
        } catch (_: GeneralSecurityException) {
            clearCorruptedPrefs(context)
            createEncryptedPrefs(context)
        } catch (_: Exception) {
            clearCorruptedPrefs(context)
            createEncryptedPrefs(context)
        }
    }

    private fun createEncryptedPrefs(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            context,
            PREFS_FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private fun clearCorruptedPrefs(context: Context) {
        context.getSharedPreferences(PREFS_FILE_NAME, Context.MODE_PRIVATE)
            .edit().clear().apply()
        context.deleteSharedPreferences(PREFS_FILE_NAME)
    }

    companion object {
        private const val PREFS_FILE_NAME = "reminera_ai_prefs"
        private const val KEY_API_KEY = "gemini_api_key"
        private const val KEY_SELECTED_MODEL = "selected_model"
        private const val KEY_VERIFIED = "key_verified"
        const val DEFAULT_MODEL_ID = "gemini-3.1-flash-lite"
        private const val VERIFY_MODEL_ID = "gemini-2.5-flash"
        const val GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/v1beta"
    }
}

enum class GeminiModelTier(val displayName: String) {
    PRO("Pro Tier"),
    FLASH("Flash Tier"),
    FLASH_LITE("Flash-Lite Tier")
}

enum class GeminiModel(
    val modelId: String,
    val displayName: String,
    val tier: GeminiModelTier,
    val description: String
) {
    GEMINI_3_1_PRO(
        modelId = "gemini-3.1-pro",
        displayName = "Gemini 3.1 Pro",
        tier = GeminiModelTier.PRO,
        description = "Latest high-intelligence model for complex problem-solving, broad context reasoning, and agentic workflows"
    ),
    GEMINI_2_5_PRO(
        modelId = "gemini-2.5-pro",
        displayName = "Gemini 2.5 Pro",
        tier = GeminiModelTier.PRO,
        description = "Stable production workhorse for high-depth tasks, adaptive thinking, and complex multimodal inputs"
    ),
    GEMINI_3_6_FLASH(
        modelId = "gemini-3.6-flash",
        displayName = "Gemini 3.6 Flash",
        tier = GeminiModelTier.FLASH,
        description = "Newest primary Flash model, balancing high speed with strong multimodal and coding capabilities"
    ),
    GEMINI_3_5_FLASH(
        modelId = "gemini-3.5-flash",
        displayName = "Gemini 3.5 Flash",
        tier = GeminiModelTier.FLASH,
        description = "High-speed, frontier-class model for agentic tasks and heavy coding workflows"
    ),
    GEMINI_3_5_FLASH_LITE(
        modelId = "gemini-3.5-flash-lite",
        displayName = "Gemini 3.5 Flash-Lite",
        tier = GeminiModelTier.FLASH_LITE,
        description = "Optimized for lightweight tasks, automated translations, and background utilities"
    ),
    GEMINI_3_1_FLASH_LITE(
        modelId = "gemini-3.1-flash-lite",
        displayName = "Gemini 3.1 Flash-Lite",
        tier = GeminiModelTier.FLASH_LITE,
        description = "Optimized for ultra-fast, high-volume, cost-sensitive execution (Recommended)"
    );

    companion object {
        fun fromModelId(modelId: String): GeminiModel {
            return entries.find { it.modelId == modelId } ?: GEMINI_3_1_FLASH_LITE
        }
    }
}
