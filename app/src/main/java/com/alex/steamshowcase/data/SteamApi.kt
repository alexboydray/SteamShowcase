package com.alex.steamshowcase.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Thin client over the public Steam Web API (needs an API key + a public profile)
 * plus one best-effort HTML scrape for screenshots, which the Web API doesn't expose.
 */
class SteamApi(private val apiKey: String) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    class SteamException(msg: String) : IOException(msg)

    private fun apiUrl(iface: String, method: String, version: String, vararg params: Pair<String, String>): HttpUrl {
        val b = "https://api.steampowered.com/$iface/$method/$version/".toHttpUrl().newBuilder()
        b.addQueryParameter("key", apiKey)
        params.forEach { (k, v) -> b.addQueryParameter(k, v) }
        return b.build()
    }

    private suspend fun getText(url: HttpUrl, headers: Map<String, String> = emptyMap()): String =
        withContext(Dispatchers.IO) {
            val req = Request.Builder().url(url).apply {
                headers.forEach { (k, v) -> header(k, v) }
            }.build()
            client.newCall(req).execute().use { resp ->
                val body = resp.body?.string() ?: ""
                if (!resp.isSuccessful) {
                    val hint = when (resp.code) {
                        401, 403 -> "check your API key / profile privacy"
                        429 -> "rate limited, try again in a minute"
                        else -> ""
                    }
                    throw SteamException("HTTP ${resp.code} for ${url.encodedPath} $hint".trim())
                }
                body
            }
        }

    private suspend fun getJson(url: HttpUrl): JSONObject = JSONObject(getText(url))

    // ---------------------------------------------------------------- identity

    /** Accepts a 17-digit SteamID64, a vanity name, or a full profile URL and returns a SteamID64. */
    suspend fun resolveSteamId(input: String): String {
        val s = input.trim().trimEnd('/')
        if (Regex("^\\d{17}$").matches(s)) return s
        Regex("/profiles/(\\d{17})").find(s)?.let { return it.groupValues[1] }
        val vanity = Regex("/id/([^/?#]+)").find(s)?.groupValues?.get(1) ?: s
        val json = getJson(apiUrl("ISteamUser", "ResolveVanityURL", "v1", "vanityurl" to vanity))
        val r = json.getJSONObject("response")
        if (r.optInt("success") != 1) throw SteamException("Could not resolve \"$vanity\" to a Steam ID")
        return r.getString("steamid")
    }

    // ---------------------------------------------------------------- profile

    suspend fun getPlayerSummary(steamId: String): PlayerSummary {
        val json = getJson(apiUrl("ISteamUser", "GetPlayerSummaries", "v2", "steamids" to steamId))
        val players = json.getJSONObject("response").getJSONArray("players")
        if (players.length() == 0) throw SteamException("No player found for $steamId")
        val p = players.getJSONObject(0)
        return PlayerSummary(
            steamId = p.getString("steamid"),
            personaName = p.optString("personaname", "Unknown"),
            realName = p.optString("realname").ifBlank { null },
            avatarFull = p.optString("avatarfull"),
            profileUrl = p.optString("profileurl"),
            personaState = p.optInt("personastate", 0),
            gameName = p.optString("gameextrainfo").ifBlank { null },
            gameId = p.optString("gameid").ifBlank { null },
            countryCode = p.optString("loccountrycode").ifBlank { null },
            timeCreated = p.optLong("timecreated", 0L),
            lastLogoff = p.optLong("lastlogoff", 0L),
        )
    }

    /** Equipped profile background / avatar frame / animated avatar. Best-effort; returns nulls on anything odd. */
    suspend fun getProfileItems(steamId: String): ProfileItems {
        val base = "https://cdn.cloudflare.steamstatic.com/steamcommunity/public/images/"
        return try {
            val json = getJson(apiUrl("IPlayerService", "GetProfileItemsEquipped", "v1", "steamid" to steamId))
            val r = json.getJSONObject("response")
            fun pick(obj: String, field: String): String? {
                val o = r.optJSONObject(obj) ?: return null
                val v = o.optString(field).ifBlank { return null }
                return if (v.startsWith("http")) v else base + v
            }
            ProfileItems(
                backgroundImage = pick("profile_background", "image_large"),
                backgroundMovie = pick("profile_background", "movie_webm"),
                avatarFrame = pick("avatar_frame", "image_small"),
                animatedAvatar = pick("animated_avatar", "image_small"),
            )
        } catch (e: Exception) {
            ProfileItems(null, null, null, null)
        }
    }

    suspend fun getSteamLevel(steamId: String): Int {
        val json = getJson(apiUrl("IPlayerService", "GetSteamLevel", "v1", "steamid" to steamId))
        return json.getJSONObject("response").optInt("player_level", 0)
    }

    suspend fun getBadges(steamId: String): BadgeSummary {
        val json = getJson(apiUrl("IPlayerService", "GetBadges", "v1", "steamid" to steamId))
        val r = json.getJSONObject("response")
        val arr = r.optJSONArray("badges") ?: JSONArray()
        val badges = (0 until arr.length()).map { i ->
            val b = arr.getJSONObject(i)
            Badge(
                badgeId = b.optInt("badgeid"),
                level = b.optInt("level"),
                xp = b.optInt("xp"),
                appId = if (b.has("appid")) b.optInt("appid") else null,
                completionTime = b.optLong("completion_time"),
                scarcity = b.optInt("scarcity"),
            )
        }
        return BadgeSummary(
            level = r.optInt("player_level"),
            xp = r.optInt("player_xp"),
            xpNeededToLevelUp = r.optInt("player_xp_needed_to_level_up"),
            xpNeededCurrentLevel = r.optInt("player_xp_needed_current_level"),
            badges = badges,
        )
    }

    // ---------------------------------------------------------------- games

    private fun parseGames(arr: JSONArray?): List<Game> {
        if (arr == null) return emptyList()
        return (0 until arr.length()).map { i ->
            val g = arr.getJSONObject(i)
            Game(
                appId = g.optInt("appid"),
                name = g.optString("name", "App ${g.optInt("appid")}"),
                playtimeForeverMin = g.optInt("playtime_forever"),
                playtime2WeeksMin = g.optInt("playtime_2weeks"),
                iconHash = g.optString("img_icon_url").ifBlank { null },
            )
        }
    }

    suspend fun getRecentlyPlayed(steamId: String, count: Int = 10): List<Game> {
        val json = getJson(apiUrl("IPlayerService", "GetRecentlyPlayedGames", "v1", "steamid" to steamId, "count" to count.toString()))
        return parseGames(json.getJSONObject("response").optJSONArray("games"))
    }

    suspend fun getOwnedGames(steamId: String): List<Game> {
        val json = getJson(
            apiUrl(
                "IPlayerService", "GetOwnedGames", "v1",
                "steamid" to steamId, "include_appinfo" to "1", "include_played_free_games" to "1"
            )
        )
        return parseGames(json.getJSONObject("response").optJSONArray("games"))
            .sortedByDescending { it.playtimeForeverMin }
    }

    // ---------------------------------------------------------------- achievements

    /** Unlock state merged with the game schema (names / icons). Returns null if the game has no achievements. */
    suspend fun getGameAchievements(steamId: String, appId: Int, gameNameHint: String? = null): GameAchievements? {
        val schemaJson = getJson(apiUrl("ISteamUserStats", "GetSchemaForGame", "v2", "appid" to appId.toString()))
        val game = schemaJson.optJSONObject("game") ?: return null
        val gameName = game.optString("gameName").ifBlank { gameNameHint ?: "App $appId" }
        val schemaArr = game.optJSONObject("availableGameStats")?.optJSONArray("achievements") ?: return null
        if (schemaArr.length() == 0) return null

        val playerJson = try {
            getJson(apiUrl("ISteamUserStats", "GetPlayerAchievements", "v1", "steamid" to steamId, "appid" to appId.toString()))
        } catch (e: SteamException) {
            // 400 = "Requested app has no stats" for this player
            return null
        }
        val playerArr = playerJson.optJSONObject("playerstats")?.optJSONArray("achievements") ?: JSONArray()
        val state = HashMap<String, Pair<Boolean, Long>>()
        for (i in 0 until playerArr.length()) {
            val a = playerArr.getJSONObject(i)
            state[a.optString("apiname")] = (a.optInt("achieved") == 1) to a.optLong("unlocktime")
        }

        val list = (0 until schemaArr.length()).map { i ->
            val s = schemaArr.getJSONObject(i)
            val api = s.optString("name")
            val st = state[api]
            Achievement(
                apiName = api,
                displayName = s.optString("displayName", api),
                description = s.optString("description").ifBlank { null },
                iconUrl = s.optString("icon"),
                iconGrayUrl = s.optString("icongray"),
                achieved = st?.first ?: false,
                unlockTime = st?.second ?: 0L,
                hidden = s.optInt("hidden") == 1,
                appId = appId,
                gameName = gameName,
            )
        }
        return GameAchievements(appId, gameName, list)
    }

    // ---------------------------------------------------------------- screenshots (scrape)

    /**
     * There is no Web API for user screenshots, so this reads the public community page.
     * It will silently return an empty list if Valve changes the markup or the profile is private.
     */
    suspend fun getScreenshots(steamId: String, page: Int = 1): List<Screenshot> {
        val url = "https://steamcommunity.com/profiles/$steamId/screenshots/".toHttpUrl().newBuilder()
            .addQueryParameter("p", page.toString())
            .addQueryParameter("sort", "newestfirst")
            .addQueryParameter("browsefilter", "myfiles")
            .addQueryParameter("view", "grid")
            .addQueryParameter("l", "english")
            .build()
        val html = try {
            getText(url, mapOf("User-Agent" to "Mozilla/5.0 (Linux; Android 13) SteamShowcase/1.0", "Accept-Language" to "en"))
        } catch (e: Exception) {
            return emptyList()
        }
        // Each tile: <div class="imgWallItem ..." ...> ... <img ... src="https://steamuserimages-a.akamaihd.net/ugc/XXXX/YYYY/?imw=...">
        val imgRegex = Regex("""<img[^>]+src="(https://steamuserimages-a\.akamaihd\.net/ugc/[^"?]+)[^"]*"[^>]*>""")
        val seen = LinkedHashSet<String>()
        imgRegex.findAll(html).forEach { m -> seen.add(m.groupValues[1]) }
        return seen.map { base ->
            Screenshot(
                thumbUrl = "$base?imw=512&imh=288&ima=fit&impolicy=Letterbox&imcolor=%23000000&letterbox=true",
                fullUrl = "$base?imw=1920&imh=1080&ima=fit&impolicy=Letterbox&imcolor=%23000000&letterbox=true",
                title = null,
            )
        }
    }
}
