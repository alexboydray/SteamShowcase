package com.alex.steamshowcase.ui.tiles

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import com.alex.steamshowcase.data.Game
import com.alex.steamshowcase.data.ProfileData
import com.alex.steamshowcase.data.TileConfig
import com.alex.steamshowcase.ui.EmptyNote
import com.alex.steamshowcase.ui.SteamColors
import com.alex.steamshowcase.ui.TileActions
import com.alex.steamshowcase.ui.TileCard
import com.alex.steamshowcase.ui.formatPlaytime

@Composable
fun RecentGamesTile(cfg: TileConfig, data: ProfileData, actions: TileActions?) {
    val games = data.recentGames.take(cfg.settingInt("count", 5))
    val style = cfg.setting("style")
    TileCard(title = cfg.title ?: "Recent activity", actions = actions) {
        if (games.isEmpty()) {
            EmptyNote("Nothing played in the last two weeks")
            return@TileCard
        }
        val total2w = data.recentGames.sumOf { it.playtime2WeeksMin }
        Text(
            "${formatPlaytime(total2w)} past 2 weeks",
            color = SteamColors.TextDim, style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(start = 14.dp, top = 4.dp),
        )
        if (style == "capsules") {
            LazyRow(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(games, key = { it.appId }) { g -> GameCapsule(g) }
            }
        } else {
            Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                games.forEach { g -> GameRow(g) }
            }
        }
    }
}

@Composable
private fun GameRow(g: Game) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        AsyncImage(
            model = g.capsuleUrl, contentDescription = g.name,
            modifier = Modifier
                .width(92.dp)
                .height(35.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(SteamColors.PanelLight),
            contentScale = ContentScale.Crop,
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(g.name, color = Color.White, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                "${formatPlaytime(g.playtime2WeeksMin)} last 2 weeks  ·  ${formatPlaytime(g.playtimeForeverMin)} total",
                color = SteamColors.TextDim, style = MaterialTheme.typography.bodySmall,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun GameCapsule(g: Game) {
    Column(Modifier.width(150.dp)) {
        AsyncImage(
            model = g.headerUrl, contentDescription = g.name,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(460f / 215f)
                .clip(RoundedCornerShape(4.dp))
                .background(SteamColors.PanelLight),
            contentScale = ContentScale.Crop,
        )
        Text(g.name, color = Color.White, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
        Text(formatPlaytime(g.playtime2WeeksMin), color = SteamColors.TextDim, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun FavouriteGameTile(cfg: TileConfig, data: ProfileData, actions: TileActions?) {
    val appId = cfg.setting("appId").toIntOrNull()
    val game = (data.ownedGames + data.recentGames).firstOrNull { it.appId == appId }
    val ach = appId?.let { data.achievementsByApp[it] }
    TileCard(title = cfg.title ?: "Favourite game", actions = actions) {
        if (appId == null) {
            EmptyNote("Tap the gear to pick a game")
            return@TileCard
        }
        Box(Modifier.fillMaxWidth()) {
            AsyncImage(
                model = game?.libraryHeroUrl ?: "https://cdn.cloudflare.steamstatic.com/steam/apps/$appId/library_hero.jpg",
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(3f),
                contentScale = ContentScale.Crop,
            )
            Box(
                Modifier
                    .matchParentSize()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, SteamColors.Panel.copy(alpha = 0.95f))))
            )
            Column(
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(14.dp)
            ) {
                Text(
                    game?.name ?: ach?.gameName ?: "App $appId",
                    color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                val bits = buildList {
                    game?.let { add("${formatPlaytime(it.playtimeForeverMin)} on record") }
                    ach?.let { add("${it.unlocked} / ${it.total} achievements") }
                }
                if (bits.isNotEmpty()) Text(bits.joinToString("  ·  "), color = SteamColors.Text, style = MaterialTheme.typography.bodyMedium)
            }
        }
        if (ach != null && ach.total > 0) {
            LinearProgressIndicator(
                progress = { ach.unlocked.toFloat() / ach.total },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = SteamColors.PanelLight,
            )
        } else {
            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
fun TextTile(cfg: TileConfig, actions: TileActions?) {
    TileCard(title = cfg.title ?: cfg.setting("heading").ifBlank { null }, actions = actions) {
        val body = cfg.setting("body")
        if (body.isBlank()) {
            EmptyNote("Tap the gear to write something")
        } else {
            Text(
                body, color = SteamColors.Text, style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(14.dp),
            )
        }
    }
}
