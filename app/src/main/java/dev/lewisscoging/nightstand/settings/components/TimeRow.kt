package dev.lewisscoging.nightstand.settings.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.lewisscoging.nightstand.R
import dev.lewisscoging.nightstand.clock.formatClock
import dev.lewisscoging.nightstand.core.MINUTES_PER_HOUR
import dev.lewisscoging.nightstand.core.minutesOfDayToLocalTime

/** A row showing a minutes-of-day value that opens a Material 3 time picker when tapped. */
@Composable
fun TimeRow(
    title: String,
    minutesOfDay: Int,
    is24Hour: Boolean,
    onTimeChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var showPicker by rememberSaveable { mutableStateOf(false) }
    ClickableRow(
        title = title,
        summary = formatClock(minutesOfDayToLocalTime(minutesOfDay), is24Hour),
        onClickLabel = stringResource(R.string.settings_time_change_action, title),
        onClick = { showPicker = true },
        modifier = modifier
    )
    if (showPicker) {
        TimePickerAlertDialog(
            title = title,
            minutesOfDay = minutesOfDay,
            is24Hour = is24Hour,
            onConfirm = { picked ->
                showPicker = false
                onTimeChange(picked)
            },
            onDismiss = { showPicker = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerAlertDialog(
    title: String,
    minutesOfDay: Int,
    is24Hour: Boolean,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val state = rememberTimePickerState(
        initialHour = minutesOfDay / MINUTES_PER_HOUR,
        initialMinute = minutesOfDay % MINUTES_PER_HOUR,
        is24Hour = is24Hour
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { TimePicker(state = state) },
        confirmButton = {
            TextButton(onClick = { onConfirm(state.hour * MINUTES_PER_HOUR + state.minute) }) {
                Text(stringResource(R.string.settings_time_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.settings_time_cancel))
            }
        }
    )
}
