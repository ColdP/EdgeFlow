package btm.m.edgeflow.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.core.floatPreferencesKey
import btm.m.edgeflow.sidebar.model.CardOrderDefaults
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * DataStore-backed user preferences for EdgeFlow.
 */
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "edgeflow_prefs")

object UserPreferences {
    private val KEY_HEADER_IMAGE_URI = stringPreferencesKey("header_image_uri")
    private val KEY_SIDEBAR_WIDTH_RATIO = floatPreferencesKey("sidebar_width_ratio")
    private val KEY_BLUR_RADIUS = floatPreferencesKey("blur_radius")
    private val KEY_PANEL_ALPHA = floatPreferencesKey("panel_alpha")
    private val KEY_BG_IMAGE_URI = stringPreferencesKey("bg_image_uri")
    private val KEY_BG_IMAGE_ALPHA = floatPreferencesKey("bg_image_alpha")
    private val KEY_BG_IMAGE_BLUR = floatPreferencesKey("bg_image_blur")

    /** Flow of the user's custom header image URI. Null if not set. */
    fun headerImageUriFlow(context: Context): Flow<String?> {
        return context.dataStore.data.map { prefs ->
            prefs[KEY_HEADER_IMAGE_URI]
        }
    }

    /** Persists the user's chosen header image URI. */
    suspend fun setHeaderImageUri(context: Context, uri: String?) {
        context.dataStore.edit { prefs ->
            if (uri.isNullOrBlank()) {
                prefs.remove(KEY_HEADER_IMAGE_URI)
            } else {
                prefs[KEY_HEADER_IMAGE_URI] = uri
            }
        }
    }

    /** Flow of the sidebar width ratio (0.5–1.0). Default 0.7. */
    fun sidebarWidthRatioFlow(context: Context): Flow<Float> {
        return context.dataStore.data.map { prefs ->
            prefs[KEY_SIDEBAR_WIDTH_RATIO] ?: 0.70f
        }
    }

    /** Persists the sidebar width ratio. */
    suspend fun setSidebarWidthRatio(context: Context, ratio: Float) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SIDEBAR_WIDTH_RATIO] = ratio.coerceIn(0.5f, 1.0f)
        }
    }

    fun blurRadiusFlow(context: Context): Flow<Float> =
        context.dataStore.data.map { it[KEY_BLUR_RADIUS] ?: 35f }

    suspend fun setBlurRadius(context: Context, value: Float) {
        context.dataStore.edit { it[KEY_BLUR_RADIUS] = value.coerceIn(0f, 100f) }
    }

    fun panelAlphaFlow(context: Context): Flow<Float> =
        context.dataStore.data.map { it[KEY_PANEL_ALPHA] ?: 0.65f }

    suspend fun setPanelAlpha(context: Context, value: Float) {
        context.dataStore.edit { it[KEY_PANEL_ALPHA] = value.coerceIn(0.1f, 1f) }
    }

    fun bgImageUriFlow(context: Context): Flow<String?> =
        context.dataStore.data.map { it[KEY_BG_IMAGE_URI] }

    suspend fun setBgImageUri(context: Context, uri: String?) {
        context.dataStore.edit {
            if (uri.isNullOrBlank()) it.remove(KEY_BG_IMAGE_URI) else it[KEY_BG_IMAGE_URI] = uri
        }
    }

    fun bgImageAlphaFlow(context: Context): Flow<Float> =
        context.dataStore.data.map { it[KEY_BG_IMAGE_ALPHA] ?: 0.5f }

    suspend fun setBgImageAlpha(context: Context, value: Float) {
        context.dataStore.edit { it[KEY_BG_IMAGE_ALPHA] = value.coerceIn(0f, 1f) }
    }

    fun bgImageBlurFlow(context: Context): Flow<Float> =
        context.dataStore.data.map { it[KEY_BG_IMAGE_BLUR] ?: 15f }

    suspend fun setBgImageBlur(context: Context, value: Float) {
        context.dataStore.edit { it[KEY_BG_IMAGE_BLUR] = value.coerceIn(0f, 50f) }
    }

    // ── Trigger Settings ──────────────────────────────────────────────────

    private val KEY_TRIGGER_METHOD = floatPreferencesKey("trigger_method")      // 0=swipe, 1=double-swipe, 2=ball
    private val KEY_TRIGGER_VIBRATE = floatPreferencesKey("trigger_vibrate")    // 0/1
    private val KEY_TRIGGER_POSITION = floatPreferencesKey("trigger_position")  // 0=left, 1=right
    private val KEY_TRIGGER_WIDTH = floatPreferencesKey("trigger_width")
    private val KEY_TRIGGER_HEIGHT = floatPreferencesKey("trigger_height")
    private val KEY_TRIGGER_Y_OFFSET = floatPreferencesKey("trigger_y_offset")
    private val KEY_TRIGGER_SENSITIVITY = floatPreferencesKey("trigger_sensitivity")
    private val KEY_TRIGGER_DEBUG = floatPreferencesKey("trigger_debug")        // 0/1 highlight mode

    fun triggerMethodFlow(ctx: Context): Flow<Int> = ctx.dataStore.data.map { (it[KEY_TRIGGER_METHOD] ?: 0f).toInt() }
    suspend fun setTriggerMethod(ctx: Context, v: Int) { ctx.dataStore.edit { it[KEY_TRIGGER_METHOD] = v.toFloat() } }

    fun triggerVibrateFlow(ctx: Context): Flow<Boolean> = ctx.dataStore.data.map { (it[KEY_TRIGGER_VIBRATE] ?: 1f) > 0f }
    suspend fun setTriggerVibrate(ctx: Context, v: Boolean) { ctx.dataStore.edit { it[KEY_TRIGGER_VIBRATE] = if (v) 1f else 0f } }

    fun triggerPositionFlow(ctx: Context): Flow<Int> = ctx.dataStore.data.map { (it[KEY_TRIGGER_POSITION] ?: 0f).toInt() }
    suspend fun setTriggerPosition(ctx: Context, v: Int) { ctx.dataStore.edit { it[KEY_TRIGGER_POSITION] = v.toFloat() } }

    fun triggerWidthFlow(ctx: Context): Flow<Float> = ctx.dataStore.data.map { it[KEY_TRIGGER_WIDTH] ?: 24f }
    suspend fun setTriggerWidth(ctx: Context, v: Float) { ctx.dataStore.edit { it[KEY_TRIGGER_WIDTH] = v.coerceIn(8f, 100f) } }

    fun triggerHeightFlow(ctx: Context): Flow<Float> = ctx.dataStore.data.map { it[KEY_TRIGGER_HEIGHT] ?: 0.6f }
    suspend fun setTriggerHeight(ctx: Context, v: Float) { ctx.dataStore.edit { it[KEY_TRIGGER_HEIGHT] = v.coerceIn(0.2f, 1.0f) } }

    fun triggerYOffsetFlow(ctx: Context): Flow<Float> = ctx.dataStore.data.map { it[KEY_TRIGGER_Y_OFFSET] ?: 0f }
    suspend fun setTriggerYOffset(ctx: Context, v: Float) { ctx.dataStore.edit { it[KEY_TRIGGER_Y_OFFSET] = v.coerceIn(-0.4f, 0.4f) } }

    fun triggerSensitivityFlow(ctx: Context): Flow<Float> = ctx.dataStore.data.map { it[KEY_TRIGGER_SENSITIVITY] ?: 40f }
    suspend fun setTriggerSensitivity(ctx: Context, v: Float) { ctx.dataStore.edit { it[KEY_TRIGGER_SENSITIVITY] = v.coerceIn(10f, 120f) } }

    fun triggerDebugFlow(ctx: Context): Flow<Boolean> = ctx.dataStore.data.map { (it[KEY_TRIGGER_DEBUG] ?: 0f) > 0f }
    suspend fun setTriggerDebug(ctx: Context, v: Boolean) { ctx.dataStore.edit { it[KEY_TRIGGER_DEBUG] = if (v) 1f else 0f } }

    // ── Card Order ────────────────────────────────────────────────────────

    private val KEY_CARD_ORDER = stringPreferencesKey("card_order")

    /** Flow of card order IDs (comma-separated). Default: "apps,shortcut,media,system,clipboard" */
    fun cardOrderFlow(context: Context): Flow<List<String>> =
        context.dataStore.data.map { prefs ->
            val raw = prefs[KEY_CARD_ORDER]
            if (raw.isNullOrBlank()) CardOrderDefaults.DEFAULT_ORDER
            else raw.split(",").filter { it.isNotBlank() }
        }

    suspend fun setCardOrder(context: Context, order: List<String>) {
        context.dataStore.edit { it[KEY_CARD_ORDER] = order.joinToString(",") }
    }
}
