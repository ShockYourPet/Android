package app.shockyourpet.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds

enum class NotificationType {
    Info, Success, Warning, Error
}

data class InAppNotification(
    val id: String = UUID.randomUUID().toString(),
    val message: String,
    val type: NotificationType = NotificationType.Info,
    val durationMs: Long = 3000L,
)

@Composable
fun InAppNotificationBanner(
    notification: InAppNotification?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = notification != null,
        enter = slideInVertically { -it } + fadeIn(),
        exit = slideOutVertically { -it } + fadeOut(),
        modifier = modifier,
    ) {
        if (notification != null) {
            LaunchedEffect(notification.id) {
                delay(notification.durationMs.milliseconds)
                onDismiss()
            }

            val (bgColor, icon, contentColor) = when (notification.type) {
                NotificationType.Success -> Triple(
                    MaterialTheme.colorScheme.tertiaryContainer,
                    Icons.Default.CheckCircle,
                    MaterialTheme.colorScheme.onTertiaryContainer,
                )
                NotificationType.Warning -> Triple(
                    MaterialTheme.colorScheme.secondaryContainer,
                    Icons.Default.Warning,
                    MaterialTheme.colorScheme.onSecondaryContainer,
                )
                NotificationType.Error -> Triple(
                    MaterialTheme.colorScheme.errorContainer,
                    Icons.Default.Error,
                    MaterialTheme.colorScheme.onErrorContainer,
                )
                NotificationType.Info -> Triple(
                    MaterialTheme.colorScheme.primaryContainer,
                    Icons.Default.Info,
                    MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .widthIn(max = 420.dp)
                    .padding(top = 6.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = bgColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                border = BorderStroke(1.dp, contentColor.copy(alpha = 0.2f)),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(9.dp))
                    Text(
                        text = notification.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = contentColor,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(30.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss",
                            tint = contentColor.copy(alpha = 0.72f),
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
        }
    }
}
