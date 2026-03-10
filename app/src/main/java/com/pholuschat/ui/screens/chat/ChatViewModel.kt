package com.pholuschat.ui.screens.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pholuschat.domain.model.ChatMessage
import com.pholuschat.domain.model.MessageRole
import com.pholuschat.domain.repository.ApiRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import com.pholuschat.domain.model.Model
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val isLoading: Boolean = false,
    val selectedModelName: String = "GPT-4o (OpenAI)"
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val apiRepository: ApiRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    fun onInputTextChanged(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun sendMessage() {
        val currentState = _uiState.value
        val text = currentState.inputText.trim()
        
        if (text.isEmpty() || currentState.isLoading) return

        val userMessage = ChatMessage(
            id = UUID.randomUUID().toString(),
            chatId = "default", // Would be passed from nav args
            role = MessageRole.USER,
            content = text,
            timestamp = System.currentTimeMillis(),
            modelUsed = currentState.selectedModelName
        )

        // Clear input, add user message, set loading
        _uiState.update { state ->
            state.copy(
                inputText = "",
                isLoading = true,
                messages = state.messages + userMessage
            )
        }

        viewModelScope.launch {
            try {
                // Collect the first emission from the Flow
                val configs = apiRepository.getApiConfigs().firstOrNull() ?: emptyList()
                if (configs.isEmpty()) {
                    addErrorMessage("Error: No API configuration found. Please add one in settings.")
                    return@launch
                }
                
                val config = configs.first()

                // Get models and find one matching the config
                val models = apiRepository.getModels().firstOrNull() ?: emptyList()
                val model = models.firstOrNull { it.apiConfigId == config.id }
                    ?: Model(name = config.defaultModel, apiConfigId = config.id)

                // Build streamed response content
                val responseBuilder = StringBuilder()

                val result = apiRepository.sendMessage(
                    apiConfig = config,
                    model = model,
                    messages = _uiState.value.messages,
                    systemPrompt = null,
                    temperature = 0.7f,
                    maxTokens = null,
                    onChunk = { chunk -> responseBuilder.append(chunk) }
                )
                
                result.fold(
                    onSuccess = { responseText ->
                        val aiMessage = ChatMessage(
                            id = UUID.randomUUID().toString(),
                            chatId = "default",
                            role = MessageRole.ASSISTANT,
                            content = responseText,
                            timestamp = System.currentTimeMillis(),
                            modelUsed = model.name
                        )
                        _uiState.update { it.copy(messages = it.messages + aiMessage) }
                    },
                    onFailure = { error ->
                        addErrorMessage("Error: ${error.message}")
                    }
                )
            } catch (e: Exception) {
                addErrorMessage("Connection error: ${e.message}")
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
    
    // Allows replying to a specific message
    fun setReplyTo(replyToMessageId: String) {
         // Optionally scroll to input, or cite the message in text
    }

    fun clearMessages() {
        _uiState.update { it.copy(messages = emptyList()) }
    }

    private fun addErrorMessage(errorMessage: String) {
        val errorMsg = ChatMessage(
            id = UUID.randomUUID().toString(),
            chatId = "default",
            role = MessageRole.SYSTEM,
            content = errorMessage,
            timestamp = System.currentTimeMillis(),
            modelUsed = "System"
        )
        _uiState.update { it.copy(messages = it.messages + errorMsg) }
    }
}
