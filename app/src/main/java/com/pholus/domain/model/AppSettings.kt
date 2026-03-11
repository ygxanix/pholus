package com.pholus.domain.model

data class AppSettings(
    val theme: String = "System",
    val dynamicColorsEnabled: Boolean = true,
    val streamingEnabled: Boolean = true,
    val codeHighlightingEnabled: Boolean = true,
    val showModelName: Boolean = true,
    val maxTokens: Int = 4096,
    val defaultApiConfigId: String? = null,
    val defaultModelId: String? = null
)
