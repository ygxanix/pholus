package com.pholuschat.domain.repository

import com.pholuschat.domain.model.ChatMessage
import com.pholuschat.domain.model.Conversation
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    fun getConversations(): Flow<List<Conversation>>
    fun getConversation(id: String): Flow<Conversation?>
    suspend fun saveConversation(conversation: Conversation)
    suspend fun deleteConversation(id: String)
    suspend fun addMessage(message: ChatMessage)
    suspend fun updateMessage(message: ChatMessage)
    suspend fun deleteMessage(messageId: String)
}
