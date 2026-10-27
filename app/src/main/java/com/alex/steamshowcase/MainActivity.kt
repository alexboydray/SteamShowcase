package com.alex.steamshowcase

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.SideEffect
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import coil.Coil
import coil.ImageLoader
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import com.alex.steamshowcase.data.TileConfig
import com.alex.steamshowcase.data.TileSize
import com.alex.steamshowcase.data.TileType
import com.alex.steamshowcase.ui.SettingsScreen
import com.alex.steamshowcase.ui.SteamColors
import com.alex.steamshowcase.ui.SteamShowcaseTheme
import com.alex.steamshowcase.ui.TileActions
import com.alex.steamshowcase.ui.TileConfigDialog
import com.alex.steamshowcase.ui.formatRelative
import com.alex.steamshowcase.ui.tiles.AchievementShowcaseTile
import com.alex.steamshowcase.ui.tiles.ArtworkTile
import com.alex.steamshowcase.ui.tiles.FavouriteGameTile
import com.alex.steamshowcase.ui.tiles.LevelBadgesTile
import com.alex.steamshowcase.ui.tiles.ProfileHeaderTile
import com.alex.steamshowcase.ui.tiles.RecentAchievementsTile
import com.alex.steamshowcase.ui.tiles.RecentGamesTile
import com.alex.steamshowcase.ui.tiles.ScreenshotsTile
import com.alex.steamshowcase.ui.tiles.TextTile

class MainActivity : ComponentActivity() {

    private val vm: ShowcaseViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Animated avatars / backgrounds are GIFs or APNGs.
        Coil.setImageLoader(
            ImageLoader.Builder(this)
                .components {
                    if (Build.VERSION.SDK_INT >= 28) add(ImageDecoderDecoder.Factory()) else add(GifDecoder.Factory())
                }
                .crossfade(true)
                .build()
        )

        setContent {
            // reading prefsVersion makes this recompose when settings are saved
            @Suppress("UNUSED_VARIABLE") val version = vm.prefsVersion
            val prefs = vm.prefs
            val keepOn = prefs.keepScreenOn
            SideEffect {
                if (keepOn) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }

            SteamShowcaseTheme(accent = prefs.accent) {
                if (vm.showSettings) {
                    SettingsScreen(
                        prefs = prefs,
                        onClose = { vm.showSettings = false },
                        onSaved = {
                            vm.prefsChanged()
                            vm.showSettings = false
                            vm.refresh()
                        },
                        onResetLayout = { vm.resetLayout() },
                    )
                } else {
                    ShowcaseScreen(vm, columns = prefs.columns, useProfileBackground = prefs.useProfileBackground)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShowcaseScreen(vm: ShowcaseViewModel, columns: Int, useProfileBackground: Boolean) {
    val data = vm.data
    var configuring by remember { mutableStateOf<TileConfig?>(null) }
    var showAdd by remember { mutableStateOf(false) }
    var showErrors by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(data.summary?.personaName ?: "Steam Showcase")
                        if (data.loadedAt > 0) {
                            Text(
                                (if (vm.editMode) "Editing layout" else "Updated " + formatRelative(data.loadedAt / 1000)),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                actions = {
                    if (vm.loading) {
                        CircularProgressIndicator(Modifier.padding(12.dp).size(22.dp), strokeWidth = 2.dp)
                    } else {
                        IconButton(onClick = { vm.refresh() }) { Icon(Icons.Default.Refresh, "Refresh") }
                    }
                    IconButton(onClick = { vm.editMode = !vm.editMode }) {
                        Icon(
                            if (vm.editMode) Icons.Default.Check else Icons.Default.Edit,
                            if (vm.editMode) "Done" else "Edit layout",
                            tint = if (vm.editMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    IconButton(onClick = { vm.showSettings = true }) { Icon(Icons.Default.Settings, "Settings") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        floatingActionButton = {
            if (vm.editMode) {
                ExtendedFloatingActionButton(
                    onClick = { showAdd = true },
                    icon = { Icon(Icons.Default.Add, null) },
                    text = { Text("Add tile") },
                )
            }
        },
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            if (data.errors.isNotEmpty()) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .background(Color(0x33F07A6E), MaterialTheme.shapes.small)
                        .clickable { showErrors = !showErrors }
                        .padding(10.dp)
                ) {
                    Text(
                        if (showErrors) data.errors.joinToString("\n")
                        else "${data.errors.size} thing(s) failed to load — tap for details",
                        color = Color(0xFFF07A6E), style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            if (vm.tiles.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No tiles. Tap the pencil, then Add tile.", color = SteamColors.TextDim)
                }
            }
            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 96.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(
                    vm.tiles,
                    key = { it.uid },
                    span = { t -> GridItemSpan(if (t.size == TileSize.FULL || columns == 1) maxLineSpan else 1) },
                ) { tile ->
                    val actions = if (vm.editMode) TileActions(
                        onMoveUp = { vm.moveTile(tile.uid, -1) },
                        onMoveDown = { vm.moveTile(tile.uid, +1) },
                        onResize = { vm.updateTile(tile.copy(size = tile.size.toggle())) },
                        onConfigure = { configuring = tile },
                        onRemove = { vm.removeTile(tile.uid) },
                    ) else null
                    RenderTile(tile, vm, actions, useProfileBackground)
                }
            }
        }
    }

    configuring?.let { tile ->
        TileConfigDialog(
            initial = tile,
            ownedGames = data.ownedGames,
            onDismiss = { configuring = null },
            onSave = { vm.updateTile(it); configuring = null },
        )
    }

    if (showAdd) {
        ModalBottomSheet(onDismissRequest = { showAdd = false }) {
            Text(
                "ADD A SHOWCASE",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
            )
            TileType.entries.forEach { type ->
                ListItem(
                    headlineContent = { Text(type.title) },
                    supportingContent = { Text(type.blurb) },
                    modifier = Modifier.clickable {
                        vm.addTile(type)
                        showAdd = false
                    },
                )
            }
            Box(Modifier.padding(24.dp))
        }
    }
}

@Composable
private fun RenderTile(tile: TileConfig, vm: ShowcaseViewModel, actions: TileActions?, useProfileBackground: Boolean) {
    val data = vm.data
    when (tile.type) {
        TileType.PROFILE_HEADER -> ProfileHeaderTile(tile, data, actions, useProfileBackground)
        TileType.LEVEL_BADGES -> LevelBadgesTile(tile, data, actions)
        TileType.RECENT_GAMES -> RecentGamesTile(tile, data, actions)
        TileType.ACHIEVEMENT_SHOWCASE -> AchievementShowcaseTile(tile, data, actions)
        TileType.RECENT_ACHIEVEMENTS -> RecentAchievementsTile(tile, data, actions)
        TileType.SCREENSHOTS -> ScreenshotsTile(tile, data, actions)
        TileType.ARTWORK -> ArtworkTile(tile, actions)
        TileType.FAVOURITE_GAME -> FavouriteGameTile(tile, data, actions)
        TileType.TEXT -> TextTile(tile, actions)
    }
}
