package com.alex.steamshowcase.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Edit-mode actions passed down to every tile. Null when not editing. */
class TileActions(
    val onMoveUp: () -> Unit,
    val onMoveDown: () -> Unit,
    val onResize: () -> Unit,
    val onConfigure: () -> Unit,
    val onRemove: () -> Unit,
)

@Composable
fun TileCard(
    title: String?,
    actions: TileActions?,
    modifier: Modifier = Modifier,
    headerExtra: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(10.dp)
    Column(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .then(
                if (actions != null) Modifier.border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f), shape)
                else Modifier
            )
    ) {
        if (title != null || actions != null) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 4.dp, top = 8.dp, bottom = 0.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (title != null) {
                    Text(
                        title.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    Box(Modifier.weight(1f))
                }
                headerExtra?.invoke()
                if (actions != null) {
                    SmallIcon(Icons.Default.ArrowUpward, "Move up", actions.onMoveUp)
                    SmallIcon(Icons.Default.ArrowDownward, "Move down", actions.onMoveDown)
                    SmallIcon(Icons.Default.SwapHoriz, "Resize", actions.onResize)
                    SmallIcon(Icons.Default.Settings, "Configure", actions.onConfigure)
                    SmallIcon(Icons.Default.Delete, "Remove", actions.onRemove, tint = Color(0xFFF07A6E))
                }
            }
        }
        content()
    }
}

@Composable
private fun SmallIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    desc: String,
    onClick: () -> Unit,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    IconButton(onClick = onClick, modifier = Modifier.size(34.dp)) {
        Icon(icon, desc, tint = tint, modifier = Modifier.size(18.dp))
    }
}

@Composable
fun EmptyNote(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(80.dp)
            .padding(14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun StatRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        Text(value, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodySmall)
    }
}

fun formatPlaytime(minutes: Int): String {
    if (minutes <= 0) return "0 min"
    val h = minutes / 60.0
    return if (h < 1) "$minutes min" else String.format(Locale.US, "%.1f hrs", h)
}

fun formatDate(unixSeconds: Long, pattern: String = "MMM d, yyyy"): String {
    if (unixSeconds <= 0) return "—"
    return SimpleDateFormat(pattern, Locale.getDefault()).format(Date(unixSeconds * 1000))
}

fun formatRelative(unixSeconds: Long): String {
    if (unixSeconds <= 0) return ""
    val diff = (System.currentTimeMillis() / 1000) - unixSeconds
    return when {
        diff < 60 -> "just now"
        diff < 3600 -> "${diff / 60} min ago"
        diff < 86400 -> "${diff / 3600} hr ago"
        diff < 86400 * 14 -> "${diff / 86400} d ago"
        else -> formatDate(unixSeconds)
    }
}
