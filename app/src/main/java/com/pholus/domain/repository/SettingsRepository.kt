package com.pholus.domain.repository

import com.pholus.domain.model.AppSettings
import com.pholus.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun getSettings(): Flow<AppSettings>
    suspend fun updateSettings(settings: AppSettings)

    fun getProfiles(): Flow<List<UserProfile>>
    fun getProfile(id: String): Flow<UserProfile?>
    suspend fun saveProfile(profile: UserProfile)
    suspend fun deleteProfile(id: String)
}
