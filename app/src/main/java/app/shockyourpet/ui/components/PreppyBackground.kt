package app.shockyourpet.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush

@Composable
fun PreppyBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = modifier.background(
            Brush.verticalGradient(
                listOf(
                    colors.background,
                    colors.surfaceVariant.copy(alpha = 0.72f),
                    colors.background,
                ),
            ),
        ),
    ) {
        content()
    }
}
