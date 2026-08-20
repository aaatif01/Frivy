package com.frivy.assistant
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.frivy.assistant.config.ApiConfig
import com.frivy.assistant.data.*
import com.frivy.assistant.device.DeviceActionExecutor
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.UUID

data class UiState(val chatId: String, val messages: List<Message> = emptyList(), val isThinking: Boolean = false, val error: String? = null)
class FrivyViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = Room.databaseBuilder(app, FrivyDatabase::class.java, "frivy.db").build().dao()
    private val api = ApiFactory.create()
    private val executor = DeviceActionExecutor(app)
    private val _state = MutableStateFlow(UiState(UUID.randomUUID().toString()))
    val state = _state.asStateFlow()
    init { viewModelScope.launch { dao.saveChat(Chat(_state.value.chatId,"FRIVY session")); dao.messages(_state.value.chatId).collect { _state.update { s -> s.copy(messages=it) } } } }
    fun send(text: String) { if(text.isBlank() || _state.value.isThinking) return; viewModelScope.launch {
        val chatId = _state.value.chatId; dao.saveMessage(Message(UUID.randomUUID().toString(),chatId,"user",text)); _state.update { it.copy(isThinking=true,error=null) }
        val history = dao.messages(chatId).first().map { message ->
            WireMessage(message.role, message.content)
        }
        val result = runCatching { api.stream(ChatRequest(ApiConfig.MODEL, history + WireMessage("user",text))) }
        val response = result.getOrElse { error: Throwable ->
            _state.update { currentState ->
                currentState.copy(isThinking = false, error = networkError(error))
            }
            return@launch
        }
        if (!response.isSuccessful) {
            val responseBody = response.errorBody()?.string().orEmpty()
            _state.update {
                it.copy(
                    isThinking = false,
                    error = httpError(response.code(), responseBody),
                )
            }
            return@launch
        }
        val raw = response.body()?.string().orEmpty()
        val content = parseStreamContent(raw)
        val tool = parseStreamTool(raw)
        val final = if (tool != null) executor.execute(tool.first,tool.second) else content
        dao.saveMessage(Message(UUID.randomUUID().toString(),chatId,"assistant",final))
        _state.update { it.copy(isThinking=false) }
    } }
    fun regenerate() { state.value.messages.lastOrNull { it.role=="user" }?.let { send(it.content) } }
    private fun parseStreamContent(raw: String): String {
        val content = raw.lineSequence()
            .filter { it.startsWith("data: ") }
            .mapNotNull { line ->
                val payload = line.removePrefix("data: ").trim()
                if (payload == "[DONE]") {
                    null
                } else {
                    runCatching {
                        JSONObject(payload)
                            .getJSONArray("choices")
                            .getJSONObject(0)
                            .getJSONObject("delta")
                            .optString("content")
                            .takeIf { it.isNotEmpty() }
                    }.getOrNull()
                }
            }
            .joinToString("")

        if (content.isNotBlank()) return content

        return runCatching {
            JSONObject(raw)
                .getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .optString("content")
        }.getOrNull().orEmpty().ifBlank {
            "I wasn't able to complete that request."
        }
    }

    private fun parseStreamTool(raw: String): Pair<String, String>? {
        return raw.lineSequence()
            .filter { it.startsWith("data: ") }
            .mapNotNull { line ->
                val payload = line.removePrefix("data: ").trim()
                if (payload == "[DONE]") {
                    null
                } else {
                    runCatching {
                        val delta = JSONObject(payload)
                            .getJSONArray("choices")
                            .getJSONObject(0)
                            .getJSONObject("delta")
                        val toolCall = delta
                            .optJSONArray("tool_calls")
                            ?.optJSONObject(0)
                            ?: return@runCatching null
                        val function = toolCall.optJSONObject("function")
                            ?: return@runCatching null
                        function.optString("name").takeIf { it.isNotBlank() }
                            ?.let { name -> name to function.optString("arguments") }
                    }.getOrNull()
                }
            }
            .firstOrNull()
            ?: runCatching {
                val message = JSONObject(raw)
                    .getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                val toolCall = message
                    .getJSONArray("tool_calls")
                    .getJSONObject(0)
                val function = toolCall.getJSONObject("function")
                function.getString("name") to function.optString("arguments")
            }.getOrNull()
    }

    private fun networkError(error: Throwable): String {
        val detail = error.message?.trim().orEmpty()
        return if (detail.isBlank()) {
            "FRIVY network error: ${error::class.java.simpleName}"
        } else {
            "FRIVY network error: ${error::class.java.simpleName}: $detail"
        }
    }

    private fun httpError(code: Int, body: String): String {
        val detail = body
            .replace(Regex("\\s+"), " ")
            .trim()
            .take(500)

        return if (detail.isBlank()) {
            "FRIVY HTTP error $code."
        } else {
            "FRIVY HTTP error $code: $detail"
        }
    }
}