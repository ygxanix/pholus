package com.pholus.domain.model

import java.util.UUID

data class UserProfile(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "Default",
    val defaultApiConfigId: String? = null,
    val defaultModelId: String? = null,
    val themeSettings: ThemeSettings = ThemeSettings()
)

data class ThemeSettings(
    val isDarkMode: Boolean? = null, // null = follow system
    val useDynamicColors: Boolean = true,
    val accentColor: String? = null
)

data class MessageLimits(
    val maxTokensPerMessage: Int? = null,
    val maxMessagesPerDay: Int? = null,
    val rateLimitPerMinute: Int? = null
)

data class AppSettings(
    val themeSettings: ThemeSettings = ThemeSettings(),
    val messageLimits: MessageLimits = MessageLimits(),
    val enableStreaming: Boolean = true,
    val enableMarkdown: Boolean = true,
    val enableCodeHighlighting: Boolean = true,
    val showModelName: Boolean = true,
    val enableHaptics: Boolean = true,
    val defaultProfileId: String? = null
)
