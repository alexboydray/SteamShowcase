package com.alex.steamshowcase.ui

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.alex.steamshowcase.data.Game
import com.alex.steamshowcase.data.TileConfig
import com.alex.steamshowcase.data.TileSize
import com.alex.steamshowcase.data.TileType

@Composable
fun TileConfigDialog(
    initial: TileConfig,
    ownedGames: List<Game>,
    onDismiss: () -> Unit,
    onSave: (TileConfig) -> Unit,
) {
    var cfg by remember { mutableStateOf(initial) }
    var pickingGame by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(cfg.type.title) },
        text = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    value = cfg.title ?: "",
                    onValueChange = { cfg = cfg.copy(title = it.ifBlank { null }) },
                    label = { Text("Custom heading (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Width", modifier = Modifier.weight(1f))
                    TileSize.entries.forEach { s ->
                        FilterChip(
                            selected = cfg.size == s,
                            onClick = { cfg = cfg.copy(size = s) },
                            label = { Text(s.label) },
                            modifier = Modifier.padding(start = 6.dp),
                        )
                    }
                }

                when (cfg.type) {
                    TileType.PROFILE_HEADER -> {
                        ToggleRow("Show real name", cfg.settingBool("showRealName", true)) { cfg = cfg.with("showRealName", it.toString()) }
                        ToggleRow("Show country", cfg.settingBool("showCountry", true)) { cfg = cfg.with("showCountry", it.toString()) }
                        ToggleRow("Show member since", cfg.settingBool("showMemberSince", true)) { cfg = cfg.with("showMemberSince", it.toString()) }
                    }
                    TileType.LEVEL_BADGES -> NumberField("Badges to show", cfg, "badgeCount") { cfg = it }
                    TileType.RECENT_GAMES -> {
                        NumberField("Games to show", cfg, "count") { cfg = it }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Style", modifier = Modifier.weight(1f))
                            listOf("list" to "List", "capsules" to "Capsules").forEach { (id, label) ->
                                FilterChip(
                                    selected = cfg.setting("style") == id,
                                    onClick = { cfg = cfg.with("style", id) },
                                    label = { Text(label) },
                                    modifier = Modifier.padding(start = 6.dp),
                                )
                            }
                        }
                    }
                    TileType.ACHIEVEMENT_SHOWCASE -> {
                        GamePickerRow(cfg, ownedGames) { pickingGame = true }
                        NumberField("Achievements to show", cfg, "count") { cfg = it }
                        ToggleRow("Only unlocked", cfg.settingBool("onlyUnlocked", true)) { cfg = cfg.with("onlyUnlocked", it.toString()) }
                    }
                    TileType.RECENT_ACHIEVEMENTS -> NumberField("Achievements to show", cfg, "count") { cfg = it }
                    TileType.SCREENSHOTS -> {
                        NumberField("Screenshots to show", cfg, "count") { cfg = it }
                        NumberField("Columns (1-4)", cfg, "columns") { cfg = it }
                    }
                    TileType.ARTWORK -> {
                        ArtworkPicker(cfg) { cfg = it }
                        NumberField("Columns (1-4)", cfg, "columns") { cfg = it }
                    }
                    TileType.FAVOURITE_GAME -> GamePickerRow(cfg, ownedGames) { pickingGame = true }
                    TileType.TEXT -> {
                        OutlinedTextField(
                            value = cfg.setting("heading"),
                            onValueChange = { cfg = cfg.with("heading", it) },
                            label = { Text("Heading") }, singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = cfg.setting("body"),
                            onValueChange = { cfg = cfg.with("body", it) },
                            label = { Text("Text") }, minLines = 3,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        },
        confirmButton = { Button(onClick = { onSave(cfg) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )

    if (pickingGame) {
        GamePickerDialog(ownedGames, onDismiss = { pickingGame = false }) { g ->
            cfg = cfg.with("appId", g.appId.toString())
            pickingGame = false
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun NumberField(label: String, cfg: TileConfig, key: String, onChange: (TileConfig) -> Unit) {
    OutlinedTextField(
        value = cfg.setting(key),
        onValueChange = { v -> onChange(cfg.with(key, v.filter { it.isDigit() })) },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun GamePickerRow(cfg: TileConfig, owned: List<Game>, onPick: () -> Unit) {
    val appId = cfg.setting("appId").toIntOrNull()
    val name = owned.firstOrNull { it.appId == appId }?.name ?: appId?.let { "App $it" } ?: "None"
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("Game", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(name)
        }
        OutlinedButton(onClick = onPick) { Text("Choose") }
    }
}

@Composable
private fun GamePickerDialog(owned: List<Game>, onDismiss: () -> Unit, onPick: (Game) -> Unit) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(query, owned) {
        if (query.isBlank()) owned else owned.filter { it.name.contains(query, ignoreCase = true) }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pick a game") },
        text = {
            Column(Modifier.height(420.dp)) {
                OutlinedTextField(
                    value = query, onValueChange = { query = it },
                    label = { Text("Search your library") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                if (owned.isEmpty()) {
                    Text("Library not loaded yet — refresh first.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                LazyColumn {
                    items(filtered, key = { it.appId }) { g ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { onPick(g) }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(g.name, modifier = Modifier.weight(1f))
                            Text(formatPlaytime(g.playtimeForeverMin), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun ArtworkPicker(cfg: TileConfig, onChange: (TileConfig) -> Unit) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        val kept = uris.mapNotNull { uri ->
            try {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                uri.toString()
            } catch (e: Exception) {
                null
            }
        }
        val existing = cfg.setting("images")
        val merged = (existing.lines() + kept).map { it.trim() }.filter { it.isNotBlank() }.distinct()
        onChange(cfg.with("images", merged.joinToString("\n")))
    }
    OutlinedButton(onClick = { launcher.launch(arrayOf("image/*")) }, modifier = Modifier.fillMaxWidth()) {
        Text("Pick images from device")
    }
    OutlinedTextField(
        value = cfg.setting("images"),
        onValueChange = { onChange(cfg.with("images", it)) },
        label = { Text("Image URLs, one per line") },
        minLines = 3,
        modifier = Modifier.fillMaxWidth(),
    )
}
