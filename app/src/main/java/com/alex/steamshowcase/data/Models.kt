package com.alex.steamshowcase.data

data class PlayerSummary(
    val steamId: String,
    val personaName: String,
    val realName: String?,
    val avatarFull: String,
    val profileUrl: String,
    val personaState: Int,          // 0 offline, 1 online, 2 busy, 3 away, 4 snooze, 5 trade, 6 play
    val gameName: String?,          // set when in-game
    val gameId: String?,
    val countryCode: String?,
    val timeCreated: Long,          // unix seconds, 0 if hidden
    val lastLogoff: Long,
) {
    val statusText: String
        get() = when {
            !gameName.isNullOrBlank() -> "In-Game"
            personaState == 0 -> "Offline"
            personaState == 1 -> "Online"
            personaState == 2 -> "Busy"
            personaState == 3 -> "Away"
            personaState == 4 -> "Snooze"
            personaState == 5 -> "Looking to trade"
            personaState == 6 -> "Looking to play"
            else -> "Online"
        }
}

data class ProfileItems(
    val backgroundImage: String?,      // full URL or null
    val backgroundMovie: String?,      // webm URL or null (not rendered, kept for later)
    val avatarFrame: String?,          // full URL or null
    val animatedAvatar: String?,       // full URL or null
)

data class Badge(
    val badgeId: Int,
    val level: Int,
    val xp: Int,
    val appId: Int?,                 // game badge when set
    val completionTime: Long,
    val scarcity: Int,
)

data class BadgeSummary(
    val level: Int,
    val xp: Int,
    val xpNeededToLevelUp: Int,
    val xpNeededCurrentLevel: Int,
    val badges: List<Badge>,
)

data class Game(
    val appId: Int,
    val name: String,
    val playtimeForeverMin: Int,
    val playtime2WeeksMin: Int,
    val iconHash: String?,
) {
    val headerUrl get() = "https://cdn.cloudflare.steamstatic.com/steam/apps/$appId/header.jpg"
    val capsuleUrl get() = "https://cdn.cloudflare.steamstatic.com/steam/apps/$appId/capsule_231x87.jpg"
    val libraryHeroUrl get() = "https://cdn.cloudflare.steamstatic.com/steam/apps/$appId/library_hero.jpg"
    val iconUrl: String?
        get() = iconHash?.let { "https://media.steampowered.com/steamcommunity/public/images/apps/$appId/$it.jpg" }
}

data class Achievement(
    val apiName: String,
    val displayName: String,
    val description: String?,
    val iconUrl: String,
    val iconGrayUrl: String,
    val achieved: Boolean,
    val unlockTime: Long,
    val hidden: Boolean,
    val appId: Int,
    val gameName: String,
)

data class GameAchievements(
    val appId: Int,
    val gameName: String,
    val achievements: List<Achievement>,
) {
    val unlocked get() = achievements.count { it.achieved }
    val total get() = achievements.size
}

data class Screenshot(
    val thumbUrl: String,
    val fullUrl: String,
    val title: String?,
)

/** Everything the tiles can draw from. Any piece may be null if it hasn't loaded or failed. */
data class ProfileData(
    val summary: PlayerSummary? = null,
    val items: ProfileItems? = null,
    val level: Int? = null,
    val badges: BadgeSummary? = null,
    val recentGames: List<Game> = emptyList(),
    val ownedGames: List<Game> = emptyList(),
    val achievementsByApp: Map<Int, GameAchievements> = emptyMap(),
    val recentAchievements: List<Achievement> = emptyList(),
    val screenshots: List<Screenshot> = emptyList(),
    val errors: List<String> = emptyList(),
    val loadedAt: Long = 0L,
)
