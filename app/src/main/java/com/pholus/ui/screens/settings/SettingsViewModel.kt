package com.pholus.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class SettingsUiState(
    val selectedTheme: String = "System",
    val dynamicColorsEnabled: Boolean = true,
    val streamingEnabled: Boolean = true,
    val codeHighlightingEnabled: Boolean = true,
    val showModelName: Boolean = true,
    val maxTokens: String = "4096"
)

@HiltViewModel
class SettingsViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    // In a real app we would read/write from PreferencesManager here.
    // Since PreferenceManager mostly holds ApiConfigs right now, 
    // we just hold state in memory for the session to prevent rotation wipe.

    fun setTheme(theme: String) {
        _uiState.update { it.copy(selectedTheme = theme) }
    }

    fun toggleDynamicColors(enabled: Boolean) {
        _uiState.update { it.copy(dynamicColorsEnabled = enabled) }
    }

    fun toggleStreaming(enabled: Boolean) {
        _uiState.update { it.copy(streamingEnabled = enabled) }
    }

    fun toggleCodeHighlighting(enabled: Boolean) {
        _uiState.update { it.copy(codeHighlightingEnabled = enabled) }
    }

    fun toggleShowModelName(enabled: Boolean) {
        _uiState.update { it.copy(showModelName = enabled) }
    }

    fun setMaxTokens(tokens: String) {
        _uiState.update { it.copy(maxTokens = tokens) }
    }
}
