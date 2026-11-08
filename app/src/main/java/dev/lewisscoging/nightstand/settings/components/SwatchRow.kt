package dev.lewisscoging.nightstand.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

private val swatchTouchSize = 48.dp
private val swatchSize = 36.dp
private val swatchOutlineWidth = 1.dp
private val selectedRingWidth = 3.dp

/** A titled row of colour swatches; the selected one has a ring and its name is the summary. */
@Composable
fun SwatchRow(
    title: String,
    swatches: List<Swatch>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(text = title, style = MaterialTheme.typography.bodyLarge)
        swatches.getOrNull(selectedIndex)?.let { selected ->
            Text(
                text = selected.label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .selectableGroup()
        ) {
            swatches.forEachIndexed { index, swatch ->
                SwatchButton(
                    swatch = swatch,
                    selected = index == selectedIndex,
                    onClick = { onSelect(index) }
                )
            }
        }
    }
}

@Composable
private fun SwatchButton(
    swatch: Swatch,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ringColor = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(swatchTouchSize)
            .clip(CircleShape)
            .border(selectedRingWidth, ringColor, CircleShape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .semantics { contentDescription = swatch.label }
    ) {
        Box(
            modifier = Modifier
                .size(swatchSize)
                .clip(CircleShape)
                .background(swatch.color)
                .border(swatchOutlineWidth, MaterialTheme.colorScheme.outline, CircleShape)
        )
    }
}
