package com.frivy.assistant.data
object ToolCatalog {
    private fun tool(name: String, description: String, properties: Map<String, Any?> = emptyMap(), required: List<String> = emptyList()) = ToolDefinition(function = FunctionDefinition(name, description, mapOf("type" to "object", "properties" to properties, "required" to required)))
    val definitions = listOf(
        tool("open_app","Open an installed Android app", mapOf("packageName" to mapOf("type" to "string")), listOf("packageName")),
        tool("close_app","Return to the home screen"), tool("open_settings","Open Android settings"), tool("open_wifi","Open WiFi settings"),
        tool("toggle_wifi","Enable or disable WiFi", mapOf("enabled" to mapOf("type" to "boolean")), listOf("enabled")),
        tool("open_bluetooth","Open Bluetooth settings"), tool("toggle_bluetooth","Enable or disable Bluetooth", mapOf("enabled" to mapOf("type" to "boolean")), listOf("enabled")),
        tool("toggle_flashlight","Enable or disable the camera flashlight", mapOf("enabled" to mapOf("type" to "boolean")), listOf("enabled")),
        tool("set_brightness","Set screen brightness percentage", mapOf("percent" to mapOf("type" to "integer")), listOf("percent")),
        tool("set_volume","Set media volume percentage", mapOf("percent" to mapOf("type" to "integer")), listOf("percent")),
        tool("open_notifications","Open notifications"), tool("open_quick_settings","Open quick settings"), tool("copy_to_clipboard","Copy text", mapOf("text" to mapOf("type" to "string")), listOf("text")),
        tool("open_camera","Open camera"), tool("open_gallery","Open gallery"), tool("open_contacts","Open contacts"), tool("open_phone","Open phone"), tool("open_messages","Open messages"),
        tool("web_search","Search the internet for current information", mapOf("query" to mapOf("type" to "string")), listOf("query")),
        tool("weather","Get current weather", mapOf("location" to mapOf("type" to "string")), listOf("location")),
        tool("stock_price","Get a live stock price", mapOf("symbol" to mapOf("type" to "string")), listOf("symbol")),
        tool("crypto_price","Get a live cryptocurrency price", mapOf("symbol" to mapOf("type" to "string")), listOf("symbol"))
    )
}