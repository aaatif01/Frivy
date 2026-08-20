package com.frivy.assistant.config
object ApiConfig {
    const val BASE_URL = "https://openrouter.ai/api/v1/"
    const val API_KEY = "PASTE_OPENROUTER_API_KEY_HERE"
    const val MODEL = "openai/gpt-oss-120b"

    fun normalizedApiKey(): String =
        API_KEY.trim().removePrefix("Bearer ").trim()
}