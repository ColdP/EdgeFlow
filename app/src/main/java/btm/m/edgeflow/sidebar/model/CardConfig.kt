package btm.m.edgeflow.sidebar.model

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import btm.m.edgeflow.data.dataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

/**
 * Card configuration for ordering and visibility.
 * Header (header image) is always fixed at top and never participates in sorting/hiding.
 */
data class CardConfig(
    val id: String,
    val label: String,
    val isVisible: Boolean = true,
    val order: Int = 0
)

object CardConfigManager {
    private val KEY_CARD_CONFIG = stringPreferencesKey("card_config_json")

    val DEFAULT_CONFIGS = listOf(
        CardConfig("apps", "App Shortcuts", true, 0),
        CardConfig("shortcut", "Quick Actions", true, 1),
        CardConfig("media", "Now Playing", true, 2),
        CardConfig("system", "Quick Controls", true, 3)
    )

    fun configFlow(context: Context): Flow<List<CardConfig>> {
        return context.dataStore.data.map { prefs ->
            val json = prefs[KEY_CARD_CONFIG]
            if (json.isNullOrBlank()) {
                DEFAULT_CONFIGS
            } else {
                try {
                    parseJson(json)
                } catch (_: Exception) {
                    DEFAULT_CONFIGS
                }
            }
        }
    }

    suspend fun saveConfig(context: Context, configs: List<CardConfig>) {
        val json = toJson(configs)
        context.dataStore.edit { it[KEY_CARD_CONFIG] = json }
    }

    suspend fun toggleVisibility(context: Context, cardId: String) {
        context.dataStore.edit { prefs ->
            val json = prefs[KEY_CARD_CONFIG]
            val current = if (json.isNullOrBlank()) DEFAULT_CONFIGS.toMutableList()
            else try { parseJson(json).toMutableList() } catch (_: Exception) { DEFAULT_CONFIGS.toMutableList() }
            val idx = current.indexOfFirst { it.id == cardId }
            if (idx >= 0) {
                current[idx] = current[idx].copy(isVisible = !current[idx].isVisible)
            }
            prefs[KEY_CARD_CONFIG] = toJson(current)
        }
    }

    suspend fun moveCard(context: Context, cardId: String, direction: Int) {
        context.dataStore.edit { prefs ->
            val json = prefs[KEY_CARD_CONFIG]
            val current = if (json.isNullOrBlank()) DEFAULT_CONFIGS.toMutableList()
            else try { parseJson(json).toMutableList() } catch (_: Exception) { DEFAULT_CONFIGS.toMutableList() }
            val idx = current.indexOfFirst { it.id == cardId }
            if (idx < 0) return@edit
            val newIdx = (idx + direction).coerceIn(0, current.size - 1)
            if (newIdx == idx) return@edit
            val item = current.removeAt(idx)
            current.add(newIdx, item)
            // Update order values
            current.forEachIndexed { i, cfg -> current[i] = cfg.copy(order = i) }
            prefs[KEY_CARD_CONFIG] = toJson(current)
        }
    }

    suspend fun resetToDefault(context: Context) {
        context.dataStore.edit { it[KEY_CARD_CONFIG] = toJson(DEFAULT_CONFIGS) }
    }

    private fun toJson(configs: List<CardConfig>): String {
        val arr = JSONArray()
        configs.forEach { cfg ->
            val obj = JSONObject()
            obj.put("id", cfg.id)
            obj.put("label", cfg.label)
            obj.put("visible", cfg.isVisible)
            obj.put("order", cfg.order)
            arr.put(obj)
        }
        return arr.toString()
    }

    private fun parseJson(json: String): List<CardConfig> {
        val arr = JSONArray(json)
        return (0 until arr.length()).map { i ->
            val obj = arr.getJSONObject(i)
            CardConfig(
                id = obj.getString("id"),
                label = obj.optString("label", obj.getString("id")),
                isVisible = obj.optBoolean("visible", true),
                order = obj.optInt("order", i)
            )
        }.sortedBy { it.order }
    }
}