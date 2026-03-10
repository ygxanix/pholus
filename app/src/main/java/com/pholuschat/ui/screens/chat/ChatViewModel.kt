package com.pholuschat.ui.screens.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pholuschat.domain.model.ChatMessage
import com.pholuschat.domain.model.MessageRole
import com.pholuschat.domain.repository.ApiRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
                // In a true implementation, we'd get the current ApiConfig
                // For now, we simulate fetching the config
                val configs = apiRepository.getApiConfigs()
                if (configs.isEmpty()) {
                    addErrorMessage("Error: No API configuration found. Please add one in settings.")
                    return@launch
                }
                
                // We just use the first one for demonstration
                val config = configs.first()

                val result = apiRepository.sendMessage(config, _uiState.value.messages)
                
                result.fold(
                    onSuccess = { responseText ->
                        val aiMessage = ChatMessage(
                            id = UUID.randomUUID().toString(),
                            chatId = "default",
                            role = MessageRole.ASSISTANT,
                            content = responseText,
                            timestamp = System.currentTimeMillis(),
                            modelUsed = config.name
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
