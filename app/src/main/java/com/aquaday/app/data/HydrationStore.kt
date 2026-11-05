package com.aquaday.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

internal val Context.aquaDataStore: DataStore<Preferences> by preferencesDataStore(name = "aqua_hydration")

private object AquaKeys {
    val EVENTS = stringPreferencesKey("events")
    val GOAL = intPreferencesKey("goal_ml")
    val UNLOCKED = stringPreferencesKey("unlocked")
    val THEME = intPreferencesKey("theme_mode")
    val REMINDER_ON = intPreferencesKey("reminder_on")
    val INTERVAL = intPreferencesKey("reminder_interval")
    val ACTIVE_START = intPreferencesKey("active_start")
    val ACTIVE_END = intPreferencesKey("active_end")
    val LIVE_ON = intPreferencesKey("live_activity_on")
    val SOUND_ON = intPreferencesKey("sound_on")
    val VIBRATE_ON = intPreferencesKey("vibrate_on")
    val LAST_REMINDER = longPreferencesKey("last_reminder_at")
    val CUSTOM_TYPES = stringPreferencesKey("custom_types")
    val DEFAULT_CUP = intPreferencesKey("default_cup_ml")
    val DYNAMIC_COLOR = intPreferencesKey("dynamic_color")
    val COLOR_STYLE = intPreferencesKey("color_style")
    val CUSTOM_ACCENT = intPreferencesKey("custom_accent")
    val BACKGROUND_IMAGE = stringPreferencesKey("background_image")
    val BACKGROUND_SCRIM = intPreferencesKey("background_scrim")
    val BACKGROUND_BLUR = intPreferencesKey("background_blur")
    val CONTAINER_OPACITY = intPreferencesKey("container_opacity")
    val ONBOARDED = intPreferencesKey("onboarded")
    val NICKNAME = stringPreferencesKey("nickname")
    val AVATAR_IMAGE = stringPreferencesKey("avatar_image")
}

/** DataStore 持久层；只负责编解码与读写，业务规则留在仓储层。 */
class HydrationStore(context: Context) {

    private val zone: ZoneId = ZoneId.systemDefault()
    private val store = context.applicationContext.aquaDataStore

    val snapshot: Flow<AquaSnapshot> = store.data
        .catch { error ->
            if (error is IOException) {
                android.util.Log.w(TAG, "snapshot: 读取失败，回退空配置 - ${error.message}")
                emit(emptyPreferences())
            } else {
                throw error
            }
        }
        .map { prefs ->
            val customTypes = decodeTypes(prefs[AquaKeys.CUSTOM_TYPES].orEmpty())
            val types = DrinkType.builtins + customTypes
            AquaSnapshot(
                events = decodeEvents(prefs[AquaKeys.EVENTS].orEmpty(), types),
                goalMl = prefs[AquaKeys.GOAL] ?: DEFAULT_GOAL_ML,
                settings = ReminderSettings(
                    enabled = (prefs[AquaKeys.REMINDER_ON] ?: 1) == 1,
                    intervalMinutes = (prefs[AquaKeys.INTERVAL] ?: DEFAULT_INTERVAL_MIN).coerceIn(15, 240),
                    activeStartHour = (prefs[AquaKeys.ACTIVE_START] ?: 8).coerceIn(0, 23),
                    activeEndHour = (prefs[AquaKeys.ACTIVE_END] ?: 22).coerceIn(1, 24),
                    liveActivityEnabled = (prefs[AquaKeys.LIVE_ON] ?: 1) == 1,
                    soundEnabled = (prefs[AquaKeys.SOUND_ON] ?: 1) == 1,
                    vibrateEnabled = (prefs[AquaKeys.VIBRATE_ON] ?: 1) == 1,
                    defaultCupMl = (prefs[AquaKeys.DEFAULT_CUP] ?: DEFAULT_CUP_ML).coerceIn(50, 1000),
                ),
                unlocked = decodeUnlocked(prefs[AquaKeys.UNLOCKED].orEmpty()),
                themeMode = ThemeMode.entries.getOrElse(prefs[AquaKeys.THEME] ?: 0) { ThemeMode.System },
                lastReminderAt = prefs[AquaKeys.LAST_REMINDER]?.let { millis ->
                    Instant.ofEpochMilli(millis).atZone(zone).toLocalDateTime()
                },
                drinkTypes = types,
                colorStyle = prefs[AquaKeys.COLOR_STYLE]?.let { ColorStyle.entries.getOrNull(it) }
                    ?: if ((prefs[AquaKeys.DYNAMIC_COLOR] ?: 0) == 1) ColorStyle.Dynamic else ColorStyle.Default,
                customAccent = prefs[AquaKeys.CUSTOM_ACCENT],
                backgroundImage = prefs[AquaKeys.BACKGROUND_IMAGE]?.takeIf { it.isNotBlank() },
                backgroundScrim = (prefs[AquaKeys.BACKGROUND_SCRIM] ?: (DEFAULT_SCRIM * 100).toInt()) / 100f,
                backgroundBlur = (prefs[AquaKeys.BACKGROUND_BLUR] ?: 0).toFloat(),
                containerOpacity = (prefs[AquaKeys.CONTAINER_OPACITY] ?: 100) / 100f,
                onboarded = (prefs[AquaKeys.ONBOARDED] ?: 0) == 1,
                nickname = prefs[AquaKeys.NICKNAME].orEmpty(),
                avatarImage = prefs[AquaKeys.AVATAR_IMAGE]?.takeIf { it.isNotBlank() },
            )
        }

    suspend fun logDrink(amountMl: Int, type: DrinkType, at: LocalDateTime = LocalDateTime.now()): Long {
        val now = LocalDateTime.now()
        val safeAt = if (at.isAfter(now)) now else at
        val id = safeAt.atZone(zone).toInstant().toEpochMilli() * 1000L + (Math.random() * 999).toLong()
        mutateEvents { current ->
            current + DrinkEvent(id, amountMl.coerceAtLeast(1), type, safeAt)
        }
        return id
    }

    /** 返回新类型；名称重复时直接复用已有类型（包括内置类型）。 */
    suspend fun addDrinkType(rawLabel: String): DrinkType? {
        val label = rawLabel.trim().replace(';', ' ').replace("\n", " ").take(12)
        if (label.isEmpty()) return null
        var result: DrinkType? = null
        store.edit { prefs ->
            val current = decodeTypes(prefs[AquaKeys.CUSTOM_TYPES].orEmpty())
            val existing = (DrinkType.builtins + current)
                .firstOrNull { it.label.equals(label, ignoreCase = true) }
            result = existing ?: DrinkType.custom(
                label = label,
                id = "custom_${System.currentTimeMillis().toString(36)}${(Math.random() * 999).toInt()}",
            ).also { created ->
                prefs[AquaKeys.CUSTOM_TYPES] = encodeTypes(current + created)
            }
        }
        return result
    }

    suspend fun deleteEvent(id: Long) {
        mutateEvents { current -> current.filterNot { it.id == id } }
    }

    suspend fun setGoal(goalMl: Int) {
        store.edit { it[AquaKeys.GOAL] = goalMl.coerceIn(500, 6_000) }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        store.edit { it[AquaKeys.THEME] = mode.ordinal }
    }

    suspend fun setColorStyle(style: ColorStyle) {
        store.edit {
            it[AquaKeys.COLOR_STYLE] = style.ordinal
            // 旧版本只认识动态取色开关，保留同步以便降级时不丢设置。
            it[AquaKeys.DYNAMIC_COLOR] = if (style == ColorStyle.Dynamic) 1 else 0
        }
    }

    suspend fun setCustomAccent(argb: Int?) {
        store.edit { prefs ->
            if (argb == null) prefs.remove(AquaKeys.CUSTOM_ACCENT) else prefs[AquaKeys.CUSTOM_ACCENT] = argb
        }
    }

    suspend fun setBackgroundImage(path: String?) {
        store.edit { prefs ->
            if (path == null) prefs.remove(AquaKeys.BACKGROUND_IMAGE) else prefs[AquaKeys.BACKGROUND_IMAGE] = path
        }
    }

    suspend fun setBackgroundScrim(percent: Int) {
        store.edit { it[AquaKeys.BACKGROUND_SCRIM] = percent.coerceIn(0, 95) }
    }

    suspend fun setBackgroundBlur(dp: Int) {
        store.edit { it[AquaKeys.BACKGROUND_BLUR] = dp.coerceIn(0, MAX_BLUR_DP.toInt()) }
    }

    suspend fun setContainerOpacity(percent: Int) {
        store.edit { it[AquaKeys.CONTAINER_OPACITY] = percent.coerceIn((MIN_CONTAINER_OPACITY * 100).toInt(), 100) }
    }

    suspend fun setOnboarded(completed: Boolean) {
        store.edit { it[AquaKeys.ONBOARDED] = if (completed) 1 else 0 }
    }

    suspend fun setNickname(raw: String) {
        val value = raw.trim().replace("\n", " ").take(24)
        store.edit { prefs ->
            if (value.isEmpty()) prefs.remove(AquaKeys.NICKNAME) else prefs[AquaKeys.NICKNAME] = value
        }
    }

    suspend fun setAvatarImage(path: String?) {
        store.edit { prefs ->
            if (path == null) prefs.remove(AquaKeys.AVATAR_IMAGE) else prefs[AquaKeys.AVATAR_IMAGE] = path
        }
    }

    suspend fun updateSettings(transform: (ReminderSettings) -> ReminderSettings) {
        store.edit { prefs ->
            val current = settingsOf(prefs)
            val next = transform(current)
            prefs[AquaKeys.REMINDER_ON] = if (next.enabled) 1 else 0
            prefs[AquaKeys.INTERVAL] = next.intervalMinutes.coerceIn(15, 240)
            prefs[AquaKeys.ACTIVE_START] = next.activeStartHour.coerceIn(0, 23)
            prefs[AquaKeys.ACTIVE_END] = next.activeEndHour.coerceIn(1, 24)
            prefs[AquaKeys.LIVE_ON] = if (next.liveActivityEnabled) 1 else 0
            prefs[AquaKeys.SOUND_ON] = if (next.soundEnabled) 1 else 0
            prefs[AquaKeys.VIBRATE_ON] = if (next.vibrateEnabled) 1 else 0
            prefs[AquaKeys.DEFAULT_CUP] = next.defaultCupMl.coerceIn(50, 1000)
        }
    }

    suspend fun markReminderFired(at: LocalDateTime = LocalDateTime.now()) {
        store.edit { it[AquaKeys.LAST_REMINDER] = at.atZone(zone).toInstant().toEpochMilli() }
    }

    /** 写入本次达成的成就，保留已达成的记录不覆盖时间。 */
    suspend fun markUnlocked(ids: Collection<String>, at: Long = System.currentTimeMillis()) {
        if (ids.isEmpty()) return
        store.edit { prefs ->
            val merged = prefs[AquaKeys.UNLOCKED].orEmpty()
            val map = decodeUnlocked(merged).toMutableMap()
            ids.forEach { map.putIfAbsent(it, at) }
            prefs[AquaKeys.UNLOCKED] = encodeUnlocked(map)
        }
    }

    /** 清空记录与设置，但保留引导标记与个人信息（昵称、头像不属于喝水数据）。 */
    suspend fun resetAll() {
        store.edit { prefs ->
            val onboarded = prefs[AquaKeys.ONBOARDED]
            val nickname = prefs[AquaKeys.NICKNAME]
            val avatar = prefs[AquaKeys.AVATAR_IMAGE]
            prefs.clear()
            if (onboarded != null) prefs[AquaKeys.ONBOARDED] = onboarded
            if (nickname != null) prefs[AquaKeys.NICKNAME] = nickname
            if (avatar != null) prefs[AquaKeys.AVATAR_IMAGE] = avatar
        }
    }

    private suspend fun mutateEvents(transform: (List<DrinkEvent>) -> List<DrinkEvent>) {
        store.edit { prefs ->
            val types = DrinkType.builtins + decodeTypes(prefs[AquaKeys.CUSTOM_TYPES].orEmpty())
            val next = transform(decodeEvents(prefs[AquaKeys.EVENTS].orEmpty(), types))
            prefs[AquaKeys.EVENTS] = encodeEvents(next.sortedBy { it.at })
        }
    }

    private fun settingsOf(prefs: Preferences) = ReminderSettings(
        enabled = (prefs[AquaKeys.REMINDER_ON] ?: 1) == 1,
        intervalMinutes = prefs[AquaKeys.INTERVAL] ?: DEFAULT_INTERVAL_MIN,
        activeStartHour = prefs[AquaKeys.ACTIVE_START] ?: 8,
        activeEndHour = prefs[AquaKeys.ACTIVE_END] ?: 22,
        liveActivityEnabled = (prefs[AquaKeys.LIVE_ON] ?: 1) == 1,
        soundEnabled = (prefs[AquaKeys.SOUND_ON] ?: 1) == 1,
        vibrateEnabled = (prefs[AquaKeys.VIBRATE_ON] ?: 1) == 1,
        defaultCupMl = prefs[AquaKeys.DEFAULT_CUP] ?: DEFAULT_CUP_ML,
    )

    private fun encodeEvents(events: List<DrinkEvent>): String =
        events.joinToString("\n") {
            "${it.id};${it.amountMl};${it.type.id};${it.at.atZone(zone).toInstant().toEpochMilli()}"
        }

    private fun decodeEvents(raw: String, types: List<DrinkType>): List<DrinkEvent> =
        raw.lineSequence()
            .filter { it.isNotBlank() }
            .mapNotNull { line ->
                runCatching {
                    val parts = line.split(';')
                    val token = parts[2]
                    // 旧版本存的是枚举 ordinal；新版本存类型 id。
                    val type = token.toIntOrNull()
                        ?.let { DrinkType.builtins.getOrElse(it) { DrinkType.WATER } }
                        ?: types.firstOrNull { it.id == token }
                        ?: DrinkType.WATER
                    DrinkEvent(
                        id = parts[0].toLong(),
                        amountMl = parts[1].toInt(),
                        type = type,
                        at = Instant.ofEpochMilli(parts[3].toLong()).atZone(zone).toLocalDateTime(),
                    )
                }.getOrNull()
            }
            .toList()

    private fun encodeTypes(types: List<DrinkType>): String =
        types.joinToString("\n") { "${it.id};${it.label};${it.factor}" }

    private fun decodeTypes(raw: String): List<DrinkType> =
        raw.lineSequence()
            .filter { it.isNotBlank() }
            .mapNotNull { line ->
                val parts = line.split(';')
                runCatching {
                    DrinkType(
                        id = parts[0],
                        label = parts[1],
                        factor = parts[2].toDouble(),
                        builtin = false,
                    )
                }.getOrNull()
            }
            .toList()

    private fun encodeUnlocked(map: Map<String, Long>): String =
        map.entries.joinToString("\n") { "${it.key};${it.value}" }

    private fun decodeUnlocked(raw: String): Map<String, Long> =
        raw.lineSequence()
            .filter { it.isNotBlank() }
            .mapNotNull { line ->
                val parts = line.split(';')
                runCatching { parts[0] to parts[1].toLong() }.getOrNull()
            }
            .toMap()

    private companion object {
        const val TAG = "HydrationStore"
    }
}

const val DEFAULT_GOAL_ML = 2000
const val DEFAULT_INTERVAL_MIN = 60
const val DEFAULT_CUP_ML = 200
