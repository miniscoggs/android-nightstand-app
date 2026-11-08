package dev.lewisscoging.nightstand.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.lewisscoging.nightstand.R

@Composable
fun SetupCard(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Card(modifier = modifier.fillMaxWidth().padding(16.dp)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.settings_setup_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() }
            )
            Text(
                text = stringResource(
                    R.string.settings_setup_steps,
                    stringResource(R.string.setup_screen_saver_menu_path),
                    stringResource(R.string.setup_while_charging_label)
                ),
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = stringResource(R.string.settings_setup_timing_note),
                style = MaterialTheme.typography.bodySmall
            )
            Button(onClick = { openScreenSaverSettings(context) }) {
                Text(stringResource(R.string.settings_setup_open_button))
            }
        }
    }
}
