// app/src/main/java/com/readwisequotes/settings/SettingsManager.kt
package com.readwisequotes.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val securePrefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private val prefs: SharedPreferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    // API Token (encrypted)
    fun getApiToken(): String = securePrefs.getString(KEY_API_TOKEN, "") ?: ""
    fun setApiToken(token: String) = securePrefs.edit().putString(KEY_API_TOKEN, token).apply()

    // Last sync timestamp
    fun getLastSyncTime(): String? = prefs.getString(KEY_LAST_SYNC, null)
    fun setLastSyncTime(time: String) = prefs.edit().putString(KEY_LAST_SYNC, time).apply()

    // Quote filter
    fun getQuoteFilter(): QuoteFilter {
        val value = prefs.getString(KEY_QUOTE_FILTER, QuoteFilter.ALL.name) ?: QuoteFilter.ALL.name
        return QuoteFilter.valueOf(value)
    }
    fun setQuoteFilter(filter: QuoteFilter) = prefs.edit().putString(KEY_QUOTE_FILTER, filter.name).apply()

    // Selected tags (for BY_TAG filter)
    fun getSelectedTags(): Set<String> = prefs.getStringSet(KEY_SELECTED_TAGS, emptySet()) ?: emptySet()
    fun setSelectedTags(tags: Set<String>) = prefs.edit().putStringSet(KEY_SELECTED_TAGS, tags).apply()

    // Visual style
    fun getVisualStyle(): VisualStyle {
        val value = prefs.getString(KEY_VISUAL_STYLE, VisualStyle.AMBIENT.name) ?: VisualStyle.AMBIENT.name
        return VisualStyle.valueOf(value)
    }
    fun setVisualStyle(style: VisualStyle) = prefs.edit().putString(KEY_VISUAL_STYLE, style.name).apply()

    // Quote duration in seconds
    fun getQuoteDuration(): Int = prefs.getInt(KEY_QUOTE_DURATION, 20)
    fun setQuoteDuration(seconds: Int) = prefs.edit().putInt(KEY_QUOTE_DURATION, seconds).apply()

    // Sync interval in hours
    fun getSyncIntervalHours(): Int = prefs.getInt(KEY_SYNC_INTERVAL, 24)
    fun setSyncIntervalHours(hours: Int) = prefs.edit().putInt(KEY_SYNC_INTERVAL, hours).apply()

    fun isSetupComplete(): Boolean = getApiToken().isNotEmpty()

    fun shouldSync(): Boolean {
        val lastSync = getLastSyncTime() ?: return true
        val lastSyncMillis = try {
            java.time.Instant.parse(lastSync).toEpochMilli()
        } catch (e: Exception) {
            return true
        }
        val intervalMillis = getSyncIntervalHours() * 60 * 60 * 1000L
        return System.currentTimeMillis() - lastSyncMillis > intervalMillis
    }

    companion object {
        private const val KEY_API_TOKEN = "api_token"
        private const val KEY_LAST_SYNC = "last_sync"
        private const val KEY_QUOTE_FILTER = "quote_filter"
        private const val KEY_SELECTED_TAGS = "selected_tags"
        private const val KEY_VISUAL_STYLE = "visual_style"
        private const val KEY_QUOTE_DURATION = "quote_duration"
        private const val KEY_SYNC_INTERVAL = "sync_interval"
    }
}
