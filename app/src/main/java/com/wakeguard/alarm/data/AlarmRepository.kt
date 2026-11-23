package com.wakeguard.alarm.data

import android.content.Context
import android.content.SharedPreferences
import com.wakeguard.alarm.model.AlarmItem
import com.wakeguard.alarm.model.MissionConfig
import com.wakeguard.alarm.model.SnakeSpeed
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class AlarmRepository(context: Context) {

    // Use device-protected storage so BootReceiver can read scheduled/active alarms even before first unlock
    private val storageContext: Context = context.applicationContext.createDeviceProtectedStorageContext()
    private val prefs: SharedPreferences = storageContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val _alarms = MutableStateFlow(loadAlarms())
    val alarms: StateFlow<List<AlarmItem>> = _alarms.asStateFlow()

    init {
        if (!prefs.contains(KEY_INITIALIZED)) {
            val starter = listOf(
                AlarmItem(
                    hour = 7,
                    minute = 0,
                    label = "Wake Up",
                    enabled = false,
                    repeatDays = setOf(2, 3, 4, 5, 6),
                    mission = MissionConfig.Snake(
                        targetLength = 8,
                        speed = SnakeSpeed.CHILL,
                        wrapWalls = true
                    ),
                    strictExitGuard = true
                )
            )
            saveAlarms(starter)
            prefs.edit().putBoolean(KEY_INITIALIZED, true).apply()
        }
    }

    private fun loadAlarms(): List<AlarmItem> {
        val raw = prefs.getString(KEY_ALARMS, null) ?: return emptyList()
        return runCatching {
            json.decodeFromString<List<AlarmItem>>(raw)
                .sortedWith(compareBy<AlarmItem> { it.hour }.thenBy { it.minute })
        }.getOrDefault(emptyList())
    }

    private fun saveAlarms(items: List<AlarmItem>) {
        val sorted = items.sortedWith(compareBy<AlarmItem> { it.hour }.thenBy { it.minute })
        prefs.edit().putString(KEY_ALARMS, json.encodeToString(sorted)).apply()
        _alarms.value = sorted
    }

    fun getAll(): List<AlarmItem> = _alarms.value

    fun getById(id: String): AlarmItem? = _alarms.value.firstOrNull { it.id == id }

    fun upsert(alarm: AlarmItem) {
        val current = _alarms.value.toMutableList()
        val index = current.indexOfFirst { it.id == alarm.id }
        if (index >= 0) {
            current[index] = alarm
        } else {
            current.add(alarm)
        }
        saveAlarms(current)
    }

    fun delete(alarmId: String) {
        saveAlarms(_alarms.value.filterNot { it.id == alarmId })
    }

    fun setEnabled(alarmId: String, enabled: Boolean): AlarmItem? {
        val target = getById(alarmId) ?: return null
        val updated = target.copy(enabled = enabled)
        upsert(updated)
        return updated
    }

    fun setActiveRingingAlarm(alarm: AlarmItem?) {
        prefs.edit().apply {
            if (alarm == null) {
                remove(KEY_ACTIVE_ALARM_JSON)
            } else {
                putString(KEY_ACTIVE_ALARM_JSON, json.encodeToString(alarm))
            }
        }.apply()
    }

    fun getActiveRingingAlarm(): AlarmItem? {
        val raw = prefs.getString(KEY_ACTIVE_ALARM_JSON, null) ?: return null
        return runCatching { json.decodeFromString<AlarmItem>(raw) }.getOrNull()
    }

    companion object {
        private const val PREFS_NAME = "wakeguard_alarms_v1"
        private const val KEY_ALARMS = "alarms_json"
        private const val KEY_INITIALIZED = "initialized"
        private const val KEY_ACTIVE_ALARM_JSON = "active_ringing_alarm_json"
    }
}
