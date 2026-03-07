package com.pholuschat.data.repository

import com.pholuschat.data.remote.ApiClient
import com.pholuschat.data.remote.ApiResponse
import com.pholuschat.domain.model.ApiConfig
import com.pholuschat.domain.model.ChatMessage
import com.pholuschat.domain.model.Conversation
import com.pholuschat.domain.model.MessageRole
import com.pholuschat.domain.repository.ApiRepository
import com.pholuschat.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiRepositoryImpl @Inject constructor(
    private val apiClient: ApiClient
) : ApiRepository {

    override fun sendMessage(
        apiConfig: ApiConfig,
        model: com.pholuschat.domain.model.Model,
        messages: List<ChatMessage>,
        systemPrompt: String?,
        temperature: Float,
        maxTokens: Int?,
        onChunk: (String) -> Unit
    ): Result<String> {
        return try {
            var fullResponse = ""

            // Using runBlocking for simplicity - in production would use proper coroutine handling
            kotlinx.coroutines.runBlocking {
                apiClient.sendMessage(
                    config = apiConfig,
                    messages = messages,
                    model = model.name,
                    temperature = temperature,
                    maxTokens = maxTokens,
                    systemPrompt = systemPrompt,
                    stream = apiConfig.supportsStreaming
                ).collect { response ->
                    when (response) {
                        is ApiResponse.Content -> {
                            fullResponse += response.text
                            onChunk(response.text)
                        }
                        is ApiResponse.Done -> {
                            // Streaming complete
                        }
                        is ApiResponse.Error -> {
                            throw Exception(response.message)
                        }
                    }
                }
            }

            Result.success(fullResponse)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun sendMessageFlow(
        apiConfig: ApiConfig,
        model: com.pholuschat.domain.model.Model,
        messages: List<ChatMessage>,
        systemPrompt: String?,
        temperature: Float,
        maxTokens: Int?
    ): Flow<ApiResponse> {
        return apiClient.sendMessage(
            config = apiConfig,
            messages = messages,
            model = model.name,
            temperature = temperature,
            maxTokens = maxTokens,
            systemPrompt = systemPrompt,
            stream = apiConfig.supportsStreaming
        )
    }
}
