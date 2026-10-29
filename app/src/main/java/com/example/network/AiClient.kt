package com.example.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

sealed class AiResponse {
    data class Text(val content: String) : AiResponse() {
        val text: String get() = content
    }
    data class ToolCall(
        val callId: String,
        val toolName: String,
        val arguments: Map<String, Any>,
        val assistantText: String? = null
    ) : AiResponse()
    data class Error(val message: String) : AiResponse()
}

data class GeneratedImageResult(
    val base64Data: String? = null,
    val imageUrl: String? = null,
    val revisedPrompt: String? = null,
    val error: String? = null
)

data class ChatHistoryItem(
    val role: String, // "user", "model", "assistant"
    val text: String? = null,
    val toolCallName: String? = null,
    val toolCallArgs: Map<String, Any>? = null,
    val toolCallId: String? = null,
    val toolResult: String? = null
)

class AiClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val TAG = "AiClient"
        const val DEFAULT_GEMINI_MODEL = "gemini-3.8-flash"
        const val DEFAULT_CLAUDE_MODEL = "claude-sonnet-5-5"

        const val SYSTEM_PROMPT_HI =
            "तुम ममता नाम की एक मददगार AI असिस्टेंट हो जो यूज़र का Android फ़ोन चला सकती है। हमेशा हिंदी में छोटे और साफ़ जवाब दो। फ़ोन का कोई काम हो तो tools का इस्तेमाल करो। ताज़ा खबर, मौसम, भाव या कोई भी ऑनलाइन जानकारी चाहिए तो web_search का इस्तेमाल करो। जब यूज़र 'अब से हमेशा...' जैसा कोई स्थायी नियम बताए, तभी save_rule tool से उसे सेव करो। get_location से फ़ोन की लोकेशन मिलती है। emergency_sos सिर्फ़ तब इस्तेमाल करो जब यूज़र को साफ़ तौर पर आपातकालीन मदद चाहिए। कॉल, SMS और मैसेज के जवाब से पहले ऐप खुद यूज़र से अनुमति माँगता है। किसी ऐप के अंदर काम करना हो तो पहले read_screen से स्क्रीन देखो, फिर tap_text या type_text करो।"

        const val SYSTEM_PROMPT_EN =
            "You are Mamta, a helpful AI assistant that operates the user's Android phone. Answer concisely and clearly in English or Hindi. Whenever the user asks to perform an action on the phone, use the tools. For latest news, weather, prices, or online queries, use web_search. When the user specifies a permanent rule (e.g. 'From now on always...'), use save_rule. Use get_location for GPS location. Use emergency_sos only when the user needs emergency assistance. The app confirms with the user before calls, SMS, or replies."
    }

    suspend fun sendPrompt(
        provider: String = "gemini",
        apiKey: String,
        modelName: String = "",
        history: List<ChatHistoryItem>,
        language: String = "hi",
        userRules: List<String> = emptyList()
    ): AiResponse = withContext(Dispatchers.IO) {
        val cleanKey = apiKey.trim()
        val isClaude = provider.equals("claude", ignoreCase = true) || cleanKey.startsWith("sk-ant-")
        val isOpenAi = provider.equals("openai", ignoreCase = true) || (cleanKey.startsWith("sk-") && !isClaude)

        if (cleanKey.isBlank() || cleanKey == "MY_GEMINI_API_KEY") {
            val providerName = when {
                isOpenAi -> "OpenAI"
                isClaude -> "Claude"
                else -> "Gemini"
            }
            val msg = if (isOpenAi) {
                "OpenAI API key is empty. Please set your API key in Settings."
            } else if (language.startsWith("hi")) {
                "कृपया ⚙️ सेटिंग में जाकर $providerName की API key डालें।"
            } else {
                "Please configure your $providerName API key in Settings."
            }
            return@withContext AiResponse.Error(msg)
        }

        if (isOpenAi) {
            val model = modelName.trim().ifBlank { "gpt-4o-mini" }
            sendOpenAiRequest(cleanKey, model, history, language, userRules)
        } else if (isClaude) {
            val model = modelName.trim().ifBlank { DEFAULT_CLAUDE_MODEL }
            sendAnthropicRequest(cleanKey, model, history, language, userRules)
        } else {
            val model = modelName.trim().ifBlank { DEFAULT_GEMINI_MODEL }
            sendGeminiRequest(cleanKey, model, history, language, userRules)
        }
    }

    private fun sendGeminiRequest(
        apiKey: String,
        modelName: String,
        history: List<ChatHistoryItem>,
        language: String,
        userRules: List<String>
    ): AiResponse {
        return try {
            val baseText = if (language.startsWith("hi")) SYSTEM_PROMPT_HI else SYSTEM_PROMPT_EN
            val rulesSuffix = if (userRules.isNotEmpty()) {
                "\n\nयूज़र के नियम (इन्हें सबसे ऊपर मानो):\n- " + userRules.joinToString("\n- ")
            } else ""
            val systemText = baseText + rulesSuffix

            val cleanModel = modelName.removePrefix("models/").ifBlank { DEFAULT_GEMINI_MODEL }
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$cleanModel:generateContent?key=$apiKey"

            val contentsArray = JSONArray()

            // Build conversation history
            for (item in history) {
                val contentObj = JSONObject()
                val partsArray = JSONArray()

                when {
                    item.toolResult != null -> {
                        contentObj.put("role", "user")
                        val funcResponse = JSONObject()
                        val responsePayload = JSONObject()
                        responsePayload.put("result", item.toolResult)
                        funcResponse.put("name", item.toolCallName ?: "tool")
                        funcResponse.put("response", responsePayload)

                        val part = JSONObject()
                        part.put("functionResponse", funcResponse)
                        partsArray.put(part)
                    }
                    item.toolCallName != null -> {
                        contentObj.put("role", "model")
                        val funcCall = JSONObject()
                        funcCall.put("name", item.toolCallName)
                        val argsObj = JSONObject()
                        item.toolCallArgs?.forEach { (k, v) -> argsObj.put(k, v) }
                        funcCall.put("args", argsObj)

                        val part = JSONObject()
                        part.put("functionCall", funcCall)
                        partsArray.put(part)
                    }
                    else -> {
                        val role = if (item.role == "model" || item.role == "assistant") "model" else "user"
                        contentObj.put("role", role)
                        val part = JSONObject()
                        part.put("text", item.text.orEmpty())
                        partsArray.put(part)
                    }
                }
                contentObj.put("parts", partsArray)
                contentsArray.put(contentObj)
            }

            val requestBodyJson = JSONObject().apply {
                put("contents", contentsArray)
                put("tools", getGeminiToolsDeclaration())
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().put(JSONObject().apply {
                        put("text", systemText)
                    }))
                })
            }

            val request = Request.Builder()
                .url(url)
                .post(requestBodyJson.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseString = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                val errObj = try { JSONObject(responseString).optJSONObject("error") } catch (e: Exception) { null }
                val errMsg = errObj?.optString("message") ?: "HTTP ${response.code}: $responseString"
                return AiResponse.Error("Gemini API error: $errMsg")
            }

            val json = JSONObject(responseString)
            val candidates = json.optJSONArray("candidates")
            if (candidates == null || candidates.length() == 0) {
                return AiResponse.Error("No response candidates returned by Gemini.")
            }

            val candidate = candidates.getJSONObject(0)
            val content = candidate.optJSONObject("content")
            val parts = content?.optJSONArray("parts")

            if (parts == null || parts.length() == 0) {
                return AiResponse.Text("Okay, done.")
            }

            var textContent: String? = null
            var toolCallInfo: AiResponse.ToolCall? = null

            for (i in 0 until parts.length()) {
                val part = parts.getJSONObject(i)
                if (part.has("text") && !part.optBoolean("thought", false)) {
                    val t = part.getString("text")
                    textContent = if (textContent == null) t else "$textContent\n$t"
                }
                if (part.has("functionCall")) {
                    val fc = part.getJSONObject("functionCall")
                    val fnName = fc.getString("name")
                    val argsObj = fc.optJSONObject("args") ?: JSONObject()
                    val argsMap = mutableMapOf<String, Any>()
                    val keys = argsObj.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        argsMap[key] = argsObj.get(key)
                    }
                    toolCallInfo = AiResponse.ToolCall(
                        callId = "call_${System.currentTimeMillis()}",
                        toolName = fnName,
                        arguments = argsMap,
                        assistantText = textContent
                    )
                }
            }

            return toolCallInfo ?: AiResponse.Text(textContent.orEmpty().trim())
        } catch (e: Exception) {
            Log.e(TAG, "Gemini call failed", e)
            AiResponse.Error("Network error: ${e.localizedMessage ?: e.message}")
        }
    }

    private fun sendAnthropicRequest(
        apiKey: String,
        modelName: String,
        history: List<ChatHistoryItem>,
        language: String,
        userRules: List<String>
    ): AiResponse {
        return try {
            val baseText = if (language.startsWith("hi")) SYSTEM_PROMPT_HI else SYSTEM_PROMPT_EN
            val rulesSuffix = if (userRules.isNotEmpty()) {
                "\n\nयूज़र के नियम (इन्हें सबसे ऊपर मानो):\n- " + userRules.joinToString("\n- ")
            } else ""
            val systemText = baseText + rulesSuffix

            val cleanModel = modelName.trim().ifBlank { DEFAULT_CLAUDE_MODEL }
            val messagesArray = JSONArray()

            for (item in history) {
                val msgObj = JSONObject()
                when {
                    item.toolResult != null -> {
                        msgObj.put("role", "user")
                        val contentArr = JSONArray().put(JSONObject().apply {
                            put("type", "tool_result")
                            put("tool_use_id", item.toolCallId ?: "call_1")
                            put("content", item.toolResult)
                        })
                        msgObj.put("content", contentArr)
                    }
                    item.toolCallName != null -> {
                        msgObj.put("role", "assistant")
                        val contentArr = JSONArray().put(JSONObject().apply {
                            put("type", "tool_use")
                            put("id", item.toolCallId ?: "call_1")
                            put("name", item.toolCallName)
                            val inputObj = JSONObject()
                            item.toolCallArgs?.forEach { (k, v) -> inputObj.put(k, v) }
                            put("input", inputObj)
                        })
                        msgObj.put("content", contentArr)
                    }
                    else -> {
                        val role = if (item.role == "model" || item.role == "assistant") "assistant" else "user"
                        msgObj.put("role", role)
                        msgObj.put("content", item.text.orEmpty())
                    }
                }
                messagesArray.put(msgObj)
            }

            val bodyJson = JSONObject().apply {
                put("model", cleanModel)
                put("max_tokens", 1024)
                put("system", systemText)
                put("tools", getAnthropicTools())
                put("messages", messagesArray)
            }

            val request = Request.Builder()
                .url("https://api.anthropic.com/v1/messages")
                .header("content-type", "application/json")
                .header("x-api-key", apiKey)
                .header("anthropic-version", "2023-06-01")
                .post(bodyJson.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseString = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                val errObj = try { JSONObject(responseString).optJSONObject("error") } catch (e: Exception) { null }
                val errMsg = errObj?.optString("message") ?: "HTTP ${response.code}: $responseString"
                return AiResponse.Error("Anthropic API error: $errMsg")
            }

            val json = JSONObject(responseString)
            val contentArr = json.optJSONArray("content") ?: JSONArray()

            var textContent = ""
            var toolCallInfo: AiResponse.ToolCall? = null

            for (i in 0 until contentArr.length()) {
                val block = contentArr.getJSONObject(i)
                val type = block.optString("type")
                if (type == "text") {
                    textContent += block.optString("text") + " "
                } else if (type == "tool_use") {
                    val toolId = block.getString("id")
                    val toolName = block.getString("name")
                    val inputObj = block.optJSONObject("input") ?: JSONObject()
                    val argsMap = mutableMapOf<String, Any>()
                    val keys = inputObj.keys()
                    while (keys.hasNext()) {
                        val k = keys.next()
                        argsMap[k] = inputObj.get(k)
                    }
                    toolCallInfo = AiResponse.ToolCall(
                        callId = toolId,
                        toolName = toolName,
                        arguments = argsMap,
                        assistantText = textContent.trim().ifEmpty { null }
                    )
                }
            }

            return toolCallInfo ?: AiResponse.Text(textContent.trim())
        } catch (e: Exception) {
            Log.e(TAG, "Anthropic call failed", e)
            AiResponse.Error("Network error: ${e.localizedMessage ?: e.message}")
        }
    }

    private fun getGeminiToolsDeclaration(): JSONArray {
        val toolsArray = JSONArray()
        val funcDecls = JSONArray()

        fun addTool(name: String, desc: String, props: Map<String, Pair<String, String>>, required: List<String>) {
            val toolObj = JSONObject()
            toolObj.put("name", name)
            toolObj.put("description", desc)

            val paramsObj = JSONObject()
            paramsObj.put("type", "OBJECT")
            val propsObj = JSONObject()
            for ((key, pair) in props) {
                val propJson = JSONObject()
                propJson.put("type", pair.first)
                propJson.put("description", pair.second)
                propsObj.put(key, propJson)
            }
            paramsObj.put("properties", propsObj)
            val reqArray = JSONArray()
            required.forEach { reqArray.put(it) }
            paramsObj.put("required", reqArray)

            toolObj.put("parameters", paramsObj)
            funcDecls.put(toolObj)
        }

        addTool("call_phone", "Call a phone number directly",
            mapOf("number" to Pair("STRING", "Phone number to dial")), listOf("number"))

        addTool("send_sms", "Send an SMS message to a phone number",
            mapOf("number" to Pair("STRING", "Recipient phone number"), "text" to Pair("STRING", "SMS body text")),
            listOf("number", "text"))

        addTool("find_contact", "Search phone contacts by name to get phone number",
            mapOf("name" to Pair("STRING", "Name or partial name of contact")), listOf("name"))

        addTool("open_app", "Open an installed Android app by its name (e.g. YouTube, WhatsApp, Settings)",
            mapOf("name" to Pair("STRING", "App name")), listOf("name"))

        addTool("open_url", "Open a web URL in browser",
            mapOf("url" to Pair("STRING", "Web address URL")), listOf("url"))

        addTool("set_alarm", "Set an alarm clock",
            mapOf("hour" to Pair("INTEGER", "Hour in 24-hour clock (0-23)"), "minute" to Pair("INTEGER", "Minute (0-59)"), "label" to Pair("STRING", "Alarm description")),
            listOf("hour", "minute"))

        addTool("set_timer", "Start a countdown timer",
            mapOf("seconds" to Pair("INTEGER", "Timer duration in seconds")), listOf("seconds"))

        addTool("flashlight", "Turn device camera flashlight torch on or off",
            mapOf("on" to Pair("BOOLEAN", "True to turn on, false to turn off")), listOf("on"))

        addTool("set_volume", "Set media volume percentage (0 to 100)",
            mapOf("percent" to Pair("INTEGER", "Volume percentage from 0 to 100")), listOf("percent"))

        addTool("battery", "Check current battery percentage and charging status",
            emptyMap(), emptyList())

        addTool("open_settings", "Open Android system settings screen (wifi, bluetooth, accessibility, notification_access, or general)",
            mapOf("which" to Pair("STRING", "Settings screen name: wifi, bluetooth, accessibility, notification_access, general")),
            listOf("which"))

        addTool("read_notifications", "Read and list recent status bar notifications with their index numbers",
            emptyMap(), emptyList())

        addTool("reply_notification", "Reply directly to a notification by its index",
            mapOf("index" to Pair("INTEGER", "Notification index number"), "text" to Pair("STRING", "Text message reply")),
            listOf("index", "text"))

        addTool("answer_call", "Answer the currently ringing incoming phone call",
            emptyMap(), emptyList())

        addTool("end_call", "End or hang up the current phone call",
            emptyMap(), emptyList())

        addTool("read_screen", "Read the text and buttons currently visible on the phone screen using accessibility",
            emptyMap(), emptyList())

        addTool("tap_text", "Tap a button or element on the active screen by its visible text label",
            mapOf("text" to Pair("STRING", "Exact or partial text of the button or element")), listOf("text"))

        addTool("type_text", "Type text into the currently focused text field on screen",
            mapOf("text" to Pair("STRING", "Text string to type")), listOf("text"))

        addTool("press_button", "Perform a hardware global system navigation button action: home, back, recents, or notifications",
            mapOf("button" to Pair("STRING", "One of: home, back, recents, notifications")), listOf("button"))

        addTool("scroll", "Scroll the screen up or down",
            mapOf("direction" to Pair("STRING", "Scroll direction: up or down")), listOf("direction"))

        addTool("web_search", "Search the internet for current information: news, weather, prices, facts",
            mapOf("query" to Pair("STRING", "Search query or question")), listOf("query"))

        addTool("get_location", "Get the phone's current GPS location as latitude,longitude",
            emptyMap(), emptyList())

        addTool("save_rule", "Save a permanent rule the user asked for (e.g. always keep answers short, call me Sir)",
            mapOf("rule" to Pair("STRING", "Rule instruction text to save")), listOf("rule"))

        addTool("emergency_sos", "Send emergency SOS SMS with location to saved SOS contacts and call the first one. Use only if user clearly needs emergency help",
            emptyMap(), emptyList())

        addTool("wake_screen", "Wake the phone screen from sleep",
            emptyMap(), emptyList())

        addTool("play_music", "Play a song or music on YouTube Music or Spotify using app intent or search",
            mapOf("song" to Pair("STRING", "Song title or artist"), "platform" to Pair("STRING", "youtube or spotify")),
            listOf("song"))

        addTool("image_generate", "Generate an image, poster, photo, cartoon, illustration, drawing, or logo based on a descriptive prompt",
            mapOf("prompt" to Pair("STRING", "Detailed visual description of what to draw or create"), "style" to Pair("STRING", "Optional style: Poster, Logo, Photo, Cartoon")),
            listOf("prompt"))

        val toolContainer = JSONObject()
        toolContainer.put("functionDeclarations", funcDecls)
        toolsArray.put(toolContainer)
        return toolsArray
    }

    private fun getAnthropicTools(): JSONArray {
        val tools = JSONArray()

        fun add(name: String, desc: String, props: Map<String, String>, req: List<String>) {
            val t = JSONObject()
            t.put("name", name)
            t.put("description", desc)
            val schema = JSONObject()
            schema.put("type", "object")
            val p = JSONObject()
            props.forEach { (k, typ) ->
                p.put(k, JSONObject().put("type", typ))
            }
            schema.put("properties", p)
            val r = JSONArray()
            req.forEach { r.put(it) }
            schema.put("required", r)
            t.put("input_schema", schema)
            tools.put(t)
        }

        add("call_phone", "Call a phone number", mapOf("number" to "string"), listOf("number"))
        add("send_sms", "Send an SMS", mapOf("number" to "string", "text" to "string"), listOf("number", "text"))
        add("find_contact", "Find contact phone number by name", mapOf("name" to "string"), listOf("name"))
        add("open_app", "Open an installed app by name", mapOf("name" to "string"), listOf("name"))
        add("open_url", "Open a web URL", mapOf("url" to "string"), listOf("url"))
        add("set_alarm", "Set an alarm, 24 hour clock", mapOf("hour" to "number", "minute" to "number", "label" to "string"), listOf("hour", "minute"))
        add("set_timer", "Start a countdown timer", mapOf("seconds" to "number"), listOf("seconds"))
        add("flashlight", "Turn flashlight on or off", mapOf("on" to "boolean"), listOf("on"))
        add("set_volume", "Set media volume percentage (0 to 100)", mapOf("percent" to "number"), listOf("percent"))
        add("battery", "Get battery percentage and status", emptyMap(), emptyList())
        add("open_settings", "Open settings: wifi, bluetooth, accessibility, notification_access, or general", mapOf("which" to "string"), listOf("which"))
        add("read_notifications", "List recent notifications with index numbers", emptyMap(), emptyList())
        add("reply_notification", "Reply to notification by index", mapOf("index" to "number", "text" to "string"), listOf("index", "text"))
        add("answer_call", "Answer the ringing call", emptyMap(), emptyList())
        add("end_call", "End the current call", emptyMap(), emptyList())
        add("read_screen", "Read text visible on current screen", emptyMap(), emptyList())
        add("tap_text", "Tap button or item on screen by text", mapOf("text" to "string"), listOf("text"))
        add("type_text", "Type text into focused input field", mapOf("text" to "string"), listOf("text"))
        add("press_button", "Press home, back, recents, or notifications", mapOf("button" to "string"), listOf("button"))
        add("scroll", "Scroll screen up or down", mapOf("direction" to "string"), listOf("direction"))
        add("web_search", "Search the internet for current information: news, weather, prices, facts", mapOf("query" to "string"), listOf("query"))
        add("get_location", "Get the phone current GPS location as latitude,longitude", emptyMap(), emptyList())
        add("save_rule", "Save a permanent rule the user asked for", mapOf("rule" to "string"), listOf("rule"))
        add("emergency_sos", "Send emergency SOS SMS with location to saved SOS contacts and call the first one", emptyMap(), emptyList())
        add("wake_screen", "Wake the phone screen from sleep", emptyMap(), emptyList())
        add("play_music", "Play a song or music on YouTube Music or Spotify using app intent or search", mapOf("song" to "string", "platform" to "string"), listOf("song"))
        add("image_generate", "Generate an image, poster, photo, cartoon, illustration, drawing, or logo based on a descriptive prompt", mapOf("prompt" to "string", "style" to "string"), listOf("prompt"))

        return tools
    }

    private fun sendOpenAiRequest(
        apiKey: String,
        modelName: String,
        history: List<ChatHistoryItem>,
        language: String,
        userRules: List<String>
    ): AiResponse {
        return try {
            val baseText = if (language.startsWith("hi")) SYSTEM_PROMPT_HI else SYSTEM_PROMPT_EN
            val rulesSuffix = if (userRules.isNotEmpty()) {
                "\n\nयूज़र के नियम (इन्हें सबसे ऊपर मानो):\n- " + userRules.joinToString("\n- ")
            } else ""
            val systemText = baseText + rulesSuffix

            val cleanModel = modelName.trim().ifBlank { "gpt-4o-mini" }
            val url = "https://api.openai.com/v1/chat/completions"

            val messagesArray = JSONArray()

            // System message
            val sysObj = JSONObject()
            sysObj.put("role", "system")
            sysObj.put("content", systemText)
            messagesArray.put(sysObj)

            for (item in history) {
                val msgObj = JSONObject()
                when {
                    item.toolResult != null -> {
                        msgObj.put("role", "tool")
                        msgObj.put("tool_call_id", item.toolCallId ?: "call_0")
                        msgObj.put("content", item.toolResult)
                    }
                    item.toolCallName != null -> {
                        msgObj.put("role", "assistant")
                        msgObj.put("content", item.text ?: "")
                        val toolCalls = JSONArray()
                        val tc = JSONObject()
                        tc.put("id", item.toolCallId ?: "call_0")
                        tc.put("type", "function")
                        val func = JSONObject()
                        func.put("name", item.toolCallName)
                        func.put("arguments", JSONObject(item.toolCallArgs ?: emptyMap<String, Any>()).toString())
                        tc.put("function", func)
                        toolCalls.put(tc)
                        msgObj.put("tool_calls", toolCalls)
                    }
                    else -> {
                        val role = if (item.role == "model" || item.role == "assistant") "assistant" else "user"
                        msgObj.put("role", role)
                        msgObj.put("content", item.text ?: "")
                    }
                }
                messagesArray.put(msgObj)
            }

            val requestBodyJson = JSONObject()
            requestBodyJson.put("model", cleanModel)
            requestBodyJson.put("messages", messagesArray)
            requestBodyJson.put("tools", getOpenAiTools())
            requestBodyJson.put("tool_choice", "auto")

            val body = requestBodyJson.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $apiKey")
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            val raw = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                Log.e(TAG, "OpenAI error ${response.code}: $raw")
                val realError = try {
                    val jsonErr = JSONObject(raw)
                    jsonErr.optJSONObject("error")?.optString("message")?.takeIf { it.isNotBlank() }
                        ?: jsonErr.optString("error").takeIf { it.isNotBlank() }
                } catch (_: Exception) { null } ?: raw.ifBlank { "HTTP ${response.code}" }

                return AiResponse.Error("OpenAI API error (${response.code}): $realError")
            }

            val json = JSONObject(raw)
            if (json.has("error")) {
                val errObj = json.optJSONObject("error")
                val errMsg = errObj?.optString("message") ?: json.getString("error")
                return AiResponse.Error("OpenAI API error: $errMsg")
            }

            val choices = json.optJSONArray("choices")
            if (choices == null || choices.length() == 0) {
                return AiResponse.Error("OpenAI returned no response choices.")
            }

            val firstChoice = choices.getJSONObject(0)
            val message = firstChoice.getJSONObject("message")

            val toolCalls = message.optJSONArray("tool_calls")
            if (toolCalls != null && toolCalls.length() > 0) {
                val firstToolCall = toolCalls.getJSONObject(0)
                val callId = firstToolCall.getString("id")
                val functionObj = firstToolCall.getJSONObject("function")
                val toolName = functionObj.getString("name")
                val argsRaw = functionObj.getString("arguments")

                val argsMap = mutableMapOf<String, Any>()
                try {
                    val argsJson = JSONObject(argsRaw)
                    val keys = argsJson.keys()
                    while (keys.hasNext()) {
                        val k = keys.next()
                        argsMap[k] = argsJson.get(k)
                    }
                } catch (_: Exception) {}

                val rawAssistantText = message.optString("content", "")
                val assistantText = rawAssistantText.takeIf { it.isNotBlank() }
                return AiResponse.ToolCall(
                    callId = callId,
                    toolName = toolName,
                    arguments = argsMap,
                    assistantText = assistantText
                )
            }

            val replyText = message.optString("content", "")
            AiResponse.Text(replyText)
        } catch (e: Exception) {
            Log.e(TAG, "OpenAI request failed", e)
            AiResponse.Error("OpenAI error: ${e.localizedMessage ?: e.message}")
        }
    }

    private fun getOpenAiTools(): JSONArray {
        val anthropicTools = getAnthropicTools()
        val openAiTools = JSONArray()
        for (i in 0 until anthropicTools.length()) {
            val t = anthropicTools.getJSONObject(i)
            val toolObj = JSONObject()
            toolObj.put("type", "function")
            val funcObj = JSONObject()
            funcObj.put("name", t.getString("name"))
            funcObj.put("description", t.getString("description"))
            funcObj.put("parameters", t.getJSONObject("input_schema"))
            toolObj.put("function", funcObj)
            openAiTools.put(toolObj)
        }
        return openAiTools
    }

    suspend fun generateImage(
        provider: String = "openai",
        apiKey: String,
        modelName: String = "",
        prompt: String,
        style: String = ""
    ): GeneratedImageResult = withContext(Dispatchers.IO) {
        val cleanPrompt = if (style.isNotBlank()) "$prompt. Style: $style." else prompt
        if (apiKey.isBlank()) {
            val provName = if (provider.equals("gemini", ignoreCase = true)) "Gemini" else "OpenAI"
            return@withContext GeneratedImageResult(error = "Please configure your $provName API key in Settings first.")
        }

        if (provider.equals("gemini", ignoreCase = true)) {
            // Gemini Imagen API
            try {
                val cleanModel = modelName.trim().ifBlank { "imagen-3.0-generate-002" }
                val url = "https://generativelanguage.googleapis.com/v1beta/models/$cleanModel:predict?key=$apiKey"

                val instances = JSONArray().apply {
                    put(JSONObject().apply {
                        put("prompt", cleanPrompt)
                    })
                }
                val parameters = JSONObject().apply {
                    put("sampleCount", 1)
                    put("aspectRatio", "1:1")
                }
                val reqBody = JSONObject().apply {
                    put("instances", instances)
                    put("parameters", parameters)
                }

                val body = reqBody.toString().toRequestBody("application/json".toMediaType())
                val request = Request.Builder()
                    .url(url)
                    .post(body)
                    .build()

                val response = client.newCall(request).execute()
                val raw = response.body?.string().orEmpty()

                if (!response.isSuccessful) {
                    val realMsg = try {
                        val jsonErr = JSONObject(raw)
                        jsonErr.optJSONObject("error")?.optString("message") ?: raw
                    } catch (_: Exception) { raw.ifBlank { "HTTP ${response.code}" } }
                    return@withContext GeneratedImageResult(error = "Gemini Imagen error (${response.code}): $realMsg")
                }

                val json = JSONObject(raw)
                val predictions = json.optJSONArray("predictions")
                if (predictions != null && predictions.length() > 0) {
                    val first = predictions.getJSONObject(0)
                    val b64 = first.optString("bytesBase64Encoded")
                    if (b64.isNotBlank()) {
                        return@withContext GeneratedImageResult(base64Data = b64)
                    }
                }
                return@withContext GeneratedImageResult(error = "Gemini returned no image data: $raw")
            } catch (e: Exception) {
                Log.e(TAG, "Gemini image generation failed", e)
                return@withContext GeneratedImageResult(error = "Gemini image error: ${e.localizedMessage ?: e.message}")
            }
        } else {
            // OpenAI Images API
            try {
                val cleanModel = modelName.trim().ifBlank { "dall-e-3" }
                val url = "https://api.openai.com/v1/images/generations"

                val reqJson = JSONObject().apply {
                    put("model", cleanModel)
                    put("prompt", cleanPrompt)
                    put("n", 1)
                    put("size", "1024x1024")
                    put("response_format", "b64_json")
                }

                val body = reqJson.toString().toRequestBody("application/json".toMediaType())
                val request = Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer $apiKey")
                    .post(body)
                    .build()

                val response = client.newCall(request).execute()
                val raw = response.body?.string().orEmpty()

                if (!response.isSuccessful) {
                    val realMsg = try {
                        val jsonErr = JSONObject(raw)
                        jsonErr.optJSONObject("error")?.optString("message") ?: raw
                    } catch (_: Exception) { raw.ifBlank { "HTTP ${response.code}" } }
                    return@withContext GeneratedImageResult(error = "OpenAI Images error (${response.code}): $realMsg")
                }

                val json = JSONObject(raw)
                val dataArr = json.optJSONArray("data")
                if (dataArr != null && dataArr.length() > 0) {
                    val first = dataArr.getJSONObject(0)
                    val b64 = first.optString("b64_json")
                    val imgUrl = first.optString("url")
                    val revised = first.optString("revised_prompt")
                    return@withContext GeneratedImageResult(
                        base64Data = b64.ifBlank { null },
                        imageUrl = imgUrl.ifBlank { null },
                        revisedPrompt = revised.ifBlank { null }
                    )
                }
                return@withContext GeneratedImageResult(error = "OpenAI returned no image output: $raw")
            } catch (e: Exception) {
                Log.e(TAG, "OpenAI image generation failed", e)
                return@withContext GeneratedImageResult(error = "OpenAI image error: ${e.localizedMessage ?: e.message}")
            }
        }
    }

    suspend fun analyzeImageWithVision(
        provider: String,
        apiKey: String,
        modelName: String = "",
        imageBase64: String,
        prompt: String,
        language: String = "hi",
        userRules: List<String> = emptyList()
    ): AiResponse = withContext(Dispatchers.IO) {
        val cleanKey = apiKey.trim()
        val isClaude = provider.equals("claude", ignoreCase = true) || cleanKey.startsWith("sk-ant-")
        val isOpenAi = provider.equals("openai", ignoreCase = true) || (cleanKey.startsWith("sk-") && !isClaude)
        val isGemini = !isClaude && !isOpenAi

        if (cleanKey.isBlank() || cleanKey == "MY_GEMINI_API_KEY") {
            val provName = when {
                isOpenAi -> "OpenAI"
                isClaude -> "Claude"
                else -> "Gemini"
            }
            return@withContext AiResponse.Error("Please configure your $provName API key in Settings first.")
        }

        val basePrompt = if (language.startsWith("hi")) {
            "तुम ममता AI का Neural Lens विज़न सिस्टम हो। तस्वीरों को देखकर सटीक, सरल और उपयोगी जानकारी दो।\n\nCRITICAL PRIVACY RULE: Do not identify real people from their faces. If the user asks who a person in a photo is, or asks to name any individual in the image, you must politely state that Mamta does not identify people from faces, and describe the scene, clothing, or general activity instead."
        } else {
            "You are Mamta AI's Neural Lens vision system. Analyze images accurately, clearly, and helpfully.\n\nCRITICAL PRIVACY RULE: Do not identify real people from their faces. If the user asks who a person in a photo is, or asks to name any individual in the image, you must politely state that Mamta does not identify people from faces, and describe the scene, clothing, or general activity instead."
        }
        val rulesSuffix = if (userRules.isNotEmpty()) {
            "\n\nयूज़र के नियम (इन्हें सबसे ऊपर मानो):\n- " + userRules.joinToString("\n- ")
        } else ""
        val systemText = basePrompt + rulesSuffix

        if (isGemini) {
            try {
                val cleanModel = modelName.removePrefix("models/").ifBlank { DEFAULT_GEMINI_MODEL }
                val url = "https://generativelanguage.googleapis.com/v1beta/models/$cleanModel:generateContent?key=$cleanKey"

                val partImage = JSONObject().apply {
                    put("inline_data", JSONObject().apply {
                        put("mime_type", "image/jpeg")
                        put("data", imageBase64)
                    })
                }
                val partText = JSONObject().apply {
                    put("text", prompt)
                }

                val contentObj = JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().put(partImage).put(partText))
                }

                val reqJson = JSONObject().apply {
                    put("contents", JSONArray().put(contentObj))
                    put("systemInstruction", JSONObject().apply {
                        put("parts", JSONArray().put(JSONObject().apply {
                            put("text", systemText)
                        }))
                    })
                }

                val body = reqJson.toString().toRequestBody("application/json".toMediaType())
                val request = Request.Builder().url(url).post(body).build()
                val response = client.newCall(request).execute()
                val raw = response.body?.string().orEmpty()

                if (!response.isSuccessful) {
                    val realMsg = try {
                        val jsonErr = JSONObject(raw)
                        jsonErr.optJSONObject("error")?.optString("message") ?: raw
                    } catch (_: Exception) { raw.ifBlank { "HTTP ${response.code}" } }
                    return@withContext AiResponse.Error("Gemini error (${response.code}): $realMsg")
                }

                val json = JSONObject(raw)
                val candidates = json.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val candidate = candidates.getJSONObject(0)
                    val content = candidate.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        val text = parts.getJSONObject(0).optString("text")
                        if (text.isNotBlank()) {
                            return@withContext AiResponse.Text(text.trim())
                        }
                    }
                }
                return@withContext AiResponse.Error("Gemini returned no response text: $raw")
            } catch (e: Exception) {
                Log.e(TAG, "Gemini vision failed", e)
                return@withContext AiResponse.Error("Gemini error: ${e.localizedMessage ?: e.message}")
            }
        } else if (isClaude) {
            try {
                val cleanModel = modelName.trim().ifBlank { DEFAULT_CLAUDE_MODEL }
                val url = "https://api.anthropic.com/v1/messages"

                val imageContent = JSONObject().apply {
                    put("type", "image")
                    put("source", JSONObject().apply {
                        put("type", "base64")
                        put("media_type", "image/jpeg")
                        put("data", imageBase64)
                    })
                }
                val textContent = JSONObject().apply {
                    put("type", "text")
                    put("text", prompt)
                }

                val msgObj = JSONObject().apply {
                    put("role", "user")
                    put("content", JSONArray().put(imageContent).put(textContent))
                }

                val reqJson = JSONObject().apply {
                    put("model", cleanModel)
                    put("max_tokens", 1024)
                    put("system", systemText)
                    put("messages", JSONArray().put(msgObj))
                }

                val body = reqJson.toString().toRequestBody("application/json".toMediaType())
                val request = Request.Builder()
                    .url(url)
                    .header("content-type", "application/json")
                    .header("x-api-key", cleanKey)
                    .header("anthropic-version", "2023-06-01")
                    .post(body)
                    .build()

                val response = client.newCall(request).execute()
                val raw = response.body?.string().orEmpty()

                if (!response.isSuccessful) {
                    val realMsg = try {
                        val jsonErr = JSONObject(raw)
                        jsonErr.optJSONObject("error")?.optString("message") ?: raw
                    } catch (_: Exception) { raw.ifBlank { "HTTP ${response.code}" } }
                    return@withContext AiResponse.Error("Claude error (${response.code}): $realMsg")
                }

                val json = JSONObject(raw)
                val contentArr = json.optJSONArray("content")
                if (contentArr != null && contentArr.length() > 0) {
                    val textBuilder = StringBuilder()
                    for (i in 0 until contentArr.length()) {
                        val block = contentArr.getJSONObject(i)
                        if (block.optString("type") == "text") {
                            textBuilder.append(block.optString("text")).append(" ")
                        }
                    }
                    val res = textBuilder.toString().trim()
                    if (res.isNotBlank()) {
                        return@withContext AiResponse.Text(res)
                    }
                }
                return@withContext AiResponse.Error("Claude returned no text content: $raw")
            } catch (e: Exception) {
                Log.e(TAG, "Claude vision failed", e)
                return@withContext AiResponse.Error("Claude error: ${e.localizedMessage ?: e.message}")
            }
        } else {
            // OpenAI Vision
            try {
                val cleanModel = modelName.trim().ifBlank { "gpt-4o-mini" }
                val url = "https://api.openai.com/v1/chat/completions"

                val systemMsg = JSONObject().apply {
                    put("role", "system")
                    put("content", systemText)
                }

                val textPart = JSONObject().apply {
                    put("type", "text")
                    put("text", prompt)
                }
                val imagePart = JSONObject().apply {
                    put("type", "image_url")
                    put("image_url", JSONObject().apply {
                        put("url", "data:image/jpeg;base64,$imageBase64")
                    })
                }

                val userMsg = JSONObject().apply {
                    put("role", "user")
                    put("content", JSONArray().put(textPart).put(imagePart))
                }

                val reqJson = JSONObject().apply {
                    put("model", cleanModel)
                    put("messages", JSONArray().put(systemMsg).put(userMsg))
                }

                val body = reqJson.toString().toRequestBody("application/json".toMediaType())
                val request = Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer $cleanKey")
                    .post(body)
                    .build()

                val response = client.newCall(request).execute()
                val raw = response.body?.string().orEmpty()

                if (!response.isSuccessful) {
                    val realMsg = try {
                        val jsonErr = JSONObject(raw)
                        jsonErr.optJSONObject("error")?.optString("message") ?: raw
                    } catch (_: Exception) { raw.ifBlank { "HTTP ${response.code}" } }
                    return@withContext AiResponse.Error("OpenAI error (${response.code}): $realMsg")
                }

                val json = JSONObject(raw)
                val choices = json.optJSONArray("choices")
                if (choices != null && choices.length() > 0) {
                    val first = choices.getJSONObject(0)
                    val msg = first.optJSONObject("message")
                    val content = msg?.optString("content")
                    if (!content.isNullOrBlank()) {
                        return@withContext AiResponse.Text(content.trim())
                    }
                }
                return@withContext AiResponse.Error("OpenAI returned no response content: $raw")
            } catch (e: Exception) {
                Log.e(TAG, "OpenAI vision failed", e)
                return@withContext AiResponse.Error("OpenAI error: ${e.localizedMessage ?: e.message}")
            }
        }
    }
}
