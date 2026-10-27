package com.alex.steamshowcase.ui.tiles

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.alex.steamshowcase.data.Achievement
import com.alex.steamshowcase.data.ProfileData
import com.alex.steamshowcase.data.TileConfig
import com.alex.steamshowcase.ui.EmptyNote
import com.alex.steamshowcase.ui.SteamColors
import com.alex.steamshowcase.ui.TileActions
import com.alex.steamshowcase.ui.TileCard
import com.alex.steamshowcase.ui.formatRelative

@Composable
fun AchievementShowcaseTile(cfg: TileConfig, data: ProfileData, actions: TileActions?) {
    val appId = cfg.setting("appId").toIntOrNull()
    val ga = appId?.let { data.achievementsByApp[it] }
    val title = cfg.title ?: ga?.gameName?.let { "$it achievements" } ?: "Achievement showcase"
    TileCard(title = title, actions = actions) {
        if (appId == null) {
            EmptyNote("Tap the gear to pick a game")
            return@TileCard
        }
        if (ga == null) {
            EmptyNote(if (data.loadedAt == 0L) "Loading…" else "No achievement data for this game")
            return@TileCard
        }
        LinearProgressIndicator(
            progress = { if (ga.total == 0) 0f else ga.unlocked.toFloat() / ga.total },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = SteamColors.PanelLight,
        )
        Text(
            "${ga.unlocked} of ${ga.total} unlocked",
            color = SteamColors.TextDim, style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(start = 14.dp),
        )
        val onlyUnlocked = cfg.settingBool("onlyUnlocked", true)
        val list = ga.achievements
            .filter { if (onlyUnlocked) it.achieved else true }
            .sortedWith(compareByDescending<Achievement> { it.achieved }.thenByDescending { it.unlockTime })
            .take(cfg.settingInt("count", 8))
        if (list.isEmpty()) {
            EmptyNote("No achievements unlocked yet")
            return@TileCard
        }
        IconGrid(list, showGame = false)
    }
}

@Composable
fun RecentAchievementsTile(cfg: TileConfig, data: ProfileData, actions: TileActions?) {
    val list = data.recentAchievements.take(cfg.settingInt("count", 10))
    TileCard(title = cfg.title ?: "Recent achievements", actions = actions) {
        if (list.isEmpty()) {
            EmptyNote(if (data.loadedAt == 0L) "Loading…" else "No recent unlocks in your recently played games")
            return@TileCard
        }
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            list.forEach { a -> AchievementRow(a) }
        }
    }
}

@Composable
private fun AchievementRow(a: Achievement) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        AsyncImage(
            model = if (a.achieved) a.iconUrl else a.iconGrayUrl,
            contentDescription = a.displayName,
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(SteamColors.PanelLight),
            contentScale = ContentScale.Crop,
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(a.displayName, color = Color.White, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                a.gameName + (if (a.unlockTime > 0) "  ·  " + formatRelative(a.unlockTime) else ""),
                color = SteamColors.TextDim, style = MaterialTheme.typography.bodySmall,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Steam-style row(s) of achievement icons; wraps onto new lines. */
@Composable
private fun IconGrid(list: List<Achievement>, showGame: Boolean) {
    val perRow = 8
    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        list.chunked(perRow).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { a ->
                    AsyncImage(
                        model = if (a.achieved) a.iconUrl else a.iconGrayUrl,
                        contentDescription = a.displayName,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(SteamColors.PanelLight)
                            .alpha(if (a.achieved) 1f else 0.45f),
                        contentScale = ContentScale.Fit,
                    )
                }
                // keep icons the same size when the last row is short
                repeat(perRow - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
        if (showGame) {
            Text(list.first().gameName, color = SteamColors.TextDim, style = MaterialTheme.typography.bodySmall)
        }
    }
}
