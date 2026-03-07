package com.pholuschat.data.util

import com.pholuschat.domain.model.ApiType
import com.pholuschat.domain.model.CurlConfig
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

object CurlParser {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val commonApiProviders = mapOf(
        "api.openai.com" to ApiType.OPENAI,
        "api.anthropic.com" to ApiType.OPENAI,
        "openrouter.ai" to ApiType.OPENROUTER,
        "ollama.ai" to ApiType.OLLAMA,
        "lmstudio.ai" to ApiType.LM_STUDIO,
        "localhost" to ApiType.OLLAMA,
        "127.0.0.1" to ApiType.OLLAMA
    )

    private val modelParamKeys = listOf(
        "model", "model_id", "modelName", "model_name", "engine"
    )

    private val temperatureParamKeys = listOf("temperature")
    private val maxTokensParamKeys = listOf("max_tokens", "maxTokens", "max_completion_tokens")
    private val systemPromptKeys = listOf("system", "system_prompt", "systemPrompt", "system_message")
    private val streamParamKeys = listOf("stream")
    private val topPParamKeys = listOf("top_p", "topP")
    private val presencePenaltyKeys = listOf("presence_penalty", "presencePenalty")
    private val frequencyPenaltyKeys = listOf("frequency_penalty", "frequencyPenalty")
    private val stopKeys = listOf("stop")
    private val messagesKeys = listOf("messages")
    private val promptKeys = listOf("prompt")

    fun parse(curlCommand: String): CurlConfig {
        val cleanCommand = curlCommand
            .replace("\\\n".toRegex(), " ")
            .replace("\\\r".toRegex(), "")
            .replace("\\s+".toRegex(), " ")
            .trim()

        val method = extractMethod(cleanCommand)
        val url = extractUrl(cleanCommand)
        val headers = extractHeaders(cleanCommand)
        val body = extractBody(cleanCommand)
        val jsonBody = body?.let { parseBodyAsJson(it) }

        val baseUrl = extractBaseUrl(url)
        val apiType = detectApiType(url, headers)
        
        val modelName = extractModelName(jsonBody, body)
        val temperature = extractFloatParam(jsonBody, body, temperatureParamKeys)
        val maxTokens = extractIntParam(jsonBody, body, maxTokensParamKeys)
        val systemPrompt = extractStringParam(jsonBody, body, systemPromptKeys)
        val stream = extractBoolParam(jsonBody, body, streamParamKeys)
        val topP = extractFloatParam(jsonBody, body, topPParamKeys)
        val presencePenalty = extractFloatParam(jsonBody, body, presencePenaltyKeys)
        val frequencyPenalty = extractFloatParam(jsonBody, body, frequencyPenaltyKeys)
        val stop = extractListParam(jsonBody, body, stopKeys)
        val messages = extractMessages(jsonBody, body, messagesKeys)
        val prompt = extractStringParam(jsonBody, body, promptKeys)

        val excludedKeys = listOf(
            modelParamKeys, temperatureParamKeys, maxTokensParamKeys,
            systemPromptKeys, streamParamKeys, topPParamKeys,
            presencePenaltyKeys, frequencyPenaltyKeys, stopKeys,
            messagesKeys, promptKeys
        ).flatten()

        val additionalParams = extractAdditionalParams(jsonBody, excludedKeys)

        return CurlConfig(
            rawCurl = curlCommand,
            method = method,
            url = url,
            headers = headers,
            body = body,
            jsonBody = jsonBody,
            modelName = modelName,
            temperature = temperature,
            maxTokens = maxTokens,
            systemPrompt = systemPrompt,
            stream = stream,
            topP = topP,
            presencePenalty = presencePenalty,
            frequencyPenalty = frequencyPenalty,
            stop = stop,
            messages = messages,
            prompt = prompt,
            additionalParams = additionalParams,
            detectedApiType = apiType,
            baseUrl = baseUrl
        )
    }

    private fun extractMethod(command: String): String {
        val methodPattern = """-X\s+['"]?(\w+)['"]?""".toRegex()
        methodPattern.find(command)?.let { return it.groupValues[1].uppercase() }

        val methodLongPattern = """--request\s+['"]?(\w+)['"]?""".toRegex()
        methodLongPattern.find(command)?.let { return it.groupValues[1].uppercase() }

        if (command.contains(Regex("""-d\s+['"]"""))) return "POST"
        if (command.contains(Regex("""--data(-raw|-binary)?\s+['"]"""))) return "POST"

        return "GET"
    }

    private fun extractUrl(command: String): String {
        val urlPattern = """['"]?(https?://[^\s'"]+)['"]?""".toRegex()
        return urlPattern.find(command)?.groupValues?.getOrNull(1) ?: ""
    }

    private fun extractBaseUrl(fullUrl: String): String {
        if (fullUrl.isBlank()) return ""
        return try {
            fullUrl.split("?").first().let { url ->
                val parts = url.split("/")
                if (parts.size >= 3) parts.take(3).joinToString("/") else url
            }.removeSuffix("/")
        } catch (e: Exception) { "" }
    }

    private fun extractHeaders(command: String): Map<String, String> {
        val headers = mutableMapOf<String, String>()
        
        val headerPattern = """-H\s+['"]([^:]+):\s*([^'"]+)['"]""".toRegex()
        headerPattern.findAll(command).forEach { match ->
            headers[match.groupValues[1].trim()] = match.groupValues[2].trim()
        }

        val headerLongPattern = """--header\s+['"]([^:]+):\s*([^'"]+)['"]""".toRegex()
        headerLongPattern.findAll(command).forEach { match ->
            headers[match.groupValues[1].trim()] = match.groupValues[2].trim()
        }

        return headers
    }

    private fun extractBody(command: String): String? {
        val patterns = listOf(
            """-d\s+['"](.+?)['"]\s*(?:-H|['"]?-|\s+$)""".toRegex(),
            """--data\s+['"](.+?)['"]""".toRegex(),
            """--data-raw\s+['"](.+?)['"]""".toRegex(),
            """--data-binary\s+['"](.+?)['"]""".toRegex()
        )

        for (pattern in patterns) {
            val match = pattern.find(command)
            if (match != null) {
                return match.groupValues[1]
                    .replace("\\n".toRegex(), "\n")
                    .replace("\\\"".toRegex(), "\"")
                    .trim()
            }
        }
        return null
    }

    private fun parseBodyAsJson(body: String): Map<String, Any>? {
        return try {
            val trimmed = body.trim()
            if (!trimmed.startsWith("{") && !trimmed.startsWith("[")) return null
            
            val jsonElement = json.parseToJsonElement(trimmed)
            if (jsonElement is JsonObject) {
                jsonElement.mapValues { (_, value) -> parseJsonElement(value) }
            } else null
        } catch (e: Exception) { null }
    }

    private fun parseJsonElement(element: JsonElement): Any {
        return when (element) {
            is JsonPrimitive -> {
                if (element.isString) element.content
                else if (element.boolean != null) element.boolean
                else element.content.toDoubleOrNull() ?: element.content
            }
            is JsonObject -> element.mapValues { (_, v) -> parseJsonElement(v) }
            is kotlinx.serialization.json.JsonArray -> element.map { parseJsonElement(it) }
            else -> element.toString()
        }
    }

    private fun detectApiType(url: String, headers: Map<String, String>): ApiType {
        val urlLower = url.lowercase()
        for ((domain, type) in commonApiProviders) {
            if (urlLower.contains(domain)) return type
        }
        val authHeader = headers["Authorization"] ?: headers["authorization"]
        if (authHeader?.contains("Bearer sk-") == true) return ApiType.OPENAI
        return ApiType.CUSTOM_REST
    }

    private fun extractModelName(jsonBody: Map<String, Any>?, rawBody: String?): String? {
        jsonBody?.let { body ->
            for (key in modelParamKeys) {
                body[key]?.toString()?.takeIf { it.isNotBlank() }?.let { return it }
            }
        }
        return null
    }

    private fun extractFloatParam(jsonBody: Map<String, Any>?, rawBody: String?, paramKeys: List<String>): Float? {
        jsonBody?.let { body ->
            for (key in paramKeys) {
                val value = body[key]
                if (value is Number) return value.toFloat()
                if (value is String) return value.toFloatOrNull()
            }
        }
        return null
    }

    private fun extractIntParam(jsonBody: Map<String, Any>?, rawBody: String?, paramKeys: List<String>): Int? {
        jsonBody?.let { body ->
            for (key in paramKeys) {
                val value = body[key]
                if (value is Number) return value.toInt()
                if (value is String) return value.toIntOrNull()
            }
        }
        return null
    }

    private fun extractStringParam(jsonBody: Map<String, Any>?, rawBody: String?, paramKeys: List<String>): String? {
        jsonBody?.let { body ->
            for (key in paramKeys) {
                body[key]?.toString()?.takeIf { it.isNotBlank() }?.let { return it }
            }
        }
        return null
    }

    private fun extractBoolParam(jsonBody: Map<String, Any>?, rawBody: String?, paramKeys: List<String>): Boolean? {
        jsonBody?.let { body ->
            for (key in paramKeys) {
                val value = body[key]
                if (value is Boolean) return value
                if (value is String) return value.toBooleanStrictOrNull()
                if (value is Number) return value.toInt() == 1
            }
        }
        return null
    }

    private fun extractListParam(jsonBody: Map<String, Any>?, rawBody: String?, paramKeys: List<String>): List<String>? {
        jsonBody?.let { body ->
            for (key in paramKeys) {
                val value = body[key]
                if (value is List<*>) return value.mapNotNull { it?.toString() }
                if (value is String && value.startsWith("[")) {
                    return value.removeSurrounding("[", "]")
                        .split(",")
                        .map { it.trim().removeSurrounding("\"") }
                        .filter { it.isNotBlank() }
                }
            }
        }
        return null
    }

    private fun extractMessages(jsonBody: Map<String, Any>?, rawBody: String?, paramKeys: List<String>): List<Map<String, String>>? {
        jsonBody?.let { body ->
            for (key in paramKeys) {
                val value = body[key]
                if (value is List<*>) {
                    return value.mapNotNull { item ->
                        if (item is Map<*, *>) {
                            item.mapKeys { it.key.toString() }
                                .mapValues { it.value.toString() }
                        } else null
                    }
                }
            }
        }
        return null
    }

    private fun extractAdditionalParams(jsonBody: Map<String, Any>?, excludedKeys: List<String>): Map<String, Any> {
        if (jsonBody == null) return emptyMap()
        return jsonBody.filter { (key, _) -> key !in excludedKeys }
    }

    fun validateCurl(curlCommand: String): ValidationResult {
        if (curlCommand.isBlank()) return ValidationResult.Error("cURL command is empty")
        if (!curlCommand.lowercase().startsWith("curl")) return ValidationResult.Error("Command must start with 'curl'")

        val url = extractUrl(curlCommand)
        if (url.isBlank()) return ValidationResult.Error("No URL found in cURL command")
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            return ValidationResult.Error("URL must start with http:// or https://")
        }

        return ValidationResult.Success
    }

    sealed class ValidationResult {
        data object Success : ValidationResult()
        data class Error(val message: String) : ValidationResult()
    }
}
