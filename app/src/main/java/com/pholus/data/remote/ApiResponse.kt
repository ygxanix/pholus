package com.pholus.data.remote

sealed class ApiResponse {
    data class Chunk(val text: String) : ApiResponse()
    data class Complete(val fullText: String) : ApiResponse()
    data class Error(val message: String, val cause: Throwable? = null) : ApiResponse()
}
