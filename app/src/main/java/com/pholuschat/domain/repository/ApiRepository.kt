package com.pholuschat.domain.repository

import com.pholuschat.data.remote.ApiResponse
import com.pholuschat.domain.model.ApiConfig
import com.pholuschat.domain.model.Model
import com.pholuschat.domain.model.Preset
import kotlinx.coroutines.flow.Flow

interface ApiRepository {
    fun getApiConfigs(): Flow<List<ApiConfig>>
    fun getApiConfig(id: String): Flow<ApiConfig?>
    suspend fun saveApiConfig(config: ApiConfig)
    suspend fun deleteApiConfig(id: String)

    fun getModels(): Flow<List<Model>>
    fun getModel(id: String): Flow<Model?>
    suspend fun saveModel(model: Model)
    suspend fun deleteModel(id: String)

    fun getPresets(): Flow<List<Preset>>
    suspend fun savePreset(preset: Preset)
    suspend fun deletePreset(id: String)

    suspend fun sendMessage(
        apiConfig: ApiConfig,
        model: Model,
        messages: List<com.pholuschat.domain.model.ChatMessage>,
        systemPrompt: String?,
        temperature: Float,
        maxTokens: Int?,
        onChunk: (String) -> Unit
    ): Result<String>

    fun sendMessageFlow(
        apiConfig: ApiConfig,
        model: Model,
        messages: List<com.pholuschat.domain.model.ChatMessage>,
        systemPrompt: String?,
        temperature: Float,
        maxTokens: Int?
    ): Flow<ApiResponse>
}
