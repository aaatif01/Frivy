package com.frivy.assistant.data

import com.frivy.assistant.config.ApiConfig
import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Streaming
import java.util.concurrent.TimeUnit

data class ChatRequest(
    val model: String,
    val messages: List<WireMessage>,
    val tools: List<ToolDefinition> = ToolCatalog.definitions,
    val stream: Boolean = true,
)

data class WireMessage(
    val role: String,
    val content: String? = null,
    @Json(name = "tool_calls") val toolCalls: List<ToolCall>? = null,
    @Json(name = "tool_call_id") val toolCallId: String? = null,
)

data class ToolDefinition(
    val type: String = "function",
    val function: FunctionDefinition,
)

data class FunctionDefinition(
    val name: String,
    val description: String,
    val parameters: JsonSchema,
)

data class JsonSchema(
    val type: String,
    val properties: Map<String, JsonSchema>? = null,
    val required: List<String>? = null,
)

data class ToolCall(
    val id: String? = null,
    val type: String? = null,
    val function: FunctionCall,
)

data class FunctionCall(
    val name: String,
    val arguments: String,
)

interface OpenRouterApi {
    @Streaming
    @POST("chat/completions")
    suspend fun stream(
        @Body body: ChatRequest,
    ): Response<ResponseBody>
}

object ApiFactory {
    fun create(): OpenRouterApi {
        val apiKey = ApiConfig.normalizedApiKey()
        require(
            apiKey.isNotBlank() &&
                apiKey != "PASTE_OPENROUTER_API_KEY_HERE",
        ) {
            "OpenRouter API key is missing. Set ApiConfig.API_KEY."
        }

        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .callTimeout(180, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request = chain.request()
                    .newBuilder()
                    .header("Authorization", "Bearer $apiKey")
                    .header("HTTP-Referer", "https://frivy.app")
                    .header("X-Title", "FRIVY")
                    .header("Accept", "text/event-stream")
                    .header("Content-Type", "application/json")
                    .build()

                chain.proceed(request)
            }
            .build()

        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()

        return Retrofit.Builder()
            .baseUrl(ApiConfig.BASE_URL)
            .client(client)
            .addConverterFactory(
                retrofit2.converter.moshi.MoshiConverterFactory.create(moshi),
            )
            .build()
            .create(OpenRouterApi::class.java)
    }
}