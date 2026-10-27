package com.alex.steamshowcase.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.alex.steamshowcase.data.Prefs
import com.alex.steamshowcase.data.SteamApi
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(prefs: Prefs, onClose: () -> Unit, onSaved: () -> Unit, onResetLayout: () -> Unit) {
    var apiKey by remember { mutableStateOf(prefs.apiKey) }
    var steamInput by remember { mutableStateOf(prefs.steamIdInput.ifBlank { prefs.steamId }) }
    var columns by remember { mutableStateOf(prefs.columns) }
    var accent by remember { mutableStateOf(prefs.accent) }
    var useBg by remember { mutableStateOf(prefs.useProfileBackground) }
    var keepOn by remember { mutableStateOf(prefs.keepScreenOn) }
    var autoRefresh by remember { mutableStateOf(prefs.autoRefreshMinutes.toString()) }
    var showKey by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    if (prefs.isConfigured) IconButton(onClick = onClose) { Icon(Icons.Default.Close, "Close") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { pad ->
        Column(
            Modifier
                .padding(pad)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Section("Steam account")
            Text(
                "Needs a free Steam Web API key (steamcommunity.com/dev/apikey — any domain name works) " +
                    "and your profile set to Public, including Game details.",
                color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall,
            )
            OutlinedTextField(
                value = apiKey, onValueChange = { apiKey = it.trim() },
                label = { Text("Steam Web API key") }, singleLine = true,
                visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = { TextButton(onClick = { showKey = !showKey }) { Text(if (showKey) "Hide" else "Show") } },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = steamInput, onValueChange = { steamInput = it },
                label = { Text("SteamID64, vanity name, or profile URL") }, singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Section("Look")
            Text("Accent colour", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SteamColors.presets.forEach { (name, value) ->
                    Box(
                        Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(value))
                            .border(
                                width = if (accent == value) 3.dp else 1.dp,
                                color = if (accent == value) Color.White else Color.Black.copy(alpha = 0.4f),
                                shape = CircleShape,
                            )
                            .clickable { accent = value }
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Columns", modifier = Modifier.weight(1f))
                (1..4).forEach { n ->
                    FilterChip(
                        selected = columns == n, onClick = { columns = n },
                        label = { Text(n.toString()) }, modifier = Modifier.padding(start = 6.dp),
                    )
                }
            }
            ToggleLine("Use Steam profile background in header", useBg) { useBg = it }
            ToggleLine("Keep screen on", keepOn) { keepOn = it }
            OutlinedTextField(
                value = autoRefresh, onValueChange = { autoRefresh = it.filter { c -> c.isDigit() } },
                label = { Text("Auto-refresh every N minutes (0 = off)") }, singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Section("Layout")
            TextButton(onClick = onResetLayout) { Text("Reset tiles to default layout") }

            Spacer(Modifier.height(4.dp))
            status?.let {
                Text(it, color = if (it.startsWith("Saved")) SteamColors.Green else Color(0xFFF07A6E))
            }
            Button(
                enabled = !saving && apiKey.isNotBlank() && steamInput.isNotBlank(),
                onClick = {
                    saving = true
                    status = "Checking profile…"
                    scope.launch {
                        try {
                            val api = SteamApi(apiKey)
                            val id = api.resolveSteamId(steamInput)
                            val summary = api.getPlayerSummary(id)
                            prefs.apiKey = apiKey
                            prefs.steamId = id
                            prefs.steamIdInput = steamInput
                            prefs.columns = columns
                            prefs.accent = accent
                            prefs.useProfileBackground = useBg
                            prefs.keepScreenOn = keepOn
                            prefs.autoRefreshMinutes = autoRefresh.toIntOrNull() ?: 10
                            status = "Saved — hello, ${summary.personaName}"
                            onSaved()
                        } catch (e: Exception) {
                            status = "Couldn't verify: ${e.message}"
                        } finally {
                            saving = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(if (saving) "Checking…" else "Save") }
        }
    }
}

@Composable
private fun Section(title: String) {
    Text(
        title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 6.dp),
    )
}

@Composable
private fun ToggleLine(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
