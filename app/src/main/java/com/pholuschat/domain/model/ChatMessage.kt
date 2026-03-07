package com.pholuschat.domain.model

import java.util.UUID

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val chatId: String,
    val role: MessageRole,
    val content: String,
    val repliedToId: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val modelUsed: String,
    val isStreaming: Boolean = false
)

enum class MessageRole {
    USER,
    ASSISTANT,
    SYSTEM
}
