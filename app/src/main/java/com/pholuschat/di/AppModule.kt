package com.pholuschat.di

import com.pholuschat.data.remote.ApiClient
import com.pholuschat.data.repository.ApiRepositoryImpl
import com.pholuschat.domain.repository.ApiRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindApiRepository(
        impl: ApiRepositoryImpl
    ): ApiRepository
}

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideApiClient(): ApiClient {
        return ApiClient()
    }
}
