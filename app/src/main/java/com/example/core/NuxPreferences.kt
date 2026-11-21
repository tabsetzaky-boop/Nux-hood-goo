package com.example.core

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

private val Context.nuxDataStore: DataStore<Preferences> by preferencesDataStore(name = "nux_prefs_v1")

enum class ThemeMode(val key: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark");

    companion object {
        fun fromKey(raw: String?): ThemeMode =
            entries.firstOrNull { it.key == raw } ?: DARK
    }
}

enum class BubbleTone(val key: String) {
    GLASS("glass"),
    BLUE("blue"),
    MINIMAL("minimal");

    companion object {
        fun fromKey(raw: String?): BubbleTone =
            entries.firstOrNull { it.key == raw } ?: GLASS
    }
}

enum class BubbleShape(val key: String) {
    ROUNDED("rounded"),
    PILL("pill");

    companion object {
        fun fromKey(raw: String?): BubbleShape =
            entries.firstOrNull { it.key == raw } ?: ROUNDED
    }
}

data class NuxUserPrefs(
    val themeMode: ThemeMode = ThemeMode.DARK,
    val appLanguage: AppLanguage = AppLanguage.AR,
    val replyLanguage: String = "auto",
    val webSearchDefault: Boolean = false,
    val deepThinkingDefault: Boolean = false,
    val showThinkingSteps: Boolean = true,
    val hapticsEnabled: Boolean = false,
    val notificationsEnabled: Boolean = false,
    val speechRate: Float = 1.0f,
    val liveVoice: String = "Aoede",
    val useMemory: Boolean = true,
    val autoSaveMemory: Boolean = false,
    val tempChatCustomized: Boolean = false,
    val bubbleTone: BubbleTone = BubbleTone.GLASS,
    val bubbleShape: BubbleShape = BubbleShape.ROUNDED,
    val watermarkEnabled: Boolean = true,
    val sidebarOpen: Boolean = true,
    val selectedModelId: String = "gemini-3.8-flash",
    val deviceId: String = "",
    val lockIntroShown: Boolean = false,
    val archiveIntroShown: Boolean = false,
    val guestMode: Boolean = false,
)

class NuxPreferencesRepository(private val context: Context) {
    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val APP_LANGUAGE = stringPreferencesKey("app_language")
        val REPLY_LANGUAGE = stringPreferencesKey("reply_language")
        val WEB_SEARCH_DEFAULT = booleanPreferencesKey("web_search_default")
        val DEEP_THINKING_DEFAULT = booleanPreferencesKey("deep_thinking_default")
        val SHOW_THINKING_STEPS = booleanPreferencesKey("show_thinking_steps")
        val HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val SPEECH_RATE = floatPreferencesKey("speech_rate")
        val LIVE_VOICE = stringPreferencesKey("live_voice")
        val USE_MEMORY = booleanPreferencesKey("use_memory")
        val AUTO_SAVE_MEMORY = booleanPreferencesKey("auto_save_memory")
        val TEMP_CHAT_CUSTOMIZED = booleanPreferencesKey("temp_chat_customized")
        val BUBBLE_TONE = stringPreferencesKey("bubble_tone")
        val BUBBLE_SHAPE = stringPreferencesKey("bubble_shape")
        val WATERMARK_ENABLED = booleanPreferencesKey("watermark_enabled")
        val SIDEBAR_OPEN = booleanPreferencesKey("sidebar_open")
        val SELECTED_MODEL = stringPreferencesKey("selected_model")
        val DEVICE_ID = stringPreferencesKey("device_id")
        val LOCK_INTRO_SHOWN = booleanPreferencesKey("lock_intro_shown")
        val ARCHIVE_INTRO_SHOWN = booleanPreferencesKey("archive_intro_shown")
        val GUEST_MODE = booleanPreferencesKey("guest_mode")
    }

    val prefsFlow: Flow<NuxUserPrefs> = context.nuxDataStore.data.map { prefs ->
        NuxUserPrefs(
            themeMode = ThemeMode.fromKey(prefs[Keys.THEME_MODE]),
            appLanguage = AppLanguage.fromCode(prefs[Keys.APP_LANGUAGE]),
            replyLanguage = prefs[Keys.REPLY_LANGUAGE] ?: "auto",
            webSearchDefault = prefs[Keys.WEB_SEARCH_DEFAULT] ?: false,
            deepThinkingDefault = prefs[Keys.DEEP_THINKING_DEFAULT] ?: false,
            showThinkingSteps = prefs[Keys.SHOW_THINKING_STEPS] ?: true,
            hapticsEnabled = prefs[Keys.HAPTICS_ENABLED] ?: false,
            notificationsEnabled = prefs[Keys.NOTIFICATIONS_ENABLED] ?: false,
            speechRate = prefs[Keys.SPEECH_RATE] ?: 1.0f,
            liveVoice = prefs[Keys.LIVE_VOICE] ?: "Aoede",
            useMemory = prefs[Keys.USE_MEMORY] ?: true,
            autoSaveMemory = prefs[Keys.AUTO_SAVE_MEMORY] ?: false,
            tempChatCustomized = prefs[Keys.TEMP_CHAT_CUSTOMIZED] ?: false,
            bubbleTone = BubbleTone.fromKey(prefs[Keys.BUBBLE_TONE]),
            bubbleShape = BubbleShape.fromKey(prefs[Keys.BUBBLE_SHAPE]),
            watermarkEnabled = prefs[Keys.WATERMARK_ENABLED] ?: true,
            sidebarOpen = prefs[Keys.SIDEBAR_OPEN] ?: true,
            selectedModelId = prefs[Keys.SELECTED_MODEL] ?: "gemini-3.8-flash",
            deviceId = prefs[Keys.DEVICE_ID] ?: "",
            lockIntroShown = prefs[Keys.LOCK_INTRO_SHOWN] ?: false,
            archiveIntroShown = prefs[Keys.ARCHIVE_INTRO_SHOWN] ?: false,
            guestMode = prefs[Keys.GUEST_MODE] ?: false,
        )
    }

    suspend fun ensureDeviceId(): String {
        var existing = ""
        context.nuxDataStore.edit { prefs ->
            val current = prefs[Keys.DEVICE_ID]
            if (current.isNullOrBlank()) {
                val created = "android_${UUID.randomUUID().toString().take(12)}"
                prefs[Keys.DEVICE_ID] = created
                existing = created
            } else {
                existing = current
            }
        }
        return existing
    }

    suspend fun setThemeMode(mode: ThemeMode) = update(Keys.THEME_MODE, mode.key)
    suspend fun setAppLanguage(lang: AppLanguage) = update(Keys.APP_LANGUAGE, lang.code)
    suspend fun setReplyLanguage(lang: String) = update(Keys.REPLY_LANGUAGE, lang)
    suspend fun setWebSearchDefault(enabled: Boolean) = update(Keys.WEB_SEARCH_DEFAULT, enabled)
    suspend fun setDeepThinkingDefault(enabled: Boolean) = update(Keys.DEEP_THINKING_DEFAULT, enabled)
    suspend fun setShowThinkingSteps(enabled: Boolean) = update(Keys.SHOW_THINKING_STEPS, enabled)
    suspend fun setHapticsEnabled(enabled: Boolean) = update(Keys.HAPTICS_ENABLED, enabled)
    suspend fun setNotificationsEnabled(enabled: Boolean) = update(Keys.NOTIFICATIONS_ENABLED, enabled)
    suspend fun setSpeechRate(rate: Float) = update(Keys.SPEECH_RATE, rate)
    suspend fun setLiveVoice(voice: String) = update(Keys.LIVE_VOICE, voice)
    suspend fun setUseMemory(enabled: Boolean) = update(Keys.USE_MEMORY, enabled)
    suspend fun setAutoSaveMemory(enabled: Boolean) = update(Keys.AUTO_SAVE_MEMORY, enabled)
    suspend fun setTempChatCustomized(enabled: Boolean) = update(Keys.TEMP_CHAT_CUSTOMIZED, enabled)
    suspend fun setBubbleTone(tone: BubbleTone) = update(Keys.BUBBLE_TONE, tone.key)
    suspend fun setBubbleShape(shape: BubbleShape) = update(Keys.BUBBLE_SHAPE, shape.key)
    suspend fun setWatermarkEnabled(enabled: Boolean) = update(Keys.WATERMARK_ENABLED, enabled)
    suspend fun setSidebarOpen(open: Boolean) = update(Keys.SIDEBAR_OPEN, open)
    suspend fun setSelectedModelId(modelId: String) = update(Keys.SELECTED_MODEL, modelId)
    suspend fun setLockIntroShown(shown: Boolean) = update(Keys.LOCK_INTRO_SHOWN, shown)
    suspend fun setArchiveIntroShown(shown: Boolean) = update(Keys.ARCHIVE_INTRO_SHOWN, shown)
    suspend fun setGuestMode(enabled: Boolean) = update(Keys.GUEST_MODE, enabled)

    fun draftFlow(mode: String): Flow<String> = context.nuxDataStore.data.map { prefs ->
        prefs[stringPreferencesKey("nux_draft_$mode")] ?: ""
    }

    suspend fun saveDraft(mode: String, text: String) {
        context.nuxDataStore.edit { prefs ->
            prefs[stringPreferencesKey("nux_draft_$mode")] = text
        }
    }

    private suspend fun <T> update(key: Preferences.Key<T>, value: T) {
        context.nuxDataStore.edit { prefs ->
            prefs[key] = value
        }
    }
}
