package com.rasoiai.app.presentation.favorites.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.rasoiai.app.presentation.favorites.TimeFilter
import com.rasoiai.app.presentation.theme.spacing
import com.rasoiai.domain.model.CuisineType

@Composable
fun FavoritesFilterChips(
    selectedCuisine: CuisineType?,
    selectedTimeFilter: TimeFilter?,
    onCuisineSelected: (CuisineType?) -> Unit,
    onTimeFilterSelected: (TimeFilter?) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        // All filter (clear)
        FilterChip(
            selected = selectedCuisine == null && selectedTimeFilter == null,
            onClick = {
                onCuisineSelected(null)
                onTimeFilterSelected(null)
            },
            label = { Text("All") },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        )

        Spacer(modifier = Modifier.width(spacing.sm))

        // Cuisine filter dropdown
        CuisineFilterChip(
            selectedCuisine = selectedCuisine,
            onCuisineSelected = onCuisineSelected
        )

        Spacer(modifier = Modifier.width(spacing.sm))

        // Time filter dropdown
        TimeFilterChip(
            selectedTimeFilter = selectedTimeFilter,
            onTimeFilterSelected = onTimeFilterSelected
        )
    }
}

@Composable
private fun CuisineFilterChip(
    selectedCuisine: CuisineType?,
    onCuisineSelected: (CuisineType?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        FilterChip(
            selected = selectedCuisine != null,
            onClick = { expanded = true },
            label = { Text(selectedCuisine?.displayName ?: "Cuisine") },
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Show cuisine options"
                )
            },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("All Cuisines") },
                onClick = {
                    onCuisineSelected(null)
                    expanded = false
                },
                leadingIcon = if (selectedCuisine == null) {
                    { Icon(Icons.Default.Check, "Selected") }
                } else null
            )

            CuisineType.entries.forEach { cuisine ->
                DropdownMenuItem(
                    text = { Text(cuisine.displayName) },
                    onClick = {
                        onCuisineSelected(cuisine)
                        expanded = false
                    },
                    leadingIcon = if (selectedCuisine == cuisine) {
                        { Icon(Icons.Default.Check, "Selected") }
                    } else null
                )
            }
        }
    }
}

@Composable
private fun TimeFilterChip(
    selectedTimeFilter: TimeFilter?,
    onTimeFilterSelected: (TimeFilter?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        FilterChip(
            selected = selectedTimeFilter != null,
            onClick = { expanded = true },
            label = { Text(selectedTimeFilter?.label ?: "Time") },
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Show time options"
                )
            },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("Any Time") },
                onClick = {
                    onTimeFilterSelected(null)
                    expanded = false
                },
                leadingIcon = if (selectedTimeFilter == null) {
                    { Icon(Icons.Default.Check, "Selected") }
                } else null
            )

            TimeFilter.entries.forEach { timeFilter ->
                DropdownMenuItem(
                    text = { Text(timeFilter.label) },
                    onClick = {
                        onTimeFilterSelected(timeFilter)
                        expanded = false
                    },
                    leadingIcon = if (selectedTimeFilter == timeFilter) {
                        { Icon(Icons.Default.Check, "Selected") }
                    } else null
                )
            }
        }
    }
}
