package com.alex.steamshowcase

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.alex.steamshowcase.data.Achievement
import com.alex.steamshowcase.data.GameAchievements
import com.alex.steamshowcase.data.Prefs
import com.alex.steamshowcase.data.ProfileData
import com.alex.steamshowcase.data.SteamApi
import com.alex.steamshowcase.data.TileConfig
import com.alex.steamshowcase.data.TileType
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ShowcaseViewModel(app: Application) : AndroidViewModel(app) {

    val prefs = Prefs(app)

    var tiles by mutableStateOf(prefs.tiles)
        private set
    var data by mutableStateOf(ProfileData())
        private set
    var loading by mutableStateOf(false)
        private set
    var editMode by mutableStateOf(false)
    var showSettings by mutableStateOf(!prefs.isConfigured)

    // bump this to force the UI to re-read prefs (accent, columns...)
    var prefsVersion by mutableStateOf(0)
        private set

    private var refreshJob: Job? = null
    private var autoJob: Job? = null

    init {
        if (prefs.isConfigured) refresh()
        startAutoRefresh()
    }

    fun prefsChanged() {
        prefsVersion++
        startAutoRefresh()
    }

    private fun startAutoRefresh() {
        autoJob?.cancel()
        val minutes = prefs.autoRefreshMinutes
        if (minutes <= 0) return
        autoJob = viewModelScope.launch {
            while (true) {
                delay(minutes * 60_000L)
                if (prefs.isConfigured && !editMode) refresh()
            }
        }
    }

    // ------------------------------------------------------------------ tiles

    private fun saveTiles(list: List<TileConfig>) {
        tiles = list
        prefs.tiles = list
    }

    fun addTile(type: TileType) {
        saveTiles(tiles + TileConfig(type = type))
        // new tile may need data we haven't fetched yet
        refresh()
    }

    fun removeTile(uid: String) = saveTiles(tiles.filterNot { it.uid == uid })

    fun updateTile(updated: TileConfig) {
        saveTiles(tiles.map { if (it.uid == updated.uid) updated else it })
        // config changes (new game picked, more items) can need more data
        refresh()
    }

    fun moveTile(uid: String, delta: Int) {
        val idx = tiles.indexOfFirst { it.uid == uid }
        if (idx < 0) return
        val target = (idx + delta).coerceIn(0, tiles.lastIndex)
        if (target == idx) return
        val m = tiles.toMutableList()
        val t = m.removeAt(idx)
        m.add(target, t)
        saveTiles(m)
    }

    fun resetLayout() = saveTiles(TileConfig.defaultLayout())

    // ------------------------------------------------------------------ data

    fun refresh() {
        if (!prefs.isConfigured) return
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            loading = true
            data = fetchAll(data)
            loading = false
        }
    }

    private suspend fun fetchAll(previous: ProfileData): ProfileData = coroutineScope {
        val api = SteamApi(prefs.apiKey)
        val id = prefs.steamId
        val errors = java.util.Collections.synchronizedList(ArrayList<String>())
        val types = tiles.map { it.type }.toSet()

        suspend fun <T> guard(label: String, block: suspend () -> T): T? = try {
            block()
        } catch (e: Exception) {
            errors.add("$label: ${e.message ?: e.javaClass.simpleName}")
            null
        }

        val needGames = types.any {
            it == TileType.RECENT_GAMES || it == TileType.RECENT_ACHIEVEMENTS ||
                it == TileType.ACHIEVEMENT_SHOWCASE || it == TileType.FAVOURITE_GAME || it == TileType.LEVEL_BADGES
        }

        val summaryD = async { guard("Profile") { api.getPlayerSummary(id) } }
        val itemsD = async { if (TileType.PROFILE_HEADER in types) api.getProfileItems(id) else previous.items }
        val levelD = async { if (TileType.PROFILE_HEADER in types || TileType.LEVEL_BADGES in types) guard("Level") { api.getSteamLevel(id) } else previous.level }
        val badgesD = async { if (TileType.LEVEL_BADGES in types) guard("Badges") { api.getBadges(id) } else previous.badges }
        val recentD = async { if (needGames) guard("Recent games") { api.getRecentlyPlayed(id, 20) } else null }
        val ownedD = async { if (needGames) guard("Owned games") { api.getOwnedGames(id) } else null }
        val shotsD = async {
            val n = tiles.filter { it.type == TileType.SCREENSHOTS }.maxOfOrNull { it.settingInt("count", 6) } ?: 0
            if (n > 0) guard("Screenshots") { api.getScreenshots(id) } else null
        }

        val summary = summaryD.await()
        val recent = recentD.await() ?: previous.recentGames
        val owned = ownedD.await() ?: previous.ownedGames

        // Which games need achievement data?
        val wantedApps = LinkedHashSet<Int>()
        tiles.filter { it.type == TileType.ACHIEVEMENT_SHOWCASE || it.type == TileType.FAVOURITE_GAME }
            .forEach { t -> t.setting("appId").toIntOrNull()?.let { wantedApps.add(it) } }
        if (TileType.RECENT_ACHIEVEMENTS in types) {
            recent.take(6).forEach { wantedApps.add(it.appId) }
        }
        // Also the game currently being played, if any
        summary?.gameId?.toIntOrNull()?.let { if (TileType.RECENT_ACHIEVEMENTS in types) wantedApps.add(it) }

        val nameOf = { appId: Int -> (recent + owned).firstOrNull { it.appId == appId }?.name }
        val achD = wantedApps.map { appId ->
            async { appId to guard("Achievements $appId") { api.getGameAchievements(id, appId, nameOf(appId)) } }
        }
        val achMap = HashMap<Int, GameAchievements>(previous.achievementsByApp)
        achD.awaitAll().forEach { (appId, ga) -> if (ga != null) achMap[appId] = ga }

        val recentAch: List<Achievement> = achMap.values
            .flatMap { it.achievements }
            .filter { it.achieved && it.unlockTime > 0 }
            .sortedByDescending { it.unlockTime }
            .take(50)

        ProfileData(
            summary = summary ?: previous.summary,
            items = itemsD.await(),
            level = levelD.await() ?: previous.level,
            badges = badgesD.await() ?: previous.badges,
            recentGames = recent,
            ownedGames = owned,
            achievementsByApp = achMap,
            recentAchievements = recentAch,
            screenshots = shotsD.await() ?: previous.screenshots,
            errors = errors.toList(),
            loadedAt = System.currentTimeMillis(),
        )
    }
}
