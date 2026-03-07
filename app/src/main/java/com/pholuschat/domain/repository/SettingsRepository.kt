package com.pholuschat.domain.repository

import com.pholuschat.domain.model.AppSettings
import com.pholuschat.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun getSettings(): Flow<AppSettings>
    suspend fun updateSettings(settings: AppSettings)

    fun getProfiles(): Flow<List<UserProfile>>
    fun getProfile(id: String): Flow<UserProfile?>
    suspend fun saveProfile(profile: UserProfile)
    suspend fun deleteProfile(id: String)
}
