package com.frivy.assistant.config
object ApiConfig {
    const val BASE_URL = "https://openrouter.ai/api/v1/"
    const val API_KEY = "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"
    const val MODEL = "openai/gpt-oss-120b"

    fun normalizedApiKey(): String =
        API_KEY.trim().removePrefix("Bearer ").trim()
}
