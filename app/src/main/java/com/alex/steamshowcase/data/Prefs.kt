package com.alex.steamshowcase.data

import android.content.Context
import android.content.SharedPreferences

class Prefs(context: Context) {
    private val sp: SharedPreferences = context.getSharedPreferences("steam_showcase", Context.MODE_PRIVATE)

    var apiKey: String
        get() = sp.getString("api_key", "") ?: ""
        set(v) = sp.edit().putString("api_key", v.trim()).apply()

    /** Resolved SteamID64. */
    var steamId: String
        get() = sp.getString("steam_id", "") ?: ""
        set(v) = sp.edit().putString("steam_id", v.trim()).apply()

    /** Whatever the user typed (vanity / URL) so we can show it back. */
    var steamIdInput: String
        get() = sp.getString("steam_id_input", "") ?: ""
        set(v) = sp.edit().putString("steam_id_input", v.trim()).apply()

    var columns: Int
        get() = sp.getInt("columns", 2)
        set(v) = sp.edit().putInt("columns", v.coerceIn(1, 4)).apply()

    /** Accent colour as 0xAARRGGBB. */
    var accent: Long
        get() = sp.getLong("accent", 0xFF66C0F4)
        set(v) = sp.edit().putLong("accent", v).apply()

    var useProfileBackground: Boolean
        get() = sp.getBoolean("use_profile_bg", true)
        set(v) = sp.edit().putBoolean("use_profile_bg", v).apply()

    var keepScreenOn: Boolean
        get() = sp.getBoolean("keep_screen_on", false)
        set(v) = sp.edit().putBoolean("keep_screen_on", v).apply()

    var autoRefreshMinutes: Int
        get() = sp.getInt("auto_refresh_min", 10)
        set(v) = sp.edit().putInt("auto_refresh_min", v.coerceIn(0, 240)).apply()

    var tiles: List<TileConfig>
        get() {
            val raw = sp.getString("tiles", null)
            return if (raw == null) TileConfig.defaultLayout() else TileConfig.listFromJson(raw)
        }
        set(v) = sp.edit().putString("tiles", TileConfig.listToJson(v)).apply()

    val isConfigured: Boolean get() = apiKey.isNotBlank() && steamId.isNotBlank()
}
