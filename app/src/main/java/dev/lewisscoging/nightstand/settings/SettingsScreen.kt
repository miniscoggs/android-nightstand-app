package dev.lewisscoging.nightstand.settings

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.lewisscoging.nightstand.R
import dev.lewisscoging.nightstand.settings.components.SwitchRow

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory)
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val actions: SettingsActions = viewModel
    SettingsContent(
        settings = settings,
        actions = actions,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsContent(
    settings: NightstandSettings,
    actions: SettingsActions,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { TopAppBar(title = { Text(stringResource(R.string.app_name)) }) }
    ) { innerPadding ->
        LazyColumn(contentPadding = innerPadding) {
            item {
                SwitchRow(
                    title = stringResource(R.string.settings_enabled_title),
                    summary = stringResource(R.string.settings_enabled_summary),
                    checked = settings.enabled,
                    onCheckedChange = actions::setEnabled
                )
            }
            behaviourSection(settings = settings, actions = actions)
            appearanceSection(settings = settings, actions = actions)
        }
    }
}
