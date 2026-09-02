package app.shockyourpet.ui.screens

import androidx.compose.runtime.Composable
import app.shockyourpet.ui.viewmodel.ShockerViewModel

@Composable
@Suppress("unused")
fun ShockerControlScreen(viewModel: ShockerViewModel) {
    LivePanelScreen(viewModel = viewModel)
}
