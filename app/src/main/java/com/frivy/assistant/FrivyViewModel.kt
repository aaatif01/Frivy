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
import okhttp3.ResponseBody
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
        val history = dao.messages(chatId).first().map { WireMessage(it.role,it.content) }
        val result = runCatching { api.stream(ChatRequest(ApiConfig.MODEL, history + WireMessage("user",text))) }
        val response = result.getOrElse { error: Throwable ->
            _state.update { currentState ->
                currentState.copy(isThinking = false, error = networkError(error))
            }
            return@launch
        }
        if (!response.isSuccessful) { _state.update { it.copy(isThinking=false,error=httpError(response.code()) ) }; return@launch }
        val raw = response.body()?.string().orEmpty()
        val content = parseContent(raw)
        val tool = parseTool(raw)
        val final = if (tool != null) executor.execute(tool.first,tool.second) else content
        dao.saveMessage(Message(UUID.randomUUID().toString(),chatId,"assistant",final))
        _state.update { it.copy(isThinking=false) }
    } }
    fun regenerate() { state.value.messages.lastOrNull { it.role=="user" }?.let { send(it.content) } }
    private fun parseContent(raw:String) = raw.lineSequence().mapNotNull { it.removePrefix("data: ").takeIf { x -> x.startsWith("{") } }.joinToString("").let { runCatching { JSONObject(it).getJSONArray("choices").getJSONObject(0).getJSONObject("message").optString("content") }.getOrNull() ?: "I wasn't able to complete that request." }
    private fun parseTool(raw:String): Pair<String,String>? = runCatching { val m=JSONObject(raw).getJSONArray("choices").getJSONObject(0).getJSONObject("message"); val c=m.getJSONArray("tool_calls").getJSONObject(0); c.getJSONObject("function").let { it.getString("name") to it.getString("arguments") } }.getOrNull()
    private fun networkError(e:Throwable) = "I couldn't reach the FRIVY network service. Please check your connection."
    private fun httpError(code:Int) = when(code) { 401,403 -> "FRIVY's AI service authorization is unavailable."; 404 -> "That FRIVY service endpoint was not found."; 429 -> "FRIVY is busy right now. Please try again shortly."; in 500..599 -> "FRIVY's AI service is temporarily unavailable."; else -> "FRIVY couldn't complete that request." }
}