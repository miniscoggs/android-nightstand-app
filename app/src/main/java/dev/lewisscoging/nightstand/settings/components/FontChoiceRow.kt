package dev.lewisscoging.nightstand.settings.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import dev.lewisscoging.nightstand.R

private val fontCardShape = RoundedCornerShape(12.dp)
private val fontCardBorderWidth = 1.dp
private val selectedFontCardBorderWidth = 3.dp

/** A titled row of font cards, each showing a sample time in its font above the font's name. */
@Composable
fun FontChoiceRow(
    title: String,
    choices: List<FontChoice>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val sample = stringResource(R.string.settings_clock_font_sample)
    Column(modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .selectableGroup()
        ) {
            choices.forEachIndexed { index, choice ->
                FontCard(
                    sample = sample,
                    choice = choice,
                    selected = index == selectedIndex,
                    onClick = { onSelect(index) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun FontCard(
    sample: String,
    choice: FontChoice,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }
    val borderWidth = if (selected) selectedFontCardBorderWidth else fontCardBorderWidth
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(fontCardShape)
            .border(borderWidth, borderColor, fontCardShape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(vertical = 12.dp)
    ) {
        // The sample is decorative; TalkBack announces the font name below instead.
        Text(
            text = sample,
            fontFamily = choice.fontFamily,
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.clearAndSetSemantics { }
        )
        Text(text = choice.label, style = MaterialTheme.typography.labelMedium)
    }
}
