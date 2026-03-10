package com.pholuschat.ui.screens.curl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pholuschat.data.util.CurlParser
import com.pholuschat.domain.model.CurlConfig
import com.pholuschat.domain.repository.ApiRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class CurlConverterUiState(
    val curlInput: String = "",
    val parsedConfig: CurlConfig? = null,
    val showPythonCode: Boolean = false,
    val showAdvancedParams: Boolean = false,
    val errorMessage: String? = null,
    val isValidating: Boolean = false,
    val isSaved: Boolean = false
)

@HiltViewModel
class CurlConverterViewModel @Inject constructor(
    private val apiRepository: ApiRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CurlConverterUiState())
    val uiState: StateFlow<CurlConverterUiState> = _uiState.asStateFlow()

    fun onInputChanged(input: String) {
        _uiState.update { 
            it.copy(
                curlInput = input,
                errorMessage = null,
                parsedConfig = null,
                isSaved = false
            ) 
        }
    }

    fun togglePythonCode(show: Boolean) {
        _uiState.update { it.copy(showPythonCode = show) }
    }

    fun toggleAdvancedParams(show: Boolean) {
        _uiState.update { it.copy(showAdvancedParams = show) }
    }

    fun parseCommand() {
        val input = _uiState.value.curlInput.trim()
        if (input.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Please enter a cURL command") }
            return
        }

        _uiState.update { it.copy(isValidating = true, errorMessage = null) }

        // Run validation and parsing off the main UI thread
        viewModelScope.launch {
            val result = withContext(Dispatchers.Default) {
                CurlParser.validateCurl(input)
            }

            when (result) {
                is CurlParser.ValidationResult.Error -> {
                    _uiState.update { 
                        it.copy(
                            errorMessage = result.message,
                            isValidating = false
                        ) 
                    }
                }
                is CurlParser.ValidationResult.Success -> {
                    try {
                        val parsed = withContext(Dispatchers.Default) {
                            CurlParser.parse(input)
                        }
                        _uiState.update { 
                            it.copy(
                                parsedConfig = parsed,
                                errorMessage = null,
                                isValidating = false
                            ) 
                        }
                    } catch (e: Exception) {
                        _uiState.update { 
                            it.copy(
                                errorMessage = "Failed to parse: ${e.message}",
                                isValidating = false
                            ) 
                        }
                    }
                }
            }
        }
    }

    fun saveConfig() {
        val config = _uiState.value.parsedConfig?.toApiConfig() ?: return
        
        viewModelScope.launch {
            try {
                apiRepository.saveApiConfig(config)
                _uiState.update { it.copy(isSaved = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to save: ${e.message}") }
            }
        }
    }
}
