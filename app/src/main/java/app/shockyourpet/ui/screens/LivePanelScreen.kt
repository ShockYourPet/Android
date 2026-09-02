package app.shockyourpet.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.shockyourpet.ui.viewmodel.ShockerViewModel
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun LivePanelScreen(
    viewModel: ShockerViewModel,
    onNavigateBack: () -> Unit = {},
) {
    val selectedShocker by viewModel.selectedShocker.collectAsState()
    val intensity by viewModel.intensity.collectAsState()

    var selectedMode by remember { mutableStateOf("Shock") } // "Shock", "Vibrate", "Sound"
    var isTouching by remember { mutableStateOf(value = false) }
    var touchYRatio by remember { mutableFloatStateOf(1f) } // 1f = 0% intensity at bottom

    val ekgHistory = remember { mutableStateListOf<Float>() }

    // When finger is released, automatically fall back to 0% intensity & cancel live jobs
    LaunchedEffect(isTouching) {
        if (!isTouching) {
            touchYRatio = 1f
            viewModel.stopLiveControl()
        }
    }

    // Smooth the rendered trace without delaying the actual control command.
    LaunchedEffect(Unit) {
        while (true) {
            val currentTargetY = if (isTouching) touchYRatio else 1f
            val previousY = ekgHistory.lastOrNull() ?: currentTargetY
            val smoothedY = previousY + ((currentTargetY - previousY) * 0.28f)
            ekgHistory.add(smoothedY)
            if (ekgHistory.size > 80) {
                ekgHistory.removeAt(0)
            }
            delay(16.milliseconds)
        }
    }

    if (selectedShocker == null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Pick a device first  ♡",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Select a device from the Shockers tab to open Live Monitor.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onNavigateBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Back to Shockers")
            }
        }
        return
    }

    val shocker = selectedShocker!!
    val chartOnPrimary = MaterialTheme.colorScheme.onPrimary

    val isShockAllowed = shocker.isModeAllowed("Shock")
    val isVibrateAllowed = shocker.isModeAllowed("Vibrate")
    val isSoundAllowed = shocker.isModeAllowed("Sound")
    val isCurrentModeAllowed = shocker.isModeAllowed(selectedMode)

    // Automatically fall back to an allowed mode if current mode is disabled
    LaunchedEffect(shocker, isShockAllowed, isVibrateAllowed, isSoundAllowed, selectedMode) {
        when (selectedMode) {
            "Shock" -> if (!isShockAllowed) {
                val fallback = if (isVibrateAllowed) "Vibrate" else if (isSoundAllowed) "Sound" else "Shock"
                selectedMode = fallback
                viewModel.setActiveMode(fallback)
            }
            "Vibrate" -> if (!isVibrateAllowed) {
                val fallback = if (isShockAllowed) "Shock" else if (isSoundAllowed) "Sound" else "Vibrate"
                selectedMode = fallback
                viewModel.setActiveMode(fallback)
            }
            "Sound" -> if (!isSoundAllowed) {
                val fallback = if (isShockAllowed) "Shock" else if (isVibrateAllowed) "Vibrate" else "Sound"
                selectedMode = fallback
                viewModel.setActiveMode(fallback)
            }
        }
    }

    DisposableEffect(shocker.id) {
        viewModel.onLivePanelActive()
        onDispose {
            viewModel.onLivePanelInactive()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        // Compact, non-obtrusive Top Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = shocker.name.ifBlank { "Shocker ${shocker.id.take(8)}" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = if (shocker.isShared) "Shared Live Device" else "Connected Device",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Mode Selector Row
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = selectedMode == "Shock",
                    onClick = {
                        if (isShockAllowed) {
                            selectedMode = "Shock"
                            viewModel.setActiveMode("Shock")
                        }
                    },
                    enabled = isShockAllowed,
                    leadingIcon = { Icon(Icons.Default.FlashOn, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    label = { Text("Shock") },
                    modifier = Modifier.weight(1f),
                )

                FilterChip(
                    selected = selectedMode == "Vibrate",
                    onClick = {
                        if (isVibrateAllowed) {
                            selectedMode = "Vibrate"
                            viewModel.setActiveMode("Vibrate")
                        }
                    },
                    enabled = isVibrateAllowed,
                    leadingIcon = { Icon(Icons.Default.Vibration, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    label = { Text("Vibrate") },
                    modifier = Modifier.weight(1f),
                )

                FilterChip(
                    selected = selectedMode == "Sound",
                    onClick = {
                        if (isSoundAllowed) {
                            selectedMode = "Sound"
                            viewModel.setActiveMode("Sound")
                        }
                    },
                    enabled = isSoundAllowed,
                    leadingIcon = { Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    label = { Text("Sound") },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // EKG Touch Canvas Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            shape = RoundedCornerShape(24.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
            ) {
                val chartPrimary = MaterialTheme.colorScheme.primary
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(selectedMode, isCurrentModeAllowed) {
                            if (isCurrentModeAllowed) {
                                awaitEachGesture {
                                    val down = awaitFirstDown(requireUnconsumed = false)
                                    val verticalInset = 20.dp.toPx().coerceAtMost(size.height / 2f)
                                    val usableHeight = (size.height - (verticalInset * 2f)).coerceAtLeast(1f)

                                    fun updateFromPointer(position: Offset) {
                                        touchYRatio = ((position.y - verticalInset) / usableHeight).coerceIn(0f, 1f)
                                        val nextIntensity = ((1f - touchYRatio) * 100f)
                                            .toInt()
                                            .coerceIn(0, 100)
                                        viewModel.triggerThrottledLiveControl(selectedMode, nextIntensity)
                                    }

                                    isTouching = true
                                    updateFromPointer(down.position)

                                    // Read every pointer move directly. Compose's drag helper waits for
                                    // touch slop, which made the first few percent above zero unreachable.
                                    var pointerPressed: Boolean
                                    do {
                                        val event = awaitPointerEvent()
                                        val change = event.changes.firstOrNull { it.id == down.id }
                                        pointerPressed = change?.pressed == true
                                        if (pointerPressed && (change != null)) {
                                            updateFromPointer(change.position)
                                            change.consume()
                                        }
                                    } while (pointerPressed)

                                    isTouching = false
                                    touchYRatio = 1f
                                    viewModel.stopLiveControl()
                                }
                            }
                        },
                ) {
                    val width = size.width
                    val height = size.height
                    val verticalInset = 20.dp.toPx().coerceAtMost(height / 2f)
                    val usableHeight = (height - (verticalInset * 2f)).coerceAtLeast(1f)
                    fun yForRatio(ratio: Float) = verticalInset + (ratio.coerceIn(0f, 1f) * usableHeight)

                    val markerX = width / 2f
                    val activeY = yForRatio(ekgHistory.lastOrNull() ?: touchYRatio)

                    // Draw waveform terminating directly AT the target circle at (markerX, activeY)
                    if ((ekgHistory.size > 1) && (markerX > 0f)) {
                        val path = Path()
                        val stepX = markerX / (ekgHistory.size - 1)

                        path.moveTo(0f, yForRatio(ekgHistory.first()))
                        for (index in 1 until (ekgHistory.size - 1)) {
                            val previousX = (index - 1) * stepX
                            val previousY = yForRatio(ekgHistory[index - 1])
                            val currentX = index * stepX
                            val currentY = yForRatio(ekgHistory[index])
                            val middleX = (previousX + currentX) / 2f
                            val middleY = (previousY + currentY) / 2f
                            path.quadraticTo(previousX, previousY, middleX, middleY)
                        }
                        path.lineTo(markerX, activeY)

                        drawPath(
                            path = path,
                            color = chartPrimary.copy(alpha = 0.16f),
                            style = Stroke(width = 10f, cap = StrokeCap.Round),
                        )
                        drawPath(
                            path = path,
                            color = chartPrimary,
                            style = Stroke(width = 3.5f, cap = StrokeCap.Round),
                        )
                    }

                    // Compact Target Marker Circles centered at (markerX, activeY)
                    drawCircle(
                        color = chartPrimary.copy(alpha = 0.22f),
                        radius = 20.dp.toPx(),
                        center = Offset(markerX, activeY),
                    )
                    drawCircle(
                        color = chartPrimary,
                        radius = 12.dp.toPx(),
                        center = Offset(markerX, activeY),
                    )
                    drawCircle(
                        color = chartOnPrimary,
                        radius = 4.dp.toPx(),
                        center = Offset(markerX, activeY),
                    )
                }

                // Header Overlay Display
                Column(
                    modifier = Modifier.align(Alignment.TopCenter),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = if (isCurrentModeAllowed) "$intensity%" else "DISABLED",
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (!isCurrentModeAllowed) MaterialTheme.colorScheme.error else if (intensity > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
                    )
                    Text(
                        text = if (!isCurrentModeAllowed) {
                            "$selectedMode MODE IS DISABLED FOR THIS DEVICE"
                        } else if (isTouching) {
                            "$selectedMode IS LIVE  ♡"
                        } else {
                            "TOUCH + DRAG • RELEASE RETURNS TO 0%"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (!isCurrentModeAllowed) MaterialTheme.colorScheme.error else if (isTouching) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
