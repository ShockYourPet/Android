package app.shockyourpet.ui.screens

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.shockyourpet.data.api.models.CommandLogEntry
import app.shockyourpet.ui.components.NotificationType
import app.shockyourpet.ui.viewmodel.SettingsViewModel
import app.shockyourpet.ui.viewmodel.ShockerViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

val TerminalBackground = Color(0xFF26151F)
val TerminalHeaderBg = Color(0xFF3A2030)
val TerminalGreen = Color(0xFFAED4B5)
val TerminalCyan = Color(0xFFFF9FC2)
val TerminalRed = Color(0xFFFF7FA6)
val TerminalYellow = Color(0xFFFFD7E5)
val TerminalGray = Color(0xFFD3A8B8)

@Composable
fun TerminalLogsDialog(
    shockerViewModel: ShockerViewModel,
    settingsViewModel: SettingsViewModel,
    onDismiss: () -> Unit,
) {
    val logs by shockerViewModel.commandLogs.collectAsState()
    val context = LocalContext.current
    val listState = rememberLazyListState()

    var activeFilterTab by remember { mutableIntStateOf(0) } // 0: All, 1: Errors, 2: Warnings, 3: Events

    val filteredLogs = remember(logs, activeFilterTab) {
        when (activeFilterTab) {
            1 -> logs.filter { !it.success }
            2 -> logs.filter { it.actionType.contains("Permission", ignoreCase = true) || it.actionType.contains("Paused", ignoreCase = true) }
            3 -> logs.filter { it.actionType.contains("Connect", ignoreCase = true) || it.actionType.contains("Ready", ignoreCase = true) || it.actionType.contains("Disconnect", ignoreCase = true) }
            else -> logs
        }
    }

    // Auto-scroll to bottom on new log entry
    LaunchedEffect(filteredLogs.size) {
        if (filteredLogs.isNotEmpty()) {
            listState.animateScrollToItem(filteredLogs.size - 1)
        }
    }

    fun handleCopyAllLogs() {
        if (filteredLogs.isEmpty()) return
        val formatted = filteredLogs.reversed().joinToString("\n") { entry ->
            val timeStr = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date(entry.timestamp))
            "[$timeStr] [${entry.actionType}] Target: ${entry.shockerName} | Intensity: ${entry.intensity}% | Status: ${entry.statusText} (${if (entry.success) "SUCCESS" else "FAIL"})"
        }
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(android.content.ClipData.newPlainText("Terminal Logs", formatted))
        settingsViewModel.showNotification("Copied ${filteredLogs.size} terminal log entries to clipboard", NotificationType.Success)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = TerminalBackground
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Terminal Header Bar
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(TerminalHeaderBg)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Terminal,
                                contentDescription = null,
                                tint = TerminalGreen,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "PAW_CONSOLE // LOGS",
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = "${filteredLogs.size} EVENTS (${logs.size} TOTAL)",
                                    fontFamily = FontFamily.Monospace,
                                    color = TerminalGray,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { handleCopyAllLogs() }) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy Logs",
                                    tint = TerminalCyan
                                )
                            }

                            if (logs.isNotEmpty()) {
                                IconButton(onClick = { shockerViewModel.clearLogs() }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Clear Logs",
                                        tint = TerminalRed
                                    )
                                }
                            }

                            IconButton(onClick = onDismiss) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Close Terminal",
                                    tint = Color.White
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Terminal Filter Chips
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = activeFilterTab == 0,
                            onClick = { activeFilterTab = 0 },
                            label = { Text("ALL (${logs.size})", fontFamily = FontFamily.Monospace, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = TerminalCyan, selectedLabelColor = Color.Black)
                        )
                        FilterChip(
                            selected = activeFilterTab == 1,
                            onClick = { activeFilterTab = 1 },
                            label = { Text("ERRORS (${logs.count { !it.success }})", fontFamily = FontFamily.Monospace, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = TerminalRed, selectedLabelColor = Color.White)
                        )
                        FilterChip(
                            selected = activeFilterTab == 2,
                            onClick = { activeFilterTab = 2 },
                            label = { Text("WARNINGS", fontFamily = FontFamily.Monospace, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = TerminalYellow, selectedLabelColor = Color.Black)
                        )
                        FilterChip(
                            selected = activeFilterTab == 3,
                            onClick = { activeFilterTab = 3 },
                            label = { Text("EVENTS", fontFamily = FontFamily.Monospace, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = TerminalGreen, selectedLabelColor = Color.Black)
                        )
                    }
                }

                // Terminal Body
                if (filteredLogs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = ">_ CONSOLE READY. NO MATCHING LOGS.",
                                fontFamily = FontFamily.Monospace,
                                color = TerminalGreen,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "System errors, warnings, and WebSocket events will appear here.",
                                fontFamily = FontFamily.Monospace,
                                color = TerminalGray,
                                fontSize = 12.sp
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(filteredLogs.reversed(), key = { it.id }) { entry ->
                            TerminalLogRow(entry)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TerminalLogRow(entry: CommandLogEntry) {
    val timeStr = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date(entry.timestamp))

    val statusColor = if (entry.success) TerminalGreen else TerminalRed
    val tagColor = when (entry.actionType.lowercase()) {
        "liveconnect", "liveready", "livedisconnect" -> TerminalCyan
        "shock", "vibrate", "sound" -> TerminalYellow
        else -> TerminalGray
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF3A2030), shape = RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "[$timeStr]",
                    fontFamily = FontFamily.Monospace,
                    color = TerminalGray,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "[${entry.actionType.uppercase()}]",
                    fontFamily = FontFamily.Monospace,
                    color = tagColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (entry.success) "SUCCESS" else "FAIL",
                    fontFamily = FontFamily.Monospace,
                    color = statusColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "Target: ${entry.shockerName} | Intensity: ${entry.intensity}% | ${entry.statusText}",
                fontFamily = FontFamily.Monospace,
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 12.sp
            )
        }
    }
}
