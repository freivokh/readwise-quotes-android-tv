// app/src/main/java/com/readwisequotes/settings/SettingsManager.kt
package com.readwisequotes.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.readwisequotes.data.model.TagGroup
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
    fun clearLastSyncTime() = prefs.edit().remove(KEY_LAST_SYNC).apply()

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

    // Text size
    fun getTextSize(): TextSize {
        val value = prefs.getString(KEY_TEXT_SIZE, TextSize.MEDIUM.name) ?: TextSize.MEDIUM.name
        return TextSize.valueOf(value)
    }
    fun setTextSize(size: TextSize) = prefs.edit().putString(KEY_TEXT_SIZE, size.name).apply()

    // Quote duration in seconds
    fun getQuoteDuration(): Int = prefs.getInt(KEY_QUOTE_DURATION, 20)
    fun setQuoteDuration(seconds: Int) = prefs.edit().putInt(KEY_QUOTE_DURATION, seconds).apply()

    // Show tags on quote display
    fun getShowTags(): Boolean = prefs.getBoolean(KEY_SHOW_TAGS, true)
    fun setShowTags(show: Boolean) = prefs.edit().putBoolean(KEY_SHOW_TAGS, show).apply()

    // Show notes on quote display
    fun getShowNotes(): Boolean = prefs.getBoolean(KEY_SHOW_NOTES, true)
    fun setShowNotes(show: Boolean) = prefs.edit().putBoolean(KEY_SHOW_NOTES, show).apply()

    // Show QR code on quote display
    fun getShowQrCode(): Boolean = prefs.getBoolean(KEY_SHOW_QR_CODE, true)
    fun setShowQrCode(show: Boolean) = prefs.edit().putBoolean(KEY_SHOW_QR_CODE, show).apply()

    // QR code link type
    fun getQrLinkType(): QrLinkType {
        val value = prefs.getString(KEY_QR_LINK_TYPE, QrLinkType.READWISE.name) ?: QrLinkType.READWISE.name
        return QrLinkType.valueOf(value)
    }
    fun setQrLinkType(type: QrLinkType) = prefs.edit().putString(KEY_QR_LINK_TYPE, type.name).apply()

    // Left align text (instead of center)
    fun getLeftAlignText(): Boolean = prefs.getBoolean(KEY_LEFT_ALIGN_TEXT, false)
    fun setLeftAlignText(leftAlign: Boolean) = prefs.edit().putBoolean(KEY_LEFT_ALIGN_TEXT, leftAlign).apply()

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

    // Tag Groups
    private val gson = Gson()

    fun getTagGroups(): List<TagGroup> {
        val json = prefs.getString(KEY_TAG_GROUPS, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<TagGroup>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveTagGroups(groups: List<TagGroup>) {
        try {
            val json = gson.toJson(groups)
            android.util.Log.d("SettingsManager", "Saving tag groups JSON: $json")
            val success = prefs.edit().putString(KEY_TAG_GROUPS, json).commit()
            android.util.Log.d("SettingsManager", "Save result: $success")
        } catch (e: Exception) {
            android.util.Log.e("SettingsManager", "Error saving tag groups", e)
        }
    }

    fun addTagGroup(group: TagGroup) {
        android.util.Log.d("SettingsManager", "Adding tag group: ${group.name}")
        val groups = getTagGroups().toMutableList()
        groups.add(group)
        android.util.Log.d("SettingsManager", "Total groups now: ${groups.size}")
        saveTagGroups(groups)
    }

    fun updateTagGroup(group: TagGroup) {
        val groups = getTagGroups().toMutableList()
        val index = groups.indexOfFirst { it.id == group.id }
        if (index >= 0) {
            groups[index] = group
            saveTagGroups(groups)
        }
    }

    fun updateTagGroupMatchMode(groupId: String, mode: TagFilterMode) {
        val groups = getTagGroups().toMutableList()
        val index = groups.indexOfFirst { it.id == groupId }
        if (index != -1) {
            groups[index] = groups[index].copy(matchMode = mode)
            saveTagGroups(groups)
        }
    }

    fun deleteTagGroup(groupId: String) {
        val groups = getTagGroups().filter { it.id != groupId }
        saveTagGroups(groups)
    }

    fun toggleTagGroup(groupId: String) {
        val groups = getTagGroups().toMutableList()
        val index = groups.indexOfFirst { it.id == groupId }
        if (index >= 0) {
            groups[index] = groups[index].copy(isEnabled = !groups[index].isEnabled)
            saveTagGroups(groups)
        }
    }

    fun getEnabledTagGroups(): List<TagGroup> = getTagGroups().filter { it.isEnabled }

    fun getAllEnabledTags(): Set<String> = getEnabledTagGroups().flatMap { it.tags }.toSet()

    companion object {
        private const val KEY_API_TOKEN = "api_token"
        private const val KEY_LAST_SYNC = "last_sync"
        private const val KEY_QUOTE_FILTER = "quote_filter"
        private const val KEY_SELECTED_TAGS = "selected_tags"
        private const val KEY_TAG_GROUPS = "tag_groups"
        private const val KEY_VISUAL_STYLE = "visual_style"
        private const val KEY_TEXT_SIZE = "text_size"
        private const val KEY_QUOTE_DURATION = "quote_duration"
        private const val KEY_SYNC_INTERVAL = "sync_interval"
        private const val KEY_SHOW_TAGS = "show_tags"
        private const val KEY_SHOW_NOTES = "show_notes"
        private const val KEY_SHOW_QR_CODE = "show_qr_code"
        private const val KEY_QR_LINK_TYPE = "qr_link_type"
        private const val KEY_LEFT_ALIGN_TEXT = "left_align_text"
    }
}
