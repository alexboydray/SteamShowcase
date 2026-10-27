package com.alex.steamshowcase.ui.tiles

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.alex.steamshowcase.data.ProfileData
import com.alex.steamshowcase.data.TileConfig
import com.alex.steamshowcase.ui.EmptyNote
import com.alex.steamshowcase.ui.SteamColors
import com.alex.steamshowcase.ui.TileActions
import com.alex.steamshowcase.ui.TileCard
import com.alex.steamshowcase.ui.formatDate
import com.alex.steamshowcase.ui.formatRelative
import java.util.Locale

@Composable
fun ProfileHeaderTile(cfg: TileConfig, data: ProfileData, actions: TileActions?, useProfileBackground: Boolean) {
    val s = data.summary
    TileCard(title = cfg.title, actions = actions) {
        if (s == null) {
            EmptyNote("Profile not loaded yet")
            return@TileCard
        }
        val bg = data.items?.backgroundImage?.takeIf { useProfileBackground }
        Box(Modifier.fillMaxWidth()) {
            if (bg != null) {
                AsyncImage(
                    model = bg, contentDescription = null,
                    modifier = Modifier.matchParentSize(),
                    contentScale = ContentScale.Crop,
                )
                Box(
                    Modifier
                        .matchParentSize()
                        .background(
                            Brush.horizontalGradient(
                                listOf(SteamColors.Panel.copy(alpha = 0.92f), SteamColors.Panel.copy(alpha = 0.55f))
                            )
                        )
                )
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val statusColor = when {
                    s.gameName != null -> SteamColors.InGame
                    s.personaState == 0 -> SteamColors.Offline
                    else -> SteamColors.Online
                }
                Box(Modifier.size(84.dp), contentAlignment = Alignment.Center) {
                    AsyncImage(
                        model = data.items?.animatedAvatar ?: s.avatarFull,
                        contentDescription = "Avatar",
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .border(2.dp, statusColor, RoundedCornerShape(4.dp)),
                        contentScale = ContentScale.Crop,
                    )
                    data.items?.avatarFrame?.let {
                        AsyncImage(model = it, contentDescription = null, modifier = Modifier.size(84.dp))
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            s.personaName,
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        data.level?.let { lvl ->
                            Spacer(Modifier.width(10.dp))
                            LevelBubble(lvl)
                        }
                    }
                    if (cfg.settingBool("showRealName", true) && s.realName != null) {
                        Text(s.realName, color = SteamColors.TextDim, style = MaterialTheme.typography.bodyMedium)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (s.gameName != null) "Playing ${s.gameName}" else s.statusText,
                        color = statusColor,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                    val extras = buildList {
                        if (cfg.settingBool("showCountry", true) && s.countryCode != null) add(countryName(s.countryCode))
                        if (cfg.settingBool("showMemberSince", true) && s.timeCreated > 0) add("Member since ${formatDate(s.timeCreated)}")
                        if (s.gameName == null && s.personaState == 0 && s.lastLogoff > 0) add("Last online ${formatRelative(s.lastLogoff)}")
                    }
                    if (extras.isNotEmpty()) {
                        Text(
                            extras.joinToString("  ·  "),
                            color = SteamColors.TextDim,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2, overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LevelBubble(level: Int, size: Int = 32) {
    val ring = levelColor(level)
    Box(
        Modifier
            .size(size.dp)
            .clip(CircleShape)
            .border(2.dp, ring, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(level.toString(), color = ring, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
    }
}

/** Rough copy of Steam's level ring colours (they change every 10 levels, cycle every 100). */
fun levelColor(level: Int): Color {
    val palette = listOf(
        0xFF9B9B9B, 0xFFC02942, 0xFFB45FC9, 0xFF2E6CF0, 0xFF4EA1FF,
        0xFF7FC32F, 0xFFC4A800, 0xFFF08C2E, 0xFFFF4F4F, 0xFFC4AF81,
    )
    return Color(palette[((level % 100) / 10).coerceIn(0, 9)])
}

private fun countryName(code: String): String =
    try { Locale("", code).displayCountry.ifBlank { code } } catch (e: Exception) { code }

@Composable
fun LevelBadgesTile(cfg: TileConfig, data: ProfileData, actions: TileActions?) {
    val b = data.badges
    TileCard(title = cfg.title ?: "Level & badges", actions = actions) {
        if (b == null) {
            EmptyNote("Badges not loaded yet")
            return@TileCard
        }
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                LevelBubble(b.level, 46)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Level ${b.level}", style = MaterialTheme.typography.titleMedium, color = Color.White)
                    val span = (b.xpNeededToLevelUp + (b.xp - b.xpNeededCurrentLevel)).coerceAtLeast(1)
                    val progress = ((b.xp - b.xpNeededCurrentLevel).toFloat() / span).coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = levelColor(b.level),
                        trackColor = SteamColors.PanelLight,
                    )
                    Text(
                        "${b.xp} XP  ·  ${b.xpNeededToLevelUp} to level ${b.level + 1}",
                        color = SteamColors.TextDim, style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "${b.badges.size} badges",
                color = SteamColors.TextDim, style = MaterialTheme.typography.labelSmall,
            )
            val top = b.badges.sortedByDescending { it.xp }.take(cfg.settingInt("badgeCount", 6))
            val names = (data.ownedGames + data.recentGames).associate { it.appId to it.name }
            top.forEach { badge ->
                val label = when {
                    badge.appId != null -> names[badge.appId] ?: "Game badge (app ${badge.appId})"
                    else -> steamBadgeName(badge.badgeId)
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        label, color = SteamColors.Text, style = MaterialTheme.typography.bodySmall,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
                    )
                    Text(
                        "Lv ${badge.level}  ·  ${badge.xp} XP",
                        color = SteamColors.TextDim, style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

/** Names for the non-game (Steam-wide) badges. Unknown ids fall back to the id. */
private fun steamBadgeName(id: Int): String = when (id) {
    1 -> "Years of Service"
    2 -> "Community Ambassador"
    3 -> "The Potato Sack"
    4 -> "The Great Steam Treasure Hunt"
    5 -> "Steam Summer Camp"
    6 -> "Steam Holiday Sale 2011"
    7 -> "Steam Summer Sale 2012"
    8 -> "Steam Holiday Sale 2012"
    9 -> "Steam Community Translator"
    10 -> "Steam Community Moderator"
    11 -> "Valve Employee"
    12 -> "Steam Hardware Beta"
    13 -> "Game Collector"
    14 -> "Trading Card Beta Tester"
    15 -> "Steam Hardware"
    16 -> "Steam Hardware Sale"
    17 -> "Red Herring"
    18 -> "Steam Workshop Contributor"
    19 -> "Steam Sale 2013"
    20 -> "Steam Holiday Sale 2013"
    21 -> "Steam Summer Sale 2014"
    22 -> "Steam Auction"
    23 -> "Holiday Sale 2014"
    24 -> "Steam Summer Sale 2015"
    25 -> "Steam Broadcast Beta"
    26 -> "Steam Awards Nomination"
    27 -> "Steam Holiday Sale 2015"
    28 -> "Steam Summer Sale 2016"
    29 -> "Steam Awards"
    30 -> "Steam Link"
    31 -> "Steam Awards 2017"
    32 -> "Steam Summer Sale 2017"
    33 -> "Steam Winter Sale 2017"
    34 -> "Steam Spring Cleaning"
    35 -> "Steam Summer Sale 2018"
    36 -> "Steam Winter Sale 2018"
    37 -> "Steam Lunar New Year 2019"
    38 -> "Steam Summer Sale 2019"
    39 -> "Steam Winter Sale 2019"
    40 -> "Steam Awards 2019"
    41 -> "Steam Lunar New Year 2020"
    42 -> "Steam Game Festival"
    43 -> "Steam Summer Sale 2020"
    44 -> "Steam Winter Sale 2020"
    45 -> "Steam Lunar New Year 2021"
    46 -> "Steam Summer Sale 2021"
    47 -> "Steam Winter Sale 2021"
    48 -> "Steam Summer Sale 2022"
    49 -> "Steam Winter Sale 2022"
    else -> "Steam badge #$id"
}
