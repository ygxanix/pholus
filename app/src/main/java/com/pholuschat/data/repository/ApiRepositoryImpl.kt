package com.pholuschat.data.repository

import com.pholuschat.data.local.PreferencesManager
import com.pholuschat.data.remote.ApiClient
import com.pholuschat.data.remote.ApiResponse
import com.pholuschat.domain.model.ApiConfig
import com.pholuschat.domain.model.ChatMessage
import com.pholuschat.domain.model.Model
import com.pholuschat.domain.model.Preset
import com.pholuschat.domain.repository.ApiRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiRepositoryImpl @Inject constructor(
    private val apiClient: ApiClient,
    private val preferencesManager: PreferencesManager
) : ApiRepository {

    // API Configs
    override fun getApiConfigs(): Flow<List<ApiConfig>> = preferencesManager.apiConfigs

    override fun getApiConfig(id: String): Flow<ApiConfig?> =
        preferencesManager.apiConfigs.map { configs -> configs.find { it.id == id } }

    override suspend fun saveApiConfig(config: ApiConfig) =
        preferencesManager.saveApiConfig(config)

    override suspend fun deleteApiConfig(id: String) =
        preferencesManager.deleteApiConfig(id)

    // Models
    override fun getModels(): Flow<List<Model>> = preferencesManager.models

    override fun getModel(id: String): Flow<Model?> =
        preferencesManager.models.map { models -> models.find { it.id == id } }

    override suspend fun saveModel(model: Model) = preferencesManager.saveModel(model)

    override suspend fun deleteModel(id: String) = preferencesManager.deleteModel(id)

    // Presets
    override fun getPresets(): Flow<List<Preset>> = preferencesManager.presets

    override suspend fun savePreset(preset: Preset) = preferencesManager.savePreset(preset)

    override suspend fun deletePreset(id: String) = preferencesManager.deletePreset(id)

    // Messaging
    override suspend fun sendMessage(
        apiConfig: ApiConfig,
        model: Model,
        messages: List<ChatMessage>,
        systemPrompt: String?,
        temperature: Float,
        maxTokens: Int?,
        onChunk: (String) -> Unit
    ): Result<String> {
        return try {
            var fullResponse = ""
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
                    is ApiResponse.Done -> { /* Streaming complete */ }
                    is ApiResponse.Error -> throw Exception(response.message)
                }
            }
            Result.success(fullResponse)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun sendMessageFlow(
        apiConfig: ApiConfig,
        model: Model,
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
