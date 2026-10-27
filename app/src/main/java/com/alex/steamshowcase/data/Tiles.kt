package com.alex.steamshowcase.data

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * A "showcase plugin". Add a new one here plus a composable in ui/tiles and it shows up
 * in the Add Tile sheet automatically.
 */
enum class TileType(
    val id: String,
    val title: String,
    val blurb: String,
    val defaultSize: TileSize,
    val defaults: Map<String, String> = emptyMap(),
) {
    PROFILE_HEADER(
        "profile_header", "Profile header",
        "Avatar, name, status, level and Steam background.",
        TileSize.FULL, mapOf("showRealName" to "true", "showCountry" to "true", "showMemberSince" to "true")
    ),
    LEVEL_BADGES(
        "level_badges", "Level & badges",
        "Steam level, XP progress and your top badges.",
        TileSize.HALF, mapOf("badgeCount" to "6")
    ),
    RECENT_GAMES(
        "recent_games", "Recently played",
        "What you've played in the last two weeks, with playtime.",
        TileSize.HALF, mapOf("count" to "5", "style" to "list")
    ),
    ACHIEVEMENT_SHOWCASE(
        "achievement_showcase", "Achievement showcase",
        "Pick a game and show its unlocked achievements.",
        TileSize.FULL, mapOf("appId" to "", "count" to "8", "onlyUnlocked" to "true")
    ),
    RECENT_ACHIEVEMENTS(
        "recent_achievements", "Recent achievements",
        "Your latest unlocks across recently played games.",
        TileSize.FULL, mapOf("count" to "10")
    ),
    SCREENSHOTS(
        "screenshots", "Screenshots",
        "Your newest Steam Community screenshots.",
        TileSize.FULL, mapOf("count" to "6", "columns" to "3")
    ),
    ARTWORK(
        "artwork", "Artwork gallery",
        "Your own images: pick from the device or paste URLs.",
        TileSize.FULL, mapOf("images" to "", "columns" to "2")
    ),
    FAVOURITE_GAME(
        "favourite_game", "Favourite game",
        "One game, big artwork, hours played and achievement progress.",
        TileSize.FULL, mapOf("appId" to "")
    ),
    TEXT(
        "text", "Text / info box",
        "A custom heading and some text. Like the Steam info box.",
        TileSize.HALF, mapOf("heading" to "About me", "body" to "")
    );

    companion object {
        fun byId(id: String): TileType? = entries.firstOrNull { it.id == id }
    }
}

enum class TileSize(val id: String, val label: String) {
    HALF("half", "Half width"),
    FULL("full", "Full width");

    fun toggle(): TileSize = if (this == HALF) FULL else HALF

    companion object {
        fun byId(id: String) = entries.firstOrNull { it.id == id } ?: FULL
    }
}

data class TileConfig(
    val uid: String = UUID.randomUUID().toString(),
    val type: TileType,
    val size: TileSize = type.defaultSize,
    val settings: Map<String, String> = type.defaults,
    val title: String? = null,   // optional custom heading
) {
    fun setting(key: String): String = settings[key] ?: type.defaults[key] ?: ""
    fun settingInt(key: String, fallback: Int): Int = setting(key).toIntOrNull() ?: fallback
    fun settingBool(key: String, fallback: Boolean = false): Boolean =
        setting(key).let { if (it.isBlank()) fallback else it.equals("true", true) }

    fun with(key: String, value: String) = copy(settings = settings + (key to value))

    fun toJson(): JSONObject = JSONObject().apply {
        put("uid", uid)
        put("type", type.id)
        put("size", size.id)
        put("settings", JSONObject(settings))
        put("title", title ?: JSONObject.NULL)
    }

    companion object {
        fun fromJson(o: JSONObject): TileConfig? {
            val type = TileType.byId(o.optString("type")) ?: return null
            val s = o.optJSONObject("settings") ?: JSONObject()
            val map = HashMap<String, String>()
            s.keys().forEach { k -> map[k] = s.optString(k) }
            return TileConfig(
                uid = o.optString("uid", UUID.randomUUID().toString()),
                type = type,
                size = TileSize.byId(o.optString("size")),
                settings = type.defaults + map,
                title = o.optString("title").ifBlank { null },
            )
        }

        fun listToJson(list: List<TileConfig>): String = JSONArray().apply { list.forEach { put(it.toJson()) } }.toString()

        fun listFromJson(s: String?): List<TileConfig> {
            if (s.isNullOrBlank()) return emptyList()
            return try {
                val arr = JSONArray(s)
                (0 until arr.length()).mapNotNull { fromJson(arr.getJSONObject(it)) }
            } catch (e: Exception) {
                emptyList()
            }
        }

        fun defaultLayout(): List<TileConfig> = listOf(
            TileConfig(type = TileType.PROFILE_HEADER),
            TileConfig(type = TileType.LEVEL_BADGES),
            TileConfig(type = TileType.RECENT_GAMES),
            TileConfig(type = TileType.RECENT_ACHIEVEMENTS),
        )
    }
}
