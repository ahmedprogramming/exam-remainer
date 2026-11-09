package com.studentreminder.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek

enum class ThemeMode(val label: String) { SYSTEM("System"), LIGHT("Light"), DARK("Dark") }

enum class AccentColor(val label: String, val argb: Long) {
    INDIGO("Indigo", 0xFF4F46E5),
    OCEAN("Ocean", 0xFF0284C7),
    EMERALD("Emerald", 0xFF059669),
    ROSE("Rose", 0xFFE11D48),
    SUNSET("Sunset", 0xFFEA580C),
    VIOLET("Violet", 0xFF7C3AED),
}

data class AppSettings(
    val userName: String = "",
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val accent: AccentColor = AccentColor.INDIGO,
    val dynamicColor: Boolean = false,
    val use24h: Boolean = false,
    val firstDayOfWeek: DayOfWeek = DayOfWeek.SUNDAY,
    val notificationsEnabled: Boolean = true,
    val sameDayHour: Int = 8,
    val defaultExamReminders: List<Long> = ReminderOption.defaultExam,
    val defaultAssignmentReminders: List<Long> = ReminderOption.defaultAssignment,
)

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val USER_NAME = stringPreferencesKey("user_name")
        val THEME = stringPreferencesKey("theme_mode")
        val ACCENT = stringPreferencesKey("accent")
        val DYNAMIC = booleanPreferencesKey("dynamic_color")
        val USE_24H = booleanPreferencesKey("use_24h")
        val FIRST_DAY = stringPreferencesKey("first_day_of_week")
        val NOTIFICATIONS = booleanPreferencesKey("notifications_enabled")
        val SAME_DAY_HOUR = intPreferencesKey("same_day_hour")
        val DEFAULT_EXAM = stringPreferencesKey("default_exam_reminders")
        val DEFAULT_ASSIGNMENT = stringPreferencesKey("default_assignment_reminders")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { p -> fromPrefs(p) }

    suspend fun current(): AppSettings = settings.first()

    private fun fromPrefs(p: Preferences): AppSettings {
        val d = AppSettings()
        return AppSettings(
            userName = p[Keys.USER_NAME] ?: d.userName,
            themeMode = enumOr(p[Keys.THEME], d.themeMode),
            accent = enumOr(p[Keys.ACCENT], d.accent),
            dynamicColor = p[Keys.DYNAMIC] ?: d.dynamicColor,
            use24h = p[Keys.USE_24H] ?: d.use24h,
            firstDayOfWeek = enumOr(p[Keys.FIRST_DAY], d.firstDayOfWeek),
            notificationsEnabled = p[Keys.NOTIFICATIONS] ?: d.notificationsEnabled,
            sameDayHour = p[Keys.SAME_DAY_HOUR] ?: d.sameDayHour,
            defaultExamReminders = decodeList(p[Keys.DEFAULT_EXAM]) ?: d.defaultExamReminders,
            defaultAssignmentReminders = decodeList(p[Keys.DEFAULT_ASSIGNMENT]) ?: d.defaultAssignmentReminders,
        )
    }

    suspend fun update(transform: (AppSettings) -> AppSettings) {
        context.dataStore.edit { p ->
            val s = transform(fromPrefs(p))
            p[Keys.USER_NAME] = s.userName
            p[Keys.THEME] = s.themeMode.name
            p[Keys.ACCENT] = s.accent.name
            p[Keys.DYNAMIC] = s.dynamicColor
            p[Keys.USE_24H] = s.use24h
            p[Keys.FIRST_DAY] = s.firstDayOfWeek.name
            p[Keys.NOTIFICATIONS] = s.notificationsEnabled
            p[Keys.SAME_DAY_HOUR] = s.sameDayHour
            p[Keys.DEFAULT_EXAM] = encodeList(s.defaultExamReminders)
            p[Keys.DEFAULT_ASSIGNMENT] = encodeList(s.defaultAssignmentReminders)
        }
    }

    private inline fun <reified T : Enum<T>> enumOr(value: String?, default: T): T =
        value?.let { v -> enumValues<T>().firstOrNull { it.name == v } } ?: default

    private fun encodeList(list: List<Long>) = list.joinToString(",")

    private fun decodeList(value: String?): List<Long>? =
        value?.split(",")?.mapNotNull { it.trim().toLongOrNull() }
}
