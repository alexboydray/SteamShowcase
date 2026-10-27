package com.alex.steamshowcase.ui.tiles

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.alex.steamshowcase.data.ProfileData
import com.alex.steamshowcase.data.TileConfig
import com.alex.steamshowcase.ui.EmptyNote
import com.alex.steamshowcase.ui.SteamColors
import com.alex.steamshowcase.ui.TileActions
import com.alex.steamshowcase.ui.TileCard

@Composable
fun ScreenshotsTile(cfg: TileConfig, data: ProfileData, actions: TileActions?) {
    val shots = data.screenshots.take(cfg.settingInt("count", 6))
    TileCard(title = cfg.title ?: "Screenshots", actions = actions) {
        if (shots.isEmpty()) {
            EmptyNote(if (data.loadedAt == 0L) "Loading…" else "No public screenshots found")
            return@TileCard
        }
        ImageGrid(
            urls = shots.map { it.thumbUrl },
            fullUrls = shots.map { it.fullUrl },
            columns = cfg.settingInt("columns", 3).coerceIn(1, 4),
        )
    }
}

@Composable
fun ArtworkTile(cfg: TileConfig, actions: TileActions?) {
    val images = cfg.setting("images").lines().map { it.trim() }.filter { it.isNotBlank() }
    TileCard(title = cfg.title ?: "Artwork", actions = actions) {
        if (images.isEmpty()) {
            EmptyNote("Tap the gear to add images")
            return@TileCard
        }
        ImageGrid(urls = images, fullUrls = images, columns = cfg.settingInt("columns", 2).coerceIn(1, 4))
    }
}

@Composable
private fun ImageGrid(urls: List<String>, fullUrls: List<String>, columns: Int) {
    var open by remember { mutableStateOf<String?>(null) }
    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        urls.chunked(columns).forEachIndexed { rowIdx, row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEachIndexed { i, url ->
                    val idx = rowIdx * columns + i
                    AsyncImage(
                        model = url, contentDescription = null,
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(16f / 9f)
                            .clip(RoundedCornerShape(4.dp))
                            .background(SteamColors.PanelLight)
                            .clickable { open = fullUrls.getOrElse(idx) { url } },
                        contentScale = ContentScale.Crop,
                    )
                }
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
    open?.let { url ->
        Dialog(onDismissRequest = { open = null }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.92f))
                    .clickable { open = null },
                contentAlignment = Alignment.Center,
            ) {
                AsyncImage(
                    model = url, contentDescription = null,
                    modifier = Modifier.fillMaxWidth(),
                    contentScale = ContentScale.Fit,
                )
                Text(
                    "Tap to close",
                    color = SteamColors.TextDim, style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                )
            }
        }
    }
}
