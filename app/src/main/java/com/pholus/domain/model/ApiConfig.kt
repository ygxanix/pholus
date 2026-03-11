package com.pholus.domain.model

import java.util.UUID

data class ApiConfig(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val type: ApiType,
    val baseUrl: String,
    val apiKey: String? = null,
    val headers: Map<String, String> = emptyMap(),
    val defaultModel: String = "",
    val supportsStreaming: Boolean = true,
    val supportsVision: Boolean = false,
    val requestBodyTemplate: String? = null,
    val isDefault: Boolean = false
)

enum class ApiType {
    OPENAI,
    OPENAI_COMPATIBLE,
    OLLAMA,
    LM_STUDIO,
    CUSTOM_REST,
    OPENROUTER
}

data class Model(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val apiConfigId: String,
    val supportsVision: Boolean = false,
    val supportsStreaming: Boolean = true,
    val contextWindow: Int = 4096
)

data class Preset(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val modelId: String,
    val systemPrompt: String? = null,
    val temperature: Float = 0.7f,
    val maxTokens: Int? = null,
    val topP: Float? = null
)
