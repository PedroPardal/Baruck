package com.pedro.baruck.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text
import com.pedro.baruck.ui.theme.BaruckColors

/** Linha de chips de seleção única (id + rótulo). */
@Composable
fun LazyRowChips(
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(vertical = 6.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
    ) {
        items(options, key = { it.first }) { (id, label) ->
            FilterChip(
                selected = id == selected,
                onClick = { onSelect(id) },
                label = { Text(label) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = BaruckColors.GreenSoft,
                    selectedLabelColor = BaruckColors.GreenDark
                )
            )
        }
    }
}

fun Modifier.clickableRow(onClick: () -> Unit): Modifier = this.clickable { onClick() }
