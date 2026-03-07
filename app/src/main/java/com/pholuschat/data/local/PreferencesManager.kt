package com.pholuschat.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.pholuschat.domain.model.ApiConfig
import com.pholuschat.domain.model.Model
import com.pholuschat.domain.model.Preset
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "pholus_prefs")

@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val gson = Gson()

    companion object {
        private val API_CONFIGS_KEY = stringPreferencesKey("api_configs")
        private val MODELS_KEY = stringPreferencesKey("models")
        private val PRESETS_KEY = stringPreferencesKey("presets")
        private val SELECTED_API_KEY = stringPreferencesKey("selected_api")
        private val SELECTED_MODEL_KEY = stringPreferencesKey("selected_model")
    }

    // API Configs
    val apiConfigs: Flow<List<ApiConfig>> = context.dataStore.data.map { prefs ->
        val json = prefs[API_CONFIGS_KEY] ?: "[]"
        try {
            val type = object : TypeToken<List<ApiConfig>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun saveApiConfig(config: ApiConfig) {
        context.dataStore.edit { prefs ->
            val currentJson = prefs[API_CONFIGS_KEY] ?: "[]"
            val type = object : TypeToken<MutableList<ApiConfig>>() {}.type
            val configs: MutableList<ApiConfig> = try {
                gson.fromJson(currentJson, type) ?: mutableListOf()
            } catch (e: Exception) {
                mutableListOf()
            }

            val existingIndex = configs.indexOfFirst { it.id == config.id }
            if (existingIndex >= 0) {
                configs[existingIndex] = config
            } else {
                configs.add(config)
            }

            prefs[API_CONFIGS_KEY] = gson.toJson(configs)
        }
    }

    suspend fun deleteApiConfig(id: String) {
        context.dataStore.edit { prefs ->
            val currentJson = prefs[API_CONFIGS_KEY] ?: "[]"
            val type = object : TypeToken<MutableList<ApiConfig>>() {}.type
            val configs: MutableList<ApiConfig> = try {
                gson.fromJson(currentJson, type) ?: mutableListOf()
            } catch (e: Exception) {
                mutableListOf()
            }
            configs.removeAll { it.id == id }
            prefs[API_CONFIGS_KEY] = gson.toJson(configs)
        }
    }

    // Models
    val models: Flow<List<Model>> = context.dataStore.data.map { prefs ->
        val json = prefs[MODELS_KEY] ?: "[]"
        try {
            val type = object : TypeToken<List<Model>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun saveModel(model: Model) {
        context.dataStore.edit { prefs ->
            val currentJson = prefs[MODELS_KEY] ?: "[]"
            val type = object : TypeToken<MutableList<Model>>() {}.type
            val models: MutableList<Model> = try {
                gson.fromJson(currentJson, type) ?: mutableListOf()
            } catch (e: Exception) {
                mutableListOf()
            }

            val existingIndex = models.indexOfFirst { it.id == model.id }
            if (existingIndex >= 0) {
                models[existingIndex] = model
            } else {
                models.add(model)
            }

            prefs[MODELS_KEY] = gson.toJson(models)
        }
    }

    suspend fun deleteModel(id: String) {
        context.dataStore.edit { prefs ->
            val currentJson = prefs[MODELS_KEY] ?: "[]"
            val type = object : TypeToken<MutableList<Model>>() {}.type
            val models: MutableList<Model> = try {
                gson.fromJson(currentJson, type) ?: mutableListOf()
            } catch (e: Exception) {
                mutableListOf()
            }
            models.removeAll { it.id == id }
            prefs[MODELS_KEY] = gson.toJson(models)
        }
    }

    // Selected values
    val selectedApiId: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[SELECTED_API_KEY]
    }

    val selectedModelId: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[SELECTED_MODEL_KEY]
    }

    suspend fun setSelectedApi(apiId: String) {
        context.dataStore.edit { prefs ->
            prefs[SELECTED_API_KEY] = apiId
        }
    }

    suspend fun setSelectedModel(modelId: String) {
        context.dataStore.edit { prefs ->
            prefs[SELECTED_MODEL_KEY] = modelId
        }
    }

    // Presets
    val presets: Flow<List<Preset>> = context.dataStore.data.map { prefs ->
        val json = prefs[PRESETS_KEY] ?: "[]"
        try {
            val type = object : TypeToken<List<Preset>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun savePreset(preset: Preset) {
        context.dataStore.edit { prefs ->
            val currentJson = prefs[PRESETS_KEY] ?: "[]"
            val type = object : TypeToken<MutableList<Preset>>() {}.type
            val presets: MutableList<Preset> = try {
                gson.fromJson(currentJson, type) ?: mutableListOf()
            } catch (e: Exception) {
                mutableListOf()
            }

            val existingIndex = presets.indexOfFirst { it.id == preset.id }
            if (existingIndex >= 0) {
                presets[existingIndex] = preset
            } else {
                presets.add(preset)
            }

            prefs[PRESETS_KEY] = gson.toJson(presets)
        }
    }

    suspend fun deletePreset(id: String) {
        context.dataStore.edit { prefs ->
            val currentJson = prefs[PRESETS_KEY] ?: "[]"
            val type = object : TypeToken<MutableList<Preset>>() {}.type
            val presets: MutableList<Preset> = try {
                gson.fromJson(currentJson, type) ?: mutableListOf()
            } catch (e: Exception) {
                mutableListOf()
            }
            presets.removeAll { it.id == id }
            prefs[PRESETS_KEY] = gson.toJson(presets)
        }
    }
}
