package com.pholuschat.data.remote

import com.pholuschat.domain.model.ApiConfig
import com.pholuschat.domain.model.ApiType
import com.pholuschat.domain.model.ChatMessage
import com.pholuschat.domain.model.MessageRole
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.plugins.sse.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiClient @Inject constructor() {

    private var httpClient: HttpClient? = null
    private var currentConfig: ApiConfig? = null

    fun configure(config: ApiConfig) {
        if (currentConfig?.id != config.id) {
            httpClient?.close()
            httpClient = createClient(config)
            currentConfig = config
        }
    }

    private fun createClient(config: ApiConfig): HttpClient {
        return HttpClient(OkHttp) {
            engine {
                config {
                    connectTimeout(30, TimeUnit.SECONDS)
                    readTimeout(60, TimeUnit.SECONDS)
                    writeTimeout(30, TimeUnit.SECONDS)
                }
            }

            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                    prettyPrint = false
                    encodeDefaults = true
                })
            }

            install(Logging) {
                logger = Logger.DEFAULT
                level = LogLevel.HEADERS
            }

            install(HttpTimeout) {
                requestTimeoutMillis = 120000
                connectTimeoutMillis = 30000
                socketTimeoutMillis = 60000
            }

            defaultRequest {
                contentType(ContentType.Application.Json)
                config.headers.forEach { (key, value) ->
                    header(key, value)
                }
            }
        }
    }

    fun sendMessage(
        config: ApiConfig,
        messages: List<ChatMessage>,
        model: String,
        temperature: Float = 0.7f,
        maxTokens: Int? = null,
        systemPrompt: String? = null,
        stream: Boolean = true,
        topP: Float? = null,
        stop: List<String>? = null
    ): Flow<ApiResponse> = flow {
        configure(config)

        val client = httpClient ?: createClient(config).also { httpClient = it }

        val requestBody = buildRequestBody(
            config = config,
            messages = messages,
            model = model,
            temperature = temperature,
            maxTokens = maxTokens,
            systemPrompt = systemPrompt,
            stream = stream,
            topP = topP,
            stop = stop
        )

        val endpoint = buildEndpoint(config, model)

        try {
            if (stream) {
                client.preparePost(endpoint)
                    .apply {
                        config.apiKey?.let { apiKey ->
                            header("Authorization", "Bearer $apiKey")
                        }
                    }
                    .setBody(requestBody)
                    .execute { response ->
                        val channel = response.bodyAsChannel()
                        while (!channel.isClosedForRead) {
                            val line = channel.readUTF8Line()
                            if (line != null && line.startsWith("data: ")) {
                                val data = line.removePrefix("data: ").trim()
                                if (data == "[DONE]") {
                                    emit(ApiResponse.Done)
                                } else {
                                    val parsed = parseStreamingResponse(data)
                                    parsed?.let { emit(ApiResponse.Content(it)) }
                                }
                            }
                        }
                    }
            } else {
                val response = client.post(endpoint) {
                    config.apiKey?.let { apiKey ->
                        header("Authorization", "Bearer $apiKey")
                    }
                    setBody(requestBody)
                }

                val responseBody = response.bodyAsText()
                val parsed = parseNonStreamingResponse(responseBody)
                emit(ApiResponse.Content(parsed ?: responseBody))
                emit(ApiResponse.Done)
            }
        } catch (e: Exception) {
            emit(ApiResponse.Error(e.message ?: "Unknown error"))
        }
    }

    private fun buildRequestBody(
        config: ApiConfig,
        messages: List<ChatMessage>,
        model: String,
        temperature: Float,
        maxTokens: Int?,
        systemPrompt: String?,
        stream: Boolean,
        topP: Float?,
        stop: List<String>?
    ): String {
        val body = mutableMapOf<String, Any>(
            "model" to model,
            "stream" to stream
        )

        // Build messages array
        val messagesList = mutableListOf<Map<String, String>>()

        systemPrompt?.let {
            messagesList.add(mapOf("role" to "system", "content" to it))
        }

        messages.forEach { msg ->
            messagesList.add(
                mapOf(
                    "role" to msg.role.name.lowercase(),
                    "content" to msg.content
                )
            )
        }

        body["messages"] = messagesList

        temperature.let { body["temperature"] = it }
        maxTokens?.let { body["max_tokens"] = it }
        topP?.let { body["top_p"] = it }
        stop?.let { body["stop"] = it }

        // Handle custom body template from cURL import
        config.requestBodyTemplate?.let { template ->
            return template
        }

        return Json.encodeToString(body.mapValues { (_, value) ->
            when (value) {
                is String -> JsonPrimitive(value)
                is Number -> JsonPrimitive(value)
                is Boolean -> JsonPrimitive(value)
                is List<*> -> JsonArray(value.map { JsonPrimitive(it.toString()) })
                else -> JsonPrimitive(value.toString())
            }
        })
    }

    private fun buildEndpoint(config: ApiConfig, model: String): String {
        val baseUrl = config.baseUrl.trimEnd('/')

        return when (config.type) {
            ApiType.OPENAI, ApiType.OPENROUTER -> {
                if (baseUrl.contains("openrouter")) {
                    "$baseUrl/chat/completions"
                } else {
                    "$baseUrl/v1/chat/completions"
                }
            }
            ApiType.OLLAMA -> {
                "$baseUrl/api/chat"
            }
            ApiType.LM_STUDIO -> {
                "$baseUrl/v1/chat/completions"
            }
            ApiType.OPENAI_COMPATIBLE -> {
                "$baseUrl/v1/chat/completions"
            }
            ApiType.CUSTOM_REST -> {
                // Use the full URL from the cURL import
                config.baseUrl.let { it }
            }
        }
    }

    private fun parseStreamingResponse(data: String): String? {
        return try {
            val json = Json.parseToJsonElement(data)
            if (json is JsonObject) {
                // OpenAI format
                val choices = json["choices"] as? JsonArray
                val firstChoice = choices?.firstOrNull() as? JsonObject
                val delta = firstChoice?.get("delta") as? JsonObject
                val content = delta?.get("content") as? JsonPrimitive
                content?.content
            } else null
        } catch (e: Exception) {
            null
        }
    }

    private fun parseNonStreamingResponse(data: String): String? {
        return try {
            val json = Json.parseToJsonElement(data)
            if (json is JsonObject) {
                val choices = json["choices"] as? JsonArray
                val firstChoice = choices?.firstOrNull() as? JsonObject
                val message = firstChoice?.get("message") as? JsonObject
                val content = message?.get("content") as? JsonPrimitive
                content?.content
            } else null
        } catch (e: Exception) {
            null
        }
    }

    fun close() {
        httpClient?.close()
        httpClient = null
        currentConfig = null
    }
}

sealed class ApiResponse {
    data class Content(val text: String) : ApiResponse()
    data object Done : ApiResponse()
    data class Error(val message: String) : ApiResponse()
}
