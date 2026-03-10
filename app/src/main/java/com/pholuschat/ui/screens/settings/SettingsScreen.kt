package com.pholuschat.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit
) {
    var selectedTheme by remember { mutableStateOf("System") }
    var darkModeExpanded by remember { mutableStateOf(false) }
    var dynamicColorsEnabled by remember { mutableStateOf(true) }
    var streamingEnabled by remember { mutableStateOf(true) }
    var codeHighlightingEnabled by remember { mutableStateOf(true) }
    var showModelName by remember { mutableStateOf(true) }
    var maxTokens by remember { mutableStateOf("4096") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            // Appearance Section
            item {
                Text(
                    text = "Appearance",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            item {
                ListItem(
                    headlineContent = { Text("Theme") },
                    supportingContent = { Text(selectedTheme) },
                    leadingContent = { Icon(Icons.Filled.Palette, null) },
                    modifier = Modifier.clickable { darkModeExpanded = true }
                )
                DropdownMenu(
                    expanded = darkModeExpanded,
                    onDismissRequest = { darkModeExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("System") },
                        onClick = {
                            selectedTheme = "System"
                            darkModeExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Light") },
                        onClick = {
                            selectedTheme = "Light"
                            darkModeExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Dark") },
                        onClick = {
                            selectedTheme = "Dark"
                            darkModeExpanded = false
                        }
                    )
                }
            }

            item {
                ListItem(
                    headlineContent = { Text("Dynamic Colors") },
                    supportingContent = { Text("Use Material You colors from wallpaper") },
                    leadingContent = { Icon(Icons.Filled.Palette, null) },
                    trailingContent = {
                        Switch(
                            checked = dynamicColorsEnabled,
                            onCheckedChange = { dynamicColorsEnabled = it }
                        )
                    }
                )
            }

            // Chat Section
            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text(
                    text = "Chat",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            item {
                ListItem(
                    headlineContent = { Text("Streaming Responses") },
                    supportingContent = { Text("Show responses as they're generated") },
                    leadingContent = { Icon(Icons.Filled.Speed, null) },
                    trailingContent = {
                        Switch(
                            checked = streamingEnabled,
                            onCheckedChange = { streamingEnabled = it }
                        )
                    }
                )
            }

            item {
                ListItem(
                    headlineContent = { Text("Code Highlighting") },
                    supportingContent = { Text("Syntax highlighting for code blocks") },
                    leadingContent = { Icon(Icons.Filled.Code, null) },
                    trailingContent = {
                        Switch(
                            checked = codeHighlightingEnabled,
                            onCheckedChange = { codeHighlightingEnabled = it }
                        )
                    }
                )
            }

            item {
                ListItem(
                    headlineContent = { Text("Show Model Name") },
                    supportingContent = { Text("Display model name below messages") },
                    leadingContent = { Icon(Icons.Filled.Settings, null) },
                    trailingContent = {
                        Switch(
                            checked = showModelName,
                            onCheckedChange = { showModelName = it }
                        )
                    }
                )
            }

            // Limits Section
            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text(
                    text = "Limits",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            item {
                ListItem(
                    headlineContent = { Text("Max Tokens") },
                    supportingContent = { Text("Maximum tokens per response") },
                    leadingContent = { Icon(Icons.Filled.TextFields, null) },
                    modifier = Modifier.clickable { /* Show dialog */ }
                )
            }

            // About Section
            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text(
                    text = "About",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            item {
                ListItem(
                    headlineContent = { Text("PholusChat") },
                    supportingContent = { Text("Version 1.0.0") },
                    leadingContent = { Icon(Icons.Filled.Info, null) }
                )
            }

            item {
                ListItem(
                    headlineContent = { Text("No Trackers") },
                    supportingContent = { Text("This app does not collect any data") },
                    leadingContent = { Icon(Icons.Filled.Security, null) }
                )
            }
        }
    }
}
