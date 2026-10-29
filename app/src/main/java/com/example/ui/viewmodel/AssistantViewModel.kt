package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.speech.tts.TextToSpeech
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.SecurePrefs
import com.example.data.model.AppConfig
import com.example.data.model.ChatMessage
import com.example.data.model.DangerActionRequest
import com.example.data.model.MessageRole
import com.example.data.model.ToolCallInfo
import com.example.data.model.ToolStatus
import com.example.network.AiClient
import com.example.network.AiResponse
import com.example.network.ChatHistoryItem
import com.example.service.EventService
import com.example.service.NotifService
import com.example.service.PhoneService
import com.example.tools.DeviceToolController
import com.example.tools.ScreenWakeHelper
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.util.Locale

data class ServiceStatus(
    val accessibilityActive: Boolean,
    val notificationActive: Boolean,
    val apiKeyConfigured: Boolean,
    val activeProvider: String
)

class AssistantViewModel(application: Application) : AndroidViewModel(application) {

    private val securePrefs: SharedPreferences =
        SecurePrefs.getEncryptedPreferences(application)
    private val prefs: SharedPreferences =
        application.getSharedPreferences("mamta_assistant_prefs", Context.MODE_PRIVATE)
    private val rulesPrefs: SharedPreferences =
        application.getSharedPreferences("mamta_rules_prefs", Context.MODE_PRIVATE)

    private val aiClient = AiClient()
    val toolController = DeviceToolController(application)
    val journalDb by lazy { com.example.data.journal.JournalDatabaseHelper(application) }
    val sevaRepository by lazy { com.example.data.sevakendra.SevaRepository(application) }

    private val _pendingVoiceCustomerWork = MutableStateFlow<com.example.data.sevakendra.SevaCustomerWork?>(null)
    val pendingVoiceCustomerWork: StateFlow<com.example.data.sevakendra.SevaCustomerWork?> = _pendingVoiceCustomerWork.asStateFlow()

    private val _requestMicTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val requestMicTrigger: SharedFlow<Unit> = _requestMicTrigger.asSharedFlow()

    fun triggerVoiceInput() {
        _requestMicTrigger.tryEmit(Unit)
    }

    fun confirmAddSevaWork() {
        val work = _pendingVoiceCustomerWork.value ?: return
        _pendingVoiceCustomerWork.value = null
        viewModelScope.launch {
            sevaRepository.insertWork(work)
            val isHindi = _currentLanguage.value.startsWith("hi")
            val msgText = if (isHindi) {
                "✅ ग्राहक '${work.customerName}' का काम '${work.serviceName}' (₹${"%.0f".format(work.amount)}) सेवा केंद्र में दर्ज कर लिया गया है।"
            } else {
                "✅ Customer '${work.customerName}' work '${work.serviceName}' (₹${"%.0f".format(work.amount)}) added to Seva Kendra."
            }
            val assistantMsg = ChatMessage(role = MessageRole.ASSISTANT, content = msgText)
            _messages.value = _messages.value + assistantMsg
            speakText(msgText)
        }
    }

    fun dismissPendingSevaWork() {
        _pendingVoiceCustomerWork.value = null
    }

    suspend fun askAiOneShot(prompt: String): String {
        val cfg = _config.value
        val key = when (cfg.provider.lowercase()) {
            "claude" -> cfg.claudeApiKey
            "openai" -> cfg.openaiApiKey
            else -> cfg.geminiApiKey
        }
        val model = when (cfg.provider.lowercase()) {
            "claude" -> cfg.claudeModel
            "openai" -> cfg.openaiModel
            else -> cfg.geminiModel
        }
        val response = aiClient.sendPrompt(
            provider = cfg.provider,
            apiKey = key,
            modelName = model,
            history = listOf(ChatHistoryItem(role = "user", text = prompt)),
            language = _currentLanguage.value,
            userRules = _rules.value
        )
        return when (response) {
            is AiResponse.Text -> response.content
            is AiResponse.ToolCall -> response.assistantText ?: "Action completed: ${response.toolName}"
            is AiResponse.Error -> response.message
        }
    }

    fun speakTextOutLoud(text: String) {
        speakText(text)
    }

    private val _config = MutableStateFlow(loadConfig())
    val config: StateFlow<AppConfig> = _config.asStateFlow()

    private val _rules = MutableStateFlow<List<String>>(loadRules())
    val rules: StateFlow<List<String>> = _rules.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _currentLanguage = MutableStateFlow(prefs.getString("app_lang", "hi-IN") ?: "hi-IN")
    val currentLanguage: StateFlow<String> = _currentLanguage.asStateFlow()

    private val _pendingDangerAction = MutableStateFlow<DangerActionRequest?>(null)
    val pendingDangerAction: StateFlow<DangerActionRequest?> = _pendingDangerAction.asStateFlow()

    private val _serviceStatus = MutableStateFlow(
        ServiceStatus(
            accessibilityActive = false,
            notificationActive = false,
            apiKeyConfigured = false,
            activeProvider = "gemini"
        )
    )
    val serviceStatus: StateFlow<ServiceStatus> = _serviceStatus.asStateFlow()

    private var textToSpeech: TextToSpeech? = null
    private var isTtsReady = false

    private val conversationHistory = mutableListOf<ChatHistoryItem>()

    init {
        initTts(application)
        refreshServiceStatus()

        // Auto start EventService if master switch enabled
        try {
            val evPrefs = application.getSharedPreferences("mamta_events", Context.MODE_PRIVATE)
            if (evPrefs.getBoolean("master", true)) {
                val intent = Intent(application, EventService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    application.startForegroundService(intent)
                } else {
                    application.startService(intent)
                }
            }
        } catch (_: Exception) {}

        // Initialize Seva Kendra defaults and check due reminders
        viewModelScope.launch {
            try {
                sevaRepository.ensureDefaultRates()
                sevaRepository.checkAndNotifyDueToday()
            } catch (_: Exception) {}
        }

        // Mamta greeting
        val isHindi = _currentLanguage.value.startsWith("hi")
        val welcome = if (isHindi) {
            "नमस्ते! मैं ममता हूँ, आपकी AI असिस्टेंट। मैं आपका फ़ोन चला सकती हूँ (कॉल, SMS, ऐप्स, अलार्म, टॉर्च, स्क्रीन, नोटिफ़िकेशन) और ताज़ा खबर, मौसम या भाव भी सर्च कर सकती हूँ।\n\nबोलने के लिए 🎤 दबाएँ या नीचे लिखें!"
        } else {
            "Hello! I am Mamta, your AI phone assistant. I can operate your device (calls, SMS, apps, alarms, flashlight, screen, notifications) and search the web for live news, weather, or prices.\n\nTap 🎤 to speak or type below!"
        }
        _messages.value = listOf(
            ChatMessage(
                role = MessageRole.ASSISTANT,
                content = welcome
            )
        )
    }

    private fun loadRules(): List<String> {
        val raw = rulesPrefs.getString("rules", "[]") ?: "[]"
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { arr.getString(it) }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun addRule(ruleText: String) {
        val clean = ruleText.trim()
        if (clean.isBlank()) return
        val updated = _rules.value + clean
        _rules.value = updated
        saveRules(updated)
    }

    fun deleteRule(index: Int) {
        if (index in _rules.value.indices) {
            val updated = _rules.value.toMutableList().apply { removeAt(index) }
            _rules.value = updated
            saveRules(updated)
        }
    }

    private fun saveRules(list: List<String>) {
        val arr = JSONArray()
        list.forEach { arr.put(it) }
        rulesPrefs.edit().putString("rules", arr.toString()).apply()
    }

    private fun loadConfig(): AppConfig {
        val defaultGeminiKey = try { BuildConfig.GEMINI_API_KEY } catch (e: Throwable) { "" }
        val prov = prefs.getString("cfg_provider", "gemini") ?: "gemini"
        val ck = securePrefs.getString("cfg_claude_key", null) ?: prefs.getString("cfg_claude_key", "") ?: ""
        val cm = prefs.getString("cfg_claude_model", "claude-sonnet-5-5") ?: "claude-sonnet-5-5"
        val gk = securePrefs.getString("cfg_gemini_key", null) ?: prefs.getString("cfg_gemini_key", defaultGeminiKey) ?: defaultGeminiKey
        val gm = prefs.getString("cfg_gemini_model", "gemini-3.8-flash") ?: "gemini-3.8-flash"
        val ok = securePrefs.getString("cfg_openai_key", null) ?: prefs.getString("cfg_openai_key", "") ?: ""
        val om = prefs.getString("cfg_openai_model", "gpt-4o-mini") ?: "gpt-4o-mini"
        val sp = prefs.getString("cfg_search_provider", "tavily") ?: "tavily"
        val sk = securePrefs.getString("cfg_search_key", null) ?: prefs.getString("cfg_search_key", "") ?: ""
        val speak = prefs.getBoolean("cfg_speak", true)
        val imgProv = prefs.getString("cfg_image_provider", "openai") ?: "openai"
        val openAiImgModel = prefs.getString("cfg_openai_image_model", "dall-e-3") ?: "dall-e-3"
        val geminiImgModel = prefs.getString("cfg_gemini_image_model", "imagen-3.0-generate-002") ?: "imagen-3.0-generate-002"

        return AppConfig(
            provider = prov,
            claudeApiKey = ck,
            claudeModel = cm,
            geminiApiKey = gk,
            geminiModel = gm,
            openaiApiKey = ok,
            openaiModel = om,
            searchProvider = sp,
            searchApiKey = sk,
            speakAnswers = speak,
            imageProvider = imgProv,
            openaiImageModel = openAiImgModel,
            geminiImageModel = geminiImgModel
        )
    }

    fun updateConfig(newConfig: AppConfig) {
        _config.value = newConfig
        securePrefs.edit()
            .putString("cfg_claude_key", newConfig.claudeApiKey)
            .putString("cfg_gemini_key", newConfig.geminiApiKey)
            .putString("cfg_openai_key", newConfig.openaiApiKey)
            .putString("cfg_search_key", newConfig.searchApiKey)
            .apply()

        prefs.edit()
            .putString("cfg_provider", newConfig.provider)
            .putString("cfg_claude_model", newConfig.claudeModel)
            .putString("cfg_gemini_model", newConfig.geminiModel)
            .putString("cfg_openai_model", newConfig.openaiModel)
            .putString("cfg_search_provider", newConfig.searchProvider)
            .putBoolean("cfg_speak", newConfig.speakAnswers)
            .putString("cfg_image_provider", newConfig.imageProvider)
            .putString("cfg_openai_image_model", newConfig.openaiImageModel)
            .putString("cfg_gemini_image_model", newConfig.geminiImageModel)
            .apply()

        refreshServiceStatus()
    }

    suspend fun generateImageDirect(
        provider: String,
        prompt: String,
        style: String,
        modelName: String
    ): com.example.network.GeneratedImageResult {
        val cfg = _config.value
        val apiKey = if (provider.equals("gemini", ignoreCase = true)) cfg.geminiApiKey else cfg.openaiApiKey
        return aiClient.generateImage(provider, apiKey, modelName, prompt, style)
    }

    suspend fun analyzeImageVision(
        imageBase64: String,
        prompt: String
    ): com.example.network.AiResponse {
        val cfg = _config.value
        val provider = cfg.provider
        val key = when (provider.lowercase()) {
            "claude" -> cfg.claudeApiKey
            "openai" -> cfg.openaiApiKey
            else -> cfg.geminiApiKey
        }
        val model = when (provider.lowercase()) {
            "claude" -> cfg.claudeModel
            "openai" -> cfg.openaiModel
            else -> cfg.geminiModel
        }
        return aiClient.analyzeImageWithVision(
            provider = provider,
            apiKey = key,
            modelName = model,
            imageBase64 = imageBase64,
            prompt = prompt,
            language = _currentLanguage.value,
            userRules = _rules.value
        )
    }

    private fun initTts(context: Context) {
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isTtsReady = true
                updateTtsLanguage(_currentLanguage.value)
            }
        }
    }

    private fun updateTtsLanguage(langCode: String) {
        textToSpeech?.let { tts ->
            val locale = if (langCode.startsWith("hi")) Locale.forLanguageTag("hi-IN") else Locale.US
            tts.language = locale
        }
    }

    fun refreshServiceStatus() {
        val cfg = _config.value
        val activeKey = when (cfg.provider.lowercase()) {
            "claude" -> cfg.claudeApiKey
            "openai" -> cfg.openaiApiKey
            else -> cfg.geminiApiKey
        }
        val hasKey = activeKey.isNotBlank() && activeKey != "MY_GEMINI_API_KEY"

        _serviceStatus.value = ServiceStatus(
            accessibilityActive = PhoneService.isActive(),
            notificationActive = NotifService.isActive(),
            apiKeyConfigured = hasKey,
            activeProvider = cfg.provider
        )
    }

    fun toggleTts() {
        val updated = _config.value.copy(speakAnswers = !_config.value.speakAnswers)
        updateConfig(updated)
        if (!updated.speakAnswers) {
            textToSpeech?.stop()
        }
    }

    fun toggleLanguage() {
        val newLang = if (_currentLanguage.value.startsWith("hi")) "en-US" else "hi-IN"
        _currentLanguage.value = newLang
        prefs.edit().putString("app_lang", newLang).apply()
        updateTtsLanguage(newLang)
    }

    fun clearConversation() {
        conversationHistory.clear()
        textToSpeech?.stop()
        val isHindi = _currentLanguage.value.startsWith("hi")
        val resetMsg = if (isHindi) "बातचीत रीसेट कर दी गई है।" else "Conversation cleared."
        _messages.value = listOf(
            ChatMessage(role = MessageRole.ASSISTANT, content = resetMsg)
        )
    }

    fun triggerSosManual() {
        viewModelScope.launch {
            val result = toolController.execute("emergency_sos", emptyMap())
            val msg = ChatMessage(
                role = MessageRole.TOOL,
                content = result,
                toolCall = ToolCallInfo(
                    name = "emergency_sos",
                    arguments = emptyMap(),
                    result = result,
                    status = ToolStatus.SUCCESS
                )
            )
            _messages.value = _messages.value + msg
            speakText(result)
        }
    }

    fun sendMessage(userText: String) {
        val query = userText.trim()
        if (query.isBlank() || _isBusy.value) return

        val userMessage = ChatMessage(
            role = MessageRole.USER,
            content = query
        )
        _messages.value = _messages.value + userMessage
        conversationHistory.add(ChatHistoryItem(role = "user", text = query))

        val lower = query.lowercase()

        // Seva Kendra Voice Command 1: "who has pending balance"
        if (lower.contains("who has pending balance") || lower.contains("pending balance") ||
            lower.contains("बैलेंस बाकी") || lower.contains("बकाया किसका") ||
            lower.contains("किसका बकाया") || lower.contains("बकाया बैलेंस") ||
            lower.contains("किसके पास बकाया") || lower.contains("किसका बैलेंस")
        ) {
            viewModelScope.launch {
                val pendingWorks = sevaRepository.getWorksWithPendingBalance()
                val isHindi = _currentLanguage.value.startsWith("hi")
                val replyText = if (pendingWorks.isEmpty()) {
                    if (isHindi) "सेवा केंद्र में किसी भी ग्राहक का कोई बैलेंस बकाया नहीं है। सभी खाते चुकता हैं।"
                    else "No customers with pending balance at Seva Kendra. All dues are cleared!"
                } else {
                    val totalDue = pendingWorks.sumOf { it.balanceDue }
                    val sb = StringBuilder()
                    if (isHindi) {
                        sb.appendLine("📋 सेवा केंद्र बकाया सूची:")
                        pendingWorks.forEachIndexed { idx, w ->
                            sb.appendLine("${idx + 1}. ${w.customerName}: ₹${"%.0f".format(w.balanceDue)} बाकी (${w.serviceName})")
                        }
                        sb.append("कुल बकाया: ₹${"%.0f".format(totalDue)}")
                    } else {
                        sb.appendLine("📋 Seva Kendra Pending Balance List:")
                        pendingWorks.forEachIndexed { idx, w ->
                            sb.appendLine("${idx + 1}. ${w.customerName}: ₹${"%.0f".format(w.balanceDue)} due (${w.serviceName})")
                        }
                        sb.append("Total Pending: ₹${"%.0f".format(totalDue)}")
                    }
                    sb.toString()
                }
                val assistantMsg = ChatMessage(role = MessageRole.ASSISTANT, content = replyText)
                _messages.value = _messages.value + assistantMsg
                speakText(replyText)
            }
            return
        }

        // Seva Kendra Voice Command 2: "Add customer Ramesh, voter ID work, 200 rupees"
        val parsedWork = tryParseAddCustomerCommand(query)
        if (parsedWork != null) {
            _pendingVoiceCustomerWork.value = parsedWork
            val isHindi = _currentLanguage.value.startsWith("hi")
            val promptMsg = if (isHindi) {
                "सेवा केंद्र के लिए नया काम मिला:\n• ग्राहक: ${parsedWork.customerName}\n• काम: ${parsedWork.serviceName}\n• शुल्क: ₹${"%.0f".format(parsedWork.amount)}\n\nजोड़ने के लिए कृपया स्क्रीन पर पुष्टि (Confirm) करें।"
            } else {
                "Found customer entry for Seva Kendra:\n• Customer: ${parsedWork.customerName}\n• Service: ${parsedWork.serviceName}\n• Amount: ₹${"%.0f".format(parsedWork.amount)}\n\nPlease tap Confirm on screen to save."
            }
            val assistantMsg = ChatMessage(role = MessageRole.ASSISTANT, content = promptMsg)
            _messages.value = _messages.value + assistantMsg
            speakText(promptMsg)
            return
        }

        val cfg = _config.value
        val activeKey = when (cfg.provider.lowercase()) {
            "claude" -> cfg.claudeApiKey
            "openai" -> cfg.openaiApiKey
            else -> cfg.geminiApiKey
        }
        val hasValidKey = activeKey.isNotBlank() && activeKey != "MY_GEMINI_API_KEY"

        // Offline fallback check
        val offlineResult = toolController.tryHandleOfflineCommand(query)
        if (offlineResult != null && !hasValidKey) {
            val (_, resultText) = offlineResult
            val toolMessage = ChatMessage(
                role = MessageRole.TOOL,
                content = resultText,
                toolCall = ToolCallInfo(
                    name = "offline_tool",
                    arguments = emptyMap(),
                    result = resultText,
                    status = ToolStatus.SUCCESS
                )
            )
            val assistantResponse = ChatMessage(
                role = MessageRole.ASSISTANT,
                content = resultText
            )
            _messages.value = _messages.value + toolMessage + assistantResponse
            speakText(resultText)
            return
        }

        if (!hasValidKey) {
            val isHindi = _currentLanguage.value.startsWith("hi")
            val providerName = when (cfg.provider.lowercase()) {
                "claude" -> "Claude"
                "openai" -> "OpenAI"
                else -> "Gemini"
            }
            val helpText = if (isHindi) {
                "कृपया ऊपर ⚙️ सेटिंग बटन दबाकर अपनी $providerName API Key डालें।\n\nआप सीधे ऑफलाइन कमांड भी आज़मा सकते हैं: 'टॉर्च चालू', 'बैटरी', 'वॉल्यूम 80', 'अलार्म 7 बजे' आदि।"
            } else {
                "Please configure your $providerName API key using the ⚙️ settings button.\n\nOffline device commands like 'flashlight on', 'battery', or 'volume 80' also work immediately."
            }
            val errorMsg = ChatMessage(role = MessageRole.ASSISTANT, content = helpText)
            _messages.value = _messages.value + errorMsg
            speakText(helpText)
            return
        }

        // Call AI
        executeAiTurn()
    }

    private fun executeAiTurn() {
        viewModelScope.launch {
            _isBusy.value = true
            val lang = _currentLanguage.value
            val cfg = _config.value
            val key = when (cfg.provider.lowercase()) {
                "claude" -> cfg.claudeApiKey
                "openai" -> cfg.openaiApiKey
                else -> cfg.geminiApiKey
            }
            val model = when (cfg.provider.lowercase()) {
                "claude" -> cfg.claudeModel
                "openai" -> cfg.openaiModel
                else -> cfg.geminiModel
            }

            val response = aiClient.sendPrompt(
                provider = cfg.provider,
                apiKey = key,
                modelName = model,
                history = conversationHistory,
                language = lang,
                userRules = _rules.value
            )

            when (response) {
                is AiResponse.Text -> {
                    _isBusy.value = false
                    val assistantMsg = ChatMessage(
                        role = MessageRole.ASSISTANT,
                        content = response.content
                    )
                    _messages.value = _messages.value + assistantMsg
                    conversationHistory.add(ChatHistoryItem(role = "model", text = response.content))
                    speakText(response.content)
                }

                is AiResponse.ToolCall -> {
                    // Check if tool is save_rule
                    if (response.toolName == "save_rule") {
                        _isBusy.value = false
                        val rule = response.arguments["rule"]?.toString() ?: ""
                        if (rule.isNotBlank()) {
                            addRule(rule)
                        }
                        val result = if (rule.isNotBlank()) "नियम सेव हो गया: $rule" else "खाली नियम"
                        val toolMsg = ChatMessage(
                            role = MessageRole.TOOL,
                            content = "⚙️ save_rule",
                            toolCall = ToolCallInfo(
                                id = response.callId,
                                name = response.toolName,
                                arguments = response.arguments,
                                result = result,
                                status = ToolStatus.SUCCESS
                            )
                        )
                        _messages.value = _messages.value + toolMsg
                        completeToolExecution(response.callId, response.toolName, result, ToolStatus.SUCCESS)
                        return@launch
                    }

                    val isDangerous = toolController.isDangerous(response.toolName)
                    val toolCallInfo = ToolCallInfo(
                        id = response.callId,
                        name = response.toolName,
                        arguments = response.arguments,
                        isDangerous = isDangerous,
                        status = if (isDangerous) ToolStatus.WAITING_APPROVAL else ToolStatus.RUNNING
                    )

                    val toolMsg = ChatMessage(
                        role = MessageRole.TOOL,
                        content = "⚙️ ${response.toolName}",
                        toolCall = toolCallInfo
                    )
                    _messages.value = _messages.value + toolMsg

                    if (!response.assistantText.isNullOrBlank()) {
                        val preambleMsg = ChatMessage(
                            role = MessageRole.ASSISTANT,
                            content = response.assistantText
                        )
                        _messages.value = _messages.value + preambleMsg
                    }

                    conversationHistory.add(
                        ChatHistoryItem(
                            role = "model",
                            toolCallName = response.toolName,
                            toolCallArgs = response.arguments,
                            toolCallId = response.callId
                        )
                    )

                    if (response.toolName == "image_generate") {
                        val prompt = response.arguments["prompt"]?.toString().orEmpty()
                        val style = response.arguments["style"]?.toString().orEmpty()
                        val imgProv = cfg.imageProvider
                        val apiKey = if (imgProv.equals("gemini", ignoreCase = true)) cfg.geminiApiKey else cfg.openaiApiKey
                        val model = if (imgProv.equals("gemini", ignoreCase = true)) cfg.geminiImageModel else cfg.openaiImageModel

                        val imgResult = aiClient.generateImage(imgProv, apiKey, model, prompt, style)
                        if (imgResult.error != null) {
                            val errorText = "Image generation failed: ${imgResult.error}"
                            completeToolExecution(response.callId, response.toolName, errorText, ToolStatus.FAILED)
                        } else {
                            var localFilePath: String? = null
                            if (!imgResult.base64Data.isNullOrBlank()) {
                                val bitmap = com.example.tools.ImageStorageHelper.decodeBase64ToBitmap(imgResult.base64Data)
                                if (bitmap != null) {
                                    val file = com.example.tools.ImageStorageHelper.saveToPrivateCache(getApplication(), bitmap)
                                    localFilePath = file.absolutePath
                                }
                            }
                            val successText = "Image generated for '$prompt'."
                            _messages.value = _messages.value.map { msg ->
                                if (msg.toolCall?.id == response.callId) {
                                    msg.copy(
                                        imageBase64 = imgResult.base64Data,
                                        imageFilePath = localFilePath,
                                        toolCall = msg.toolCall.copy(result = successText, status = ToolStatus.SUCCESS)
                                    )
                                } else msg
                            }
                            completeToolExecution(response.callId, response.toolName, successText, ToolStatus.SUCCESS)
                        }
                    } else if (isDangerous) {
                        _isBusy.value = false
                        ScreenWakeHelper.wakeScreen(getApplication())
                        val summary = toolController.formatDangerSummary(response.toolName, response.arguments)
                        _pendingDangerAction.value = DangerActionRequest(
                            actionId = response.callId,
                            toolName = response.toolName,
                            arguments = response.arguments,
                            humanReadableSummary = summary
                        )
                    } else {
                        val result = toolController.execute(
                            response.toolName,
                            response.arguments,
                            cfg.searchProvider,
                            cfg.searchApiKey
                        )
                        completeToolExecution(response.callId, response.toolName, result, ToolStatus.SUCCESS)
                    }
                }

                is AiResponse.Error -> {
                    _isBusy.value = false
                    val errorMsg = ChatMessage(
                        role = MessageRole.ASSISTANT,
                        content = "⚠️ ${response.message}"
                    )
                    _messages.value = _messages.value + errorMsg
                }
            }
        }
    }

    fun confirmDangerAction(approved: Boolean) {
        val pending = _pendingDangerAction.value ?: return
        _pendingDangerAction.value = null
        val cfg = _config.value

        viewModelScope.launch {
            if (approved) {
                val result = toolController.execute(
                    pending.toolName,
                    pending.arguments,
                    cfg.searchProvider,
                    cfg.searchApiKey
                )
                completeToolExecution(pending.actionId, pending.toolName, result, ToolStatus.SUCCESS)
            } else {
                val isHindi = _currentLanguage.value.startsWith("hi")
                val deniedMsg = if (isHindi) "यूज़र ने यह कार्य करने से मना कर दिया।" else "User declined permission for this action."
                completeToolExecution(pending.actionId, pending.toolName, deniedMsg, ToolStatus.DENIED)
            }
        }
    }

    private fun completeToolExecution(callId: String, toolName: String, result: String, status: ToolStatus) {
        _messages.value = _messages.value.map { msg ->
            if (msg.toolCall?.id == callId) {
                msg.copy(
                    toolCall = msg.toolCall.copy(
                        result = result,
                        status = status
                    )
                )
            } else msg
        }

        conversationHistory.add(
            ChatHistoryItem(
                role = "user",
                toolCallName = toolName,
                toolCallId = callId,
                toolResult = result
            )
        )

        executeAiTurn()
    }

    fun speakText(text: String) {
        if (!_config.value.speakAnswers || !isTtsReady) return
        val clean = text.replace(Regex("[*#`_~]"), "").trim()
        textToSpeech?.speak(clean, TextToSpeech.QUEUE_FLUSH, null, "mamta_reply")
    }

    private fun tryParseAddCustomerCommand(text: String): com.example.data.sevakendra.SevaCustomerWork? {
        val clean = text.trim()
        val lower = clean.lowercase()
        if (!lower.startsWith("add customer") && !lower.startsWith("add work") && !lower.contains("ग्राहक") && !lower.contains("कस्टमर")) {
            return null
        }

        // Pattern 1: Add customer Ramesh, voter ID work, 200 rupees (comma separated)
        val commaRegex = Regex("(?i)(?:add customer|add work|ग्राहक जोड़ो|ग्राहक)\\s+([^,]+),\\s*([^,]+),\\s*(\\d+(?:\\.\\d+)?)\\s*(?:rupees|rs|रुपये|रूपये)?.*")
        val match1 = commaRegex.find(clean)
        if (match1 != null) {
            val name = match1.groupValues[1].replace(Regex("(?i)customer|ग्राहक"), "").trim()
            val service = match1.groupValues[2].trim()
            val amt = match1.groupValues[3].toDoubleOrNull() ?: 0.0
            if (name.isNotBlank() && service.isNotBlank() && amt > 0) {
                return com.example.data.sevakendra.SevaCustomerWork(
                    customerName = name,
                    serviceName = service,
                    amount = amt,
                    paidAmount = 0.0,
                    phoneNumber = ""
                )
            }
        }

        // Pattern 2: Add customer Ramesh voter ID work 200 rupees
        val spaceRegex = Regex("(?i)add customer\\s+([A-Za-z]+)\\s+(.+?)\\s+(\\d+(?:\\.\\d+)?)\\s*(?:rupees|rs)?.*")
        val match2 = spaceRegex.find(clean)
        if (match2 != null) {
            val name = match2.groupValues[1].trim()
            val service = match2.groupValues[2].replace(Regex("(?i)for|work|काम"), "").trim().ifBlank { "Service" }
            val amt = match2.groupValues[3].toDoubleOrNull() ?: 0.0
            if (name.isNotBlank() && amt > 0) {
                return com.example.data.sevakendra.SevaCustomerWork(
                    customerName = name,
                    serviceName = service,
                    amount = amt,
                    paidAmount = 0.0,
                    phoneNumber = ""
                )
            }
        }

        // Pattern 3: ग्राहक रमेश वोटर आईडी 200 रुपये
        val hindiRegex = Regex("(?:ग्राहक\\s+)?([\\u0900-\\u097FA-Za-z]+)\\s+(.+?)\\s+(\\d+(?:\\.\\d+)?)\\s*(?:रुपये|रूपये|रु).*")
        val match3 = hindiRegex.find(clean)
        if (match3 != null) {
            val name = match3.groupValues[1].replace("ग्राहक", "").trim()
            val service = match3.groupValues[2].replace("काम", "").replace("जोड़ो", "").trim().ifBlank { "सेवा" }
            val amt = match3.groupValues[3].toDoubleOrNull() ?: 0.0
            if (name.isNotBlank() && amt > 0) {
                return com.example.data.sevakendra.SevaCustomerWork(
                    customerName = name,
                    serviceName = service,
                    amount = amt,
                    paidAmount = 0.0,
                    phoneNumber = ""
                )
            }
        }

        return null
    }

    override fun onCleared() {
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        super.onCleared()
    }
}
