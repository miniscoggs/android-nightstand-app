package dev.lewisscoging.nightstand.settings

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.lewisscoging.nightstand.R
import dev.lewisscoging.nightstand.settings.components.SwitchRow

/** Applies a change to the settings, eg `onUpdate { it.copy(showDate = true) }`. */
typealias SettingsUpdate = (transform: (NightstandSettings) -> NightstandSettings) -> Unit

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory)
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val saveFailedMessage = stringResource(R.string.settings_save_failed)
    LaunchedEffect(viewModel, saveFailedMessage) {
        viewModel.saveFailures.collect { snackbarHostState.showSnackbar(saveFailedMessage) }
    }
    SettingsContent(
        settings = settings,
        onUpdate = viewModel::update,
        snackbarHostState = snackbarHostState,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsContent(
    settings: NightstandSettings?,
    onUpdate: SettingsUpdate,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { TopAppBar(title = { Text(stringResource(R.string.app_name)) }) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        // The body stays empty until storage is read, so controls never flip from the defaults.
        if (settings == null) return@Scaffold
        LazyColumn(contentPadding = innerPadding) {
            item { SetupCard() }
            item {
                SwitchRow(
                    title = stringResource(R.string.settings_enabled_title),
                    summary = stringResource(R.string.settings_enabled_summary),
                    checked = settings.enabled,
                    onCheckedChange = { checked -> onUpdate { it.copy(enabled = checked) } }
                )
            }
            behaviourSection(settings = settings, onUpdate = onUpdate)
            appearanceSection(settings = settings, onUpdate = onUpdate)
        }
    }
}
