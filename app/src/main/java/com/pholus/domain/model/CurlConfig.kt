package com.pholus.domain.model

import java.util.UUID

data class CurlConfig(
    val id: String = UUID.randomUUID().toString(),
    val rawCurl: String = "",
    val method: String = "GET",
    val url: String = "",
    val headers: Map<String, String> = emptyMap(),
    val body: String? = null,
    val jsonBody: Map<String, Any>? = null,
    val modelName: String? = null,
    val temperature: Float? = null,
    val maxTokens: Int? = null,
    val systemPrompt: String? = null,
    val stream: Boolean? = null,
    val topP: Float? = null,
    val presencePenalty: Float? = null,
    val frequencyPenalty: Float? = null,
    val stop: List<String>? = null,
    val messages: List<Map<String, String>>? = null,
    val prompt: String? = null,
    val additionalParams: Map<String, Any> = emptyMap(),
    val detectedApiType: ApiType = ApiType.CUSTOM_REST,
    val baseUrl: String = ""
) {
    fun toApiConfig(): ApiConfig {
        return ApiConfig(
            name = modelName?.let { "Imported: $it" } ?: "Imported from cURL",
            type = detectedApiType,
            baseUrl = baseUrl,
            apiKey = headers["Authorization"]?.replace("Bearer ", "")
                ?: headers["api-key"]
                ?: headers["x-api-key"],
            headers = headers.filter {
                it.key.lowercase() !in listOf(
                    "authorization",
                    "api-key",
                    "x-api-key",
                    "content-type"
                )
            },
            defaultModel = modelName ?: "",
            supportsStreaming = stream ?: detectStreamingCapability(),
            supportsVision = detectVisionCapability(),
            requestBodyTemplate = body
        )
    }

    private fun detectStreamingCapability(): Boolean {
        return stream == true || 
               url.contains("stream") || 
               url.contains("/v1/chat/completions") ||
               additionalParams["stream"] == true
    }

    private fun detectVisionCapability(): Boolean {
        return url.contains("vision") ||
               additionalParams["vision"] == true ||
               jsonBody?.any { it.key.contains("image") } == true
    }

    fun toPythonCode(): String {
        val hasJsonBody = jsonBody != null
        
        return buildString {
            append("import requests\n")
            append("import json\n\n")
            
            append("# Configuration\n")
            append("BASE_URL = \"$baseUrl\"\n")
            append("API_KEY = \"YOUR_API_KEY\"\n\n")
            
            append("HEADERS = {\n")
            headers.forEach { (key, value) ->
                append("    \"$key\": \"$value\",\n")
            }
            append("    \"Authorization\": f\"Bearer {API_KEY}\",\n")
            append("}\n\n")
            
            if (hasJsonBody && jsonBody != null) {
                append("# Request payload\n")
                append("DATA = ")
                append(buildJsonBody())
                append("\n\n")
            }
            
            append("def chat(")
            if (messages != null && messages.isNotEmpty()) {
                append("messages: list")
            } else {
                append("prompt: str")
            }
            append(") -> dict:\n")
            
            if (hasJsonBody && jsonBody != null) {
                append("    payload = DATA.copy()\n")
                if (messages != null && messages.isNotEmpty()) {
                    append("    payload[\"messages\"] = messages\n")
                } else {
                    append("    payload[\"prompt\"] = prompt\n")
                }
                append("    \n")
                append("    response = requests.post(\n")
                append("        f\"{BASE_URL}$url\",\n")
                append("        headers=HEADERS,\n")
                append("        json=payload")
                if (stream == true) {
                    append(",\n        stream=True")
                }
                append("\n    )\n")
                append("    \n")
                if (stream == true) {
                    append("    # Streaming response\n")
                    append("    for line in response.iter_lines():\n")
                    append("        if line:\n")
                    append("            yield json.loads(line.decode('utf-8'))\n")
                } else {
                    append("    return response.json()\n")
                }
            } else {
                append("    response = requests.request(\n")
                append("        method=\"$method\",\n")
                append("        url=f\"{BASE_URL}$url\",\n")
                append("        headers=HEADERS\n")
                append("    )\n")
                append("    return response.json()\n")
            }
            
            append("\n\n")
            append("# Usage example\n")
            if (messages != null && messages.isNotEmpty()) {
                append("if __name__ == \"__main__\":\n")
                append("    messages = [\n")
                append("        {\"role\": \"system\", \"content\": \"You are a helpful assistant\"},\n")
                append("        {\"role\": \"user\", \"content\": \"Hello!\"}\n")
                append("    ]\n")
                append("    response = chat(messages)\n")
                append("    print(response)\n")
            } else {
                append("if __name__ == \"__main__\":\n")
                append("    response = chat(\"Hello, how are you?\")\n")
                append("    print(response)\n")
            }
        }
    }

    private fun buildJsonBody(): String {
        if (jsonBody == null) return "{}"
        
        return buildString {
            append("{\n")
            jsonBody.entries.forEachIndexed { index, (key, value) ->
                append("    \"$key\": ")
                append(formatJsonValue(value))
                if (index < jsonBody.size - 1) append(",")
                append("\n")
            }
            append("}")
        }
    }

    private fun formatJsonValue(value: Any): String {
        return when (value) {
            is String -> "\"$value\""
            is Number -> value.toString()
            is Boolean -> value.toString()
            is List<*> -> "[${value.joinToString(",") { formatJsonValue(it ?: "null") }}]"
            is Map<*, *> -> "{${value.entries.joinToString(",") { "\"${it.key}\": ${formatJsonValue(it.value ?: "null")}" }}}"
            else -> "\"$value\""
        }
    }
}

enum class ImportSource {
    CURL,
    JSON,
    PYTHON
}
