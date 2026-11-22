package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddToHomeScreen
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.FilledTonalButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.WidgetCategory
import com.example.data.model.WidgetConfig
import com.example.data.model.WidgetType
import com.example.ui.components.WidgetPreviewCard
import com.example.util.SystemTelemetry

@Composable
fun ExploreScreen(
    searchQuery: String,
    selectedCategory: WidgetCategory?,
    telemetry: SystemTelemetry,
    nowMillis: Long,
    isDarkTheme: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onSelectCategory: (WidgetCategory?) -> Unit,
    onCustomizeType: (WidgetType) -> Unit,
    onQuickPinType: (WidgetType) -> Unit,
    modifier: Modifier = Modifier
) {
    val filteredTypes = remember(searchQuery, selectedCategory) {
        WidgetType.entries.filter { type ->
            val catOk = selectedCategory == null || type.category == selectedCategory
            val qOk = searchQuery.isBlank() ||
                type.title.contains(searchQuery, ignoreCase = true) ||
                type.subtitle.contains(searchQuery, ignoreCase = true) ||
                type.category.displayName.contains(searchQuery, ignoreCase = true)
            catOk && qOk
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
            Text("Explore Widgets", style = MaterialTheme.typography.headlineMedium)
            Text(
                "All ${WidgetType.entries.size} widgets unlocked & completely free",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("Filter library...") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = "Search library") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("explore_search_input")
            )
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = selectedCategory == null,
                    onClick = { onSelectCategory(null) },
                    label = { Text("All (${WidgetType.entries.size})") }
                )
            }
            items(WidgetCategory.entries) { cat ->
                FilterChip(
                    selected = selectedCategory == cat,
                    onClick = { onSelectCategory(if (selectedCategory == cat) null else cat) },
                    label = { Text(cat.displayName) }
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(filteredTypes, key = { it.id }) { type ->
                val defaultCfg = remember(type, isDarkTheme) {
                    WidgetConfig.createDefault(type, isDarkTheme)
                }
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onCustomizeType(type) }
                        .testTag("explore_card_${type.id}")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = type.category.displayName.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(type.title, style = MaterialTheme.typography.titleLarge)
                                Text(
                                    type.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = type.defaultSize.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(Modifier.height(14.dp))
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            WidgetPreviewCard(
                                config = defaultCfg,
                                telemetry = telemetry,
                                nowMillis = nowMillis,
                                compactMode = true
                            )
                        }

                        if (type.platformNote != null) {
                            Spacer(Modifier.height(10.dp))
                            Row(
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 2.dp).height(14.dp)
                                )
                                Text(
                                    text = type.platformNote,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { onCustomizeType(type) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("explore_customize_${type.id}")
                            ) {
                                Icon(Icons.Rounded.Edit, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("Customize")
                            }
                            FilledTonalButton(
                                onClick = { onQuickPinType(type) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("explore_pin_${type.id}")
                            ) {
                                Icon(Icons.Rounded.AddToHomeScreen, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("Pin")
                            }
                        }
                    }
                }
            }
        }
    }
}
