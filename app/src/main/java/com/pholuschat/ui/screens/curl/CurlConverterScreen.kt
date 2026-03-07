package com.pholuschat.ui.screens.curl

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.pholuschat.data.util.CurlParser
import com.pholuschat.domain.model.ApiType
import com.pholuschat.domain.model.CurlConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurlConverterScreen(
    onNavigateBack: () -> Unit,
    onImportConfig: (CurlConfig) -> Unit = {}
) {
    var curlInput by remember { mutableStateOf("") }
    var parsedConfig by remember { mutableStateOf<CurlConfig?>(null) }
    var showPythonCode by remember { mutableStateOf(false) }
    var showAdvancedParams by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isValidating by remember { mutableStateOf(false) }

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
                        IconButton(onClick = { 
                            parsedConfig?.let { onImportConfig(it) }
                        }) {
                            Icon(Icons.Default.Save, "Save")
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
                    onValueChange = {
                        curlInput = it
                        errorMessage = null
                        parsedConfig = null
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    label = { Text("cURL Command") },
                    placeholder = { Text("curl -X POST https://api.openai.com/v1/chat/completions -H 'Authorization: Bearer KEY' -d '{\"model\": \"gpt-4\"}')" },
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
                        onClick = {
                            isValidating = true
                            val validation = CurlParser.validateCurl(curlInput)
                            when (validation) {
                                is CurlParser.ValidationResult.Error -> {
                                    errorMessage = validation.message
                                    isValidating = false
                                }
                                is CurlParser.ValidationResult.Success -> {
                                    try {
                                        parsedConfig = CurlParser.parse(curlInput)
                                        errorMessage = null
                                    } catch (e: Exception) {
                                        errorMessage = "Failed to parse: ${e.message}"
                                    }
                                    isValidating = false
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = curlInput.isNotBlank() && !isValidating
                    ) {
                        if (isValidating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Parse, null)
                        }
                        Spacer(Modifier.width(8.dp))
                        Text("Parse cURL")
                    }

                    if (parsedConfig != null) {
                        OutlinedButton(
                            onClick = { showPythonCode = !showPythonCode },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Code, null)
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
                                Icons.Default.Error,
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
                                    ApiType.OPENAI -> Icons.Default.Cloud
                                    ApiType.OPENROUTER -> Icons.Default.Router
                                    ApiType.OLLAMA -> Icons.Default.Terminal
                                    ApiType.LM_STUDIO -> Icons.Default.Code
                                    else -> Icons.Default.Api
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
                                ParamRow("Model", it, Icons.Default.SmartToy)
                            }
                            ParamRow("Method", config.method, Icons.Default.SwapVert)
                            ParamRow("Base URL", config.baseUrl, Icons.Default.Link)
                            
                            config.temperature?.let {
                                ParamRow("Temperature", it.toString(), Icons.Default.Thermostat)
                            }
                            config.maxTokens?.let {
                                ParamRow("Max Tokens", it.toString(), Icons.Default.TextFields)
                            }
                            config.stream?.let {
                                ParamRow("Streaming", if (it) "Enabled" else "Disabled", Icons.Default.Stream)
                            }
                            config.topP?.let {
                                ParamRow("Top P", it.toString(), Icons.Default.TrendingUp)
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
                        onClick = { showAdvancedParams = !showAdvancedParams }
                    ) {
                        Icon(
                            if (showAdvancedParams) Icons.Default.ExpandLess 
                            else Icons.Default.ExpandMore, 
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
                        onClick = { onImportConfig(config) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, null)
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
