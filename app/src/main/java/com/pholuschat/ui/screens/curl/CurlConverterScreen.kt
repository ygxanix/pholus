package com.pholuschat.ui.screens.curl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.pholuschat.data.util.CurlParser
import com.pholuschat.domain.model.ApiType
import com.pholuschat.domain.model.CurlConfig

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurlConverterScreen(
    onNavigateBack: () -> Unit,
    onImportConfig: (CurlConfig) -> Unit = {},
    viewModel: CurlConverterViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    val curlInput = uiState.curlInput
    val parsedConfig = uiState.parsedConfig
    val showPythonCode = uiState.showPythonCode
    val showAdvancedParams = uiState.showAdvancedParams
    val errorMessage = uiState.errorMessage
    val isValidating = uiState.isValidating
    val isSaved = uiState.isSaved

    // Trigger onImportConfig callback and navigate back when saved successfully
    LaunchedEffect(isSaved) {
        if (isSaved) {
            parsedConfig?.let { onImportConfig(it) }
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Import from cURL") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back")
                    }
                },
                actions = {
                    if (parsedConfig != null) {
                        IconButton(onClick = { viewModel.saveConfig() }) {
                            Icon(Icons.Filled.Save, "Save")
                        }
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Paste a cURL command from any AI provider. The app will automatically extract all parameters and create an API configuration.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                OutlinedTextField(
                    value = curlInput,
                    onValueChange = { viewModel.onInputChanged(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    label = { Text("cURL Command") },
                    placeholder = { Text("curl -X POST https://api.openai.com/v1/chat/completions -H 'Authorization: Bearer KEY' -d '{\"model\": \"gpt-4\"}'") },
                    supportingText = {
                        Text("Paste the full curl command including headers and body")
                    }
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.parseCommand() },
                        modifier = Modifier.weight(1f),
                        enabled = curlInput.isNotBlank() && !isValidating
                    ) {
                        if (isValidating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Filled.PlayArrow, null)
                        }
                        Spacer(Modifier.width(8.dp))
                        Text("Parse cURL")
                    }

                    if (parsedConfig != null) {
                        OutlinedButton(
                            onClick = { viewModel.togglePythonCode(!showPythonCode) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.Code, null)
                            Spacer(Modifier.width(8.dp))
                            Text(if (showPythonCode) "Hide Code" else "Python")
                        }
                    }
                }
            }

            errorMessage?.let { error ->
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.Error,
                                null,
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = error,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }

            parsedConfig?.let { config ->
                item {
                    HorizontalDivider()
                }

                item {
                    Text(
                        text = "Detected API",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                when (config.detectedApiType) {
                                    ApiType.OPENAI -> Icons.Filled.Cloud
                                    ApiType.OPENROUTER -> Icons.Filled.Hub
                                    ApiType.OLLAMA -> Icons.Filled.Computer
                                    ApiType.LM_STUDIO -> Icons.Filled.Code
                                    else -> Icons.Filled.CloudQueue
                                },
                                null,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = config.detectedApiType.name.replace("_", " "),
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = config.baseUrl,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                item {
                    Text(
                        text = "Extracted Parameters",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            config.modelName?.let {
                                ParamRow("Model", it, Icons.Filled.CloudQueue)
                            }
                            ParamRow("Method", config.method, Icons.Filled.SwapVert)
                            ParamRow("Base URL", config.baseUrl, Icons.Filled.Link)

                            config.temperature?.let {
                                ParamRow("Temperature", it.toString(), Icons.Filled.Tune)
                            }
                            config.maxTokens?.let {
                                ParamRow("Max Tokens", it.toString(), Icons.Filled.TextFields)
                            }
                            config.stream?.let {
                                ParamRow("Streaming", if (it) "Enabled" else "Disabled", Icons.Filled.Speed)
                            }
                            config.topP?.let {
                                ParamRow("Top P", it.toString(), Icons.Filled.TrendingUp)
                            }
                        }
                    }
                }

                if (config.headers.isNotEmpty()) {
                    item {
                        Text(
                            text = "Headers",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                config.headers.forEach { (key, value) ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = key,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.width(120.dp)
                                        )
                                        Text(
                                            text = if (key.lowercase().contains("key") || key.lowercase().contains("secret"))
                                                "••••••••" else value,
                                            style = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    TextButton(
                        onClick = { viewModel.toggleAdvancedParams(!showAdvancedParams) }
                    ) {
                        Icon(
                            if (showAdvancedParams) Icons.Filled.ExpandLess
                            else Icons.Filled.ExpandMore,
                            null
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(if (showAdvancedParams) "Less Details" else "More Details")
                    }
                }

                if (showAdvancedParams) {
                    config.systemPrompt?.let {
                        item {
                            Text(
                                text = "System Prompt",
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                        item {
                            SelectionContainer {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                ) {
                                    Text(
                                        text = it,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            }
                        }
                    }

                    config.jsonBody?.let { body ->
                        if (body.isNotEmpty()) {
                            item {
                                Text(
                                    text = "Request Body",
                                    style = MaterialTheme.typography.titleSmall
                                )
                            }
                            item {
                                SelectionContainer {
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                                        )
                                    ) {
                                        Text(
                                            text = body.entries.joinToString(",\n") {
                                                "\"${it.key}\": ${it.value}"
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            fontFamily = FontFamily.Monospace,
                                            modifier = Modifier.padding(12.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (config.additionalParams.isNotEmpty()) {
                        item {
                            Text(
                                text = "Additional Parameters",
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                        item {
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    config.additionalParams.forEach { (key, value) ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = key,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Text(
                                                text = value.toString(),
                                                style = MaterialTheme.typography.bodySmall,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (showPythonCode) {
                    item {
                        Text(
                            text = "Python Code (for reference)",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                SelectionContainer {
                                    Text(
                                        text = config.toPythonCode(),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Button(
                        onClick = { viewModel.saveConfig() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Add, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Import as API Config")
                    }
                }
            }

            item {
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun ParamRow(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(100.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
