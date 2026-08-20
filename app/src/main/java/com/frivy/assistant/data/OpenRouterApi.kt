package com.frivy.assistant.data
import com.frivy.assistant.config.ApiConfig
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Streaming
import okhttp3.ResponseBody

data class ChatRequest(val model: String, val messages: List<WireMessage>, val tools: List<ToolDefinition> = ToolCatalog.definitions, val stream: Boolean = true)
data class WireMessage(val role: String, val content: String? = null, val tool_calls: List<ToolCall>? = null, val tool_call_id: String? = null)
data class ToolDefinition(val type: String = "function", val function: FunctionDefinition)
data class FunctionDefinition(val name: String, val description: String, val parameters: Map<String, Any?>)
data class ToolCall(val id: String, val type: String, val function: FunctionCall)
data class FunctionCall(val name: String, val arguments: String)
interface OpenRouterApi { @Streaming @POST("chat/completions") suspend fun stream(@Body body: ChatRequest): retrofit2.Response<ResponseBody> }
object ApiFactory {
    fun create(): OpenRouterApi {
        val client = okhttp3.OkHttpClient.Builder().addInterceptor { chain ->
            chain.proceed(chain.request().newBuilder().addHeader("Authorization", "Bearer ${ApiConfig.API_KEY}").addHeader("HTTP-Referer", "https://frivy.app").addHeader("X-Title", "FRIVY").build())
        }.build()
        return retrofit2.Retrofit.Builder().baseUrl(ApiConfig.BASE_URL).client(client).addConverterFactory(retrofit2.converter.moshi.MoshiConverterFactory.create()).build().create(OpenRouterApi::class.java)
    }
}