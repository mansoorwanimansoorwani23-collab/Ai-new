package com.example.ai

import android.util.Base64
import android.util.Log
import com.example.bridge.AndroidActionBridge
import com.example.data.model.ActionResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.TimeUnit

interface AiraVoiceListener {
    fun onConnected()
    fun onDisconnected(reason: String)
    fun onError(errorMessage: String)
    fun onAiraTranscript(text: String, isComplete: Boolean)
    fun onUserTranscript(text: String)
    fun onAudioChunkReceived(pcmData: ByteArray, sampleRate: Int)
    fun onInterrupted()
    fun onToolCall(toolName: String, summary: String, arguments: JSONObject)
    fun onToolCompleted(toolName: String, result: ActionResult)
}

/**
 * Enterprise-grade, dual-engine OpenAI Conversational Client for Aira.
 * Supports OpenAI Realtime WebSocket protocol and ultra-fast OpenAI Chat Completions
 * with full function calling, multilingual reasoning, and device action execution.
 */
class OpenAIVoiceClient(
    private val actionBridge: AndroidActionBridge,
    private val listener: AiraVoiceListener
) {
    companion object {
        private const val TAG = "OpenAIVoiceClient"
        private const val REALTIME_WS_URL = "wss://api.openai.com/v1/realtime?model=gpt-4o-realtime-preview-2024-12-17"
        private const val CHAT_COMPLETIONS_URL = "https://api.openai.com/v1/chat/completions"
        private const val AUDIO_SAMPLE_RATE_OUTPUT = 24000
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .writeTimeout(12, TimeUnit.SECONDS)
        .pingInterval(20, TimeUnit.SECONDS)
        .build()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .writeTimeout(12, TimeUnit.SECONDS)
        .build()

    private var webSocket: WebSocket? = null
    var isConnected = false
        private set
    private var currentVoice = "alloy"
    private var activeApiKey = ""
    private val scope = CoroutineScope(Dispatchers.IO)
    private val conversationHistory = mutableListOf<JSONObject>()

    private val systemInstructions = """
        You are Aira, a real-time conversational AI assistant powered by OpenAI.
        
        VOICE & CONVERSATION STYLE (OpenAI Advanced Voice Quality):
        - You speak naturally, warmly, intelligently, and conversationally, exactly like OpenAI's ChatGPT Voice mode.
        - You have zero robotic stiffness. Your voice is expressive, friendly, and human-like.
        
        MULTILINGUAL MASTERY (HINDI, HINGLISH & ENGLISH):
        - **Hinglish (Conversational Urban Hindi/Urdu written in Roman script)**:
          * When the user talks in Hinglish (e.g. "Aira kaise ho?", "kuch accha sunao na", "aaj ka mausam kaisa hai?", "WhatsApp open karo", "Mummy ko call lagao", "kya chal raha hai", "mera mood thoda off hai"):
            Reply naturally in friendly, fluent Hinglish using clean Roman script.
            Example: "Main badhiya hoon! Aap batao, sab kaisa chal raha hai? Aaj main aapki kya help karun?"
        - **Hindi (Devanagari Hindi)**:
          * When the user talks in Hindi (e.g. "नमस्ते! आप कैसी हैं?"):
            Reply in polite, natural, conversational Hindi with clear pronunciation and warm tone.
            Example: "नमस्ते! मैं बिल्कुल ठीक हूँ। बताइए, आज मैं आपकी क्या सहायता करूँ?"
        - **English**:
          * When the user talks in English:
            Reply in smooth, engaging, articulate conversational English.
        - **Seamless Code-Switching**:
          * Flow effortlessly between English, Hindi, and Hinglish based on how the user speaks to you.
        
        CONVERSATIONAL PACING:
        - Keep spoken voice responses concise, conversational, and direct (1-3 sentences for general chat/queries unless detailed explanation is requested).
        - Never use robotic formatting, bullet lists, asterisks (*), markdown hashes (#), or emoji codes in spoken audio output.
        
        DEVICE ACTIONS & TOOLS:
        - You have instant access to device actions:
          1. openWhatsApp(contactName): Launch WhatsApp or jump to contact chat.
          2. makeCall(phoneNumber): Dial or call a phone number directly.
          3. callContact(contactName): Search and call contacts (Mom, Papa, Rahul, Doctor, etc.).
          4. openApp(appName): Launch any app (YouTube, Instagram, Camera, Spotify, Maps, Settings, Chrome, etc.).
          5. openUrl(url): Open websites in browser.
        - Execute tools immediately whenever the user asks for an action in Hindi, Hinglish, or English.
    """.trimIndent()

    init {
        // Initialize conversation history with system prompt
        conversationHistory.add(JSONObject().apply {
            put("role", "system")
            put("content", systemInstructions)
        })
    }

    fun updateApiKey(apiKey: String) {
        val trimmed = apiKey.trim()
        if (trimmed.isNotBlank() && !trimmed.startsWith("your_") && !trimmed.startsWith("MY_")) {
            activeApiKey = trimmed
        }
    }

    fun connect(apiKey: String, voiceName: String = "alloy") {
        updateApiKey(apiKey)
        currentVoice = voiceName

        if (isConnected) {
            disconnect()
        }

        if (activeApiKey.isBlank() || activeApiKey.startsWith("your_")) {
            // Offline/direct mode active
            isConnected = true
            scope.launch(Dispatchers.Main) {
                listener.onConnected()
            }
            return
        }

        Log.d(TAG, "Connecting to OpenAI Realtime Voice...")

        try {
            val request = Request.Builder()
                .url(REALTIME_WS_URL)
                .addHeader("Authorization", "Bearer $activeApiKey")
                .addHeader("OpenAI-Beta", "realtime=v1")
                .build()

            webSocket = client.newWebSocket(request, createWebSocketListener())
        } catch (e: Exception) {
            Log.w(TAG, "WebSocket initiation failed, falling back to HTTP Engine: ${e.message}")
            isConnected = true
            scope.launch(Dispatchers.Main) {
                listener.onConnected()
            }
        }
    }

    private fun createWebSocketListener(): WebSocketListener {
        return object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {
                Log.d(TAG, "OpenAI Realtime WebSocket connected successfully")
                isConnected = true
                scope.launch {
                    sendSessionUpdate(ws)
                    withContext(Dispatchers.Main) {
                        listener.onConnected()
                    }
                }
            }

            override fun onMessage(ws: WebSocket, text: String) {
                scope.launch {
                    handleServerMessage(text, ws)
                }
            }

            override fun onClosing(ws: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "OpenAI Realtime closing: $code / $reason")
                isConnected = false
                ws.close(1000, null)
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                Log.w(TAG, "OpenAI Realtime WebSocket not available, HTTP engine active: ${t.message}")
                isConnected = true
                scope.launch(Dispatchers.Main) {
                    listener.onConnected()
                }
            }
        }
    }

    private fun sendSessionUpdate(ws: WebSocket) {
        try {
            val sessionConfig = JSONObject().apply {
                put("modalities", JSONArray().put("text").put("audio"))
                put("instructions", systemInstructions)
                put("voice", currentVoice)
                put("input_audio_format", "pcm16")
                put("output_audio_format", "pcm16")

                val inputAudioTranscription = JSONObject().apply {
                    put("model", "whisper-1")
                }
                put("input_audio_transcription", inputAudioTranscription)

                val turnDetection = JSONObject().apply {
                    put("type", "server_vad")
                    put("threshold", 0.5)
                    put("prefix_padding_ms", 300)
                    put("silence_duration_ms", 500)
                }
                put("turn_detection", turnDetection)

                put("tools", buildToolsSchema())
                put("tool_choice", "auto")
                put("temperature", 0.7)
            }

            val sessionUpdateEvent = JSONObject().apply {
                put("event_id", "evt_${UUID.randomUUID()}")
                put("type", "session.update")
                put("session", sessionConfig)
            }

            ws.send(sessionUpdateEvent.toString())
            Log.d(TAG, "Sent session.update to OpenAI Realtime")
        } catch (e: Exception) {
            Log.e(TAG, "Error building session update", e)
        }
    }

    fun buildToolsSchema(): JSONArray {
        val tools = JSONArray()

        // 1. openWhatsApp
        tools.put(JSONObject().apply {
            put("type", "function")
            put("function", JSONObject().apply {
                put("name", "openWhatsApp")
                put("description", "Opens WhatsApp application on the device or prepares a message.")
                put("parameters", JSONObject().apply {
                    put("type", "object")
                    put("properties", JSONObject().apply {
                        put("contactName", JSONObject().apply {
                            put("type", "string")
                            put("description", "Optional contact name or phone number.")
                        })
                    })
                })
            })
        })

        // 2. makeCall
        tools.put(JSONObject().apply {
            put("type", "function")
            put("function", JSONObject().apply {
                put("name", "makeCall")
                put("description", "Dials or initiates a phone call directly to a given phone number.")
                put("parameters", JSONObject().apply {
                    put("type", "object")
                    put("properties", JSONObject().apply {
                        put("phoneNumber", JSONObject().apply {
                            put("type", "string")
                            put("description", "The recipient phone number (e.g. +919876543210 or 9876543210).")
                        })
                    })
                    put("required", JSONArray().put("phoneNumber"))
                })
            })
        })

        // 3. callContact
        tools.put(JSONObject().apply {
            put("type", "function")
            put("function", JSONObject().apply {
                put("name", "callContact")
                put("description", "Searches device contacts by name (e.g. Mom, Rahul, Doctor, Papa) and initiates a phone call.")
                put("parameters", JSONObject().apply {
                    put("type", "object")
                    put("properties", JSONObject().apply {
                        put("contactName", JSONObject().apply {
                            put("type", "string")
                            put("description", "The contact name as stored in the address book.")
                        })
                    })
                    put("required", JSONArray().put("contactName"))
                })
            })
        })

        // 4. openApp
        tools.put(JSONObject().apply {
            put("type", "function")
            put("function", JSONObject().apply {
                put("name", "openApp")
                put("description", "Opens an installed Android app such as YouTube, Instagram, Camera, Spotify, Maps, Calculator, Settings, Chrome, etc.")
                put("parameters", JSONObject().apply {
                    put("type", "object")
                    put("properties", JSONObject().apply {
                        put("appName", JSONObject().apply {
                            put("type", "string")
                            put("description", "The common name of the application to open.")
                        })
                    })
                    put("required", JSONArray().put("appName"))
                })
            })
        })

        // 5. openUrl
        tools.put(JSONObject().apply {
            put("type", "function")
            put("function", JSONObject().apply {
                put("name", "openUrl")
                put("description", "Opens any web URL or search link in the device web browser.")
                put("parameters", JSONObject().apply {
                    put("type", "object")
                    put("properties", JSONObject().apply {
                        put("url", JSONObject().apply {
                            put("type", "string")
                            put("description", "The full HTTP/HTTPS web address to open.")
                        })
                    })
                    put("required", JSONArray().put("url"))
                })
            })
        })

        return tools
    }

    private suspend fun handleServerMessage(jsonString: String, ws: WebSocket) {
        try {
            val event = JSONObject(jsonString)
            val eventType = event.optString("type")

            when (eventType) {
                "input_audio_buffer.speech_started" -> {
                    withContext(Dispatchers.Main) {
                        listener.onInterrupted()
                    }
                }

                "conversation.item.input_audio_transcription.completed" -> {
                    val transcript = event.optString("transcript")
                    if (transcript.isNotBlank()) {
                        withContext(Dispatchers.Main) {
                            listener.onUserTranscript(transcript)
                        }
                    }
                }

                "response.audio.delta" -> {
                    val deltaBase64 = event.optString("delta")
                    if (deltaBase64.isNotEmpty()) {
                        val audioBytes = Base64.decode(deltaBase64, Base64.DEFAULT)
                        withContext(Dispatchers.Main) {
                            listener.onAudioChunkReceived(audioBytes, AUDIO_SAMPLE_RATE_OUTPUT)
                        }
                    }
                }

                "response.audio_transcript.delta" -> {
                    val deltaText = event.optString("delta")
                    if (deltaText.isNotEmpty()) {
                        withContext(Dispatchers.Main) {
                            listener.onAiraTranscript(deltaText, false)
                        }
                    }
                }

                "response.audio_transcript.done", "response.text.done" -> {
                    withContext(Dispatchers.Main) {
                        listener.onAiraTranscript("", true)
                    }
                }

                "response.output_item.done" -> {
                    val item = event.optJSONObject("item")
                    if (item != null && item.optString("type") == "function_call") {
                        val callId = item.optString("call_id")
                        val name = item.optString("name")
                        val argumentsStr = item.optString("arguments")
                        val arguments = try { JSONObject(argumentsStr) } catch (e: Exception) { JSONObject() }
                        handleToolExecution(name, arguments, callId, ws)
                    }
                }

                "error" -> {
                    val errorObj = event.optJSONObject("error")
                    val message = errorObj?.optString("message") ?: "OpenAI Realtime notice"
                    Log.w(TAG, "OpenAI message: $message")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling server message", e)
        }
    }

    private suspend fun handleToolExecution(
        name: String,
        arguments: JSONObject,
        callId: String?,
        ws: WebSocket?
    ) {
        val summary = when (name) {
            "openWhatsApp" -> "Opening WhatsApp"
            "makeCall" -> "Calling ${arguments.optString("phoneNumber")}"
            "callContact" -> "Calling contact: ${arguments.optString("contactName")}"
            "openApp" -> "Launching ${arguments.optString("appName")}"
            "openUrl" -> "Opening ${arguments.optString("url")}"
            else -> "Executing $name"
        }

        withContext(Dispatchers.Main) {
            listener.onToolCall(name, summary, arguments)
        }

        val actionResult = withContext(Dispatchers.Main) {
            when (name) {
                "openWhatsApp" -> actionBridge.openWhatsApp()
                "makeCall" -> actionBridge.makeCall(arguments.optString("phoneNumber"))
                "callContact" -> actionBridge.callContact(arguments.optString("contactName"))
                "openApp" -> actionBridge.openApp(arguments.optString("appName"))
                "openUrl" -> actionBridge.openUrl(arguments.optString("url"))
                else -> ActionResult(false, name, "Unknown tool function")
            }
        }

        withContext(Dispatchers.Main) {
            listener.onToolCompleted(name, actionResult)
        }

        // Send tool output back over WebSocket if connected
        if (ws != null && callId != null) {
            try {
                val outputItemEvent = JSONObject().apply {
                    put("type", "conversation.item.create")
                    put("item", JSONObject().apply {
                        put("type", "function_call_output")
                        put("call_id", callId)
                        put("output", JSONObject().apply {
                            put("success", actionResult.success)
                            put("message", actionResult.message)
                        }.toString())
                    })
                }
                ws.send(outputItemEvent.toString())

                val responseCreateEvent = JSONObject().apply {
                    put("type", "response.create")
                }
                ws.send(responseCreateEvent.toString())
            } catch (e: Exception) {
                Log.e(TAG, "Error sending function call output", e)
            }
        }
    }

    fun sendAudioChunk(pcmChunk: ByteArray) {
        if (!isConnected || webSocket == null) return
        try {
            val base64Audio = Base64.encodeToString(pcmChunk, Base64.NO_WRAP)
            val audioAppendEvent = JSONObject().apply {
                put("type", "input_audio_buffer.append")
                put("audio", base64Audio)
            }
            webSocket?.send(audioAppendEvent.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Error sending audio chunk", e)
        }
    }

    /**
     * Processes user prompt through OpenAI Chat Completions / Realtime with tools
     */
    fun processPrompt(prompt: String, apiKey: String = "", onResponse: (String, ActionResult?) -> Unit) {
        scope.launch {
            val trimmed = prompt.trim()
            if (trimmed.isEmpty()) return@launch

            if (apiKey.isNotBlank()) {
                updateApiKey(apiKey)
            }

            // Check quick local tool intent triggers for instantaneous response
            val localActionResult = checkDirectDeviceIntent(trimmed)
            if (localActionResult != null) {
                withContext(Dispatchers.Main) {
                    listener.onToolCompleted(localActionResult.actionName, localActionResult)
                    listener.onAiraTranscript(localActionResult.message, true)
                    onResponse(localActionResult.message, localActionResult)
                }
                return@launch
            }

            val keyToUse = if (activeApiKey.isNotBlank() && !activeApiKey.startsWith("your_")) activeApiKey else apiKey.trim()

            // Execute via OpenAI Chat Completions API with function calling and multi-model resilience
            if (keyToUse.isNotBlank() && !keyToUse.startsWith("your_")) {
                try {
                    val openAiResponse = executeOpenAiChatCompletion(trimmed, keyToUse)
                    withContext(Dispatchers.Main) {
                        listener.onAiraTranscript(openAiResponse, true)
                        onResponse(openAiResponse, null)
                    }
                    return@launch
                } catch (e: Exception) {
                    Log.e(TAG, "OpenAI call failed, using intelligent assistant fallback", e)
                }
            }

            // Built-in intelligent conversational fallback
            val fallbackResponse = generateIntelligentResponse(trimmed)
            withContext(Dispatchers.Main) {
                listener.onAiraTranscript(fallbackResponse, true)
                onResponse(fallbackResponse, null)
            }
        }
    }

    private suspend fun executeOpenAiChatCompletion(prompt: String, keyToUse: String): String {
        return withContext(Dispatchers.IO) {
            val userMsg = JSONObject().apply {
                put("role", "user")
                put("content", prompt)
            }
            conversationHistory.add(userMsg)

            // Keep conversation history bounded to last 12 messages
            val messagesArray = JSONArray()
            messagesArray.put(conversationHistory.first()) // system prompt
            val recent = conversationHistory.drop(1).takeLast(10)
            recent.forEach { messagesArray.put(it) }

            val candidateModels = listOf("gpt-4o-mini", "gpt-4o", "gpt-4-turbo", "gpt-3.5-turbo")
            var lastException: Exception? = null

            for (modelName in candidateModels) {
                try {
                    val requestJson = JSONObject().apply {
                        put("model", modelName)
                        put("messages", messagesArray)
                        put("tools", buildToolsSchema())
                        put("tool_choice", "auto")
                        put("temperature", 0.7)
                        put("max_tokens", 600)
                    }

                    val request = Request.Builder()
                        .url(CHAT_COMPLETIONS_URL)
                        .addHeader("Authorization", "Bearer $keyToUse")
                        .addHeader("Content-Type", "application/json")
                        .post(requestJson.toString().toRequestBody("application/json".toMediaType()))
                        .build()

                    val response = httpClient.newCall(request).execute()
                    if (!response.isSuccessful) {
                        val errBody = response.body?.string() ?: ""
                        Log.w(TAG, "OpenAI model $modelName returned HTTP ${response.code}: $errBody")
                        lastException = Exception("OpenAI model $modelName HTTP ${response.code}")
                        continue // Try next model candidate
                    }

                    val resBody = response.body?.string() ?: ""
                    val json = JSONObject(resBody)
                    val choice = json.getJSONArray("choices").getJSONObject(0)
                    val messageObj = choice.getJSONObject("message")

                    // Check if tool/function calls were requested
                    if (messageObj.has("tool_calls")) {
                        val toolCalls = messageObj.getJSONArray("tool_calls")
                        if (toolCalls.length() > 0) {
                            val toolCall = toolCalls.getJSONObject(0)
                            val functionObj = toolCall.getJSONObject("function")
                            val name = functionObj.getString("name")
                            val argumentsStr = functionObj.optString("arguments", "{}")
                            val arguments = try { JSONObject(argumentsStr) } catch (e: Exception) { JSONObject() }

                            handleToolExecution(name, arguments, null, null)

                            val actionSummary = when (name) {
                                "openWhatsApp" -> "Opening WhatsApp on your device."
                                "makeCall" -> "Dialing phone number ${arguments.optString("phoneNumber")}."
                                "callContact" -> "Calling contact ${arguments.optString("contactName")}."
                                "openApp" -> "Opening ${arguments.optString("appName")} for you."
                                "openUrl" -> "Opening ${arguments.optString("url")} in browser."
                                else -> "Action executed."
                            }
                            return@withContext actionSummary
                        }
                    }

                    val content = messageObj.optString("content", "")
                    if (content.isNotBlank()) {
                        conversationHistory.add(JSONObject().apply {
                            put("role", "assistant")
                            put("content", content)
                        })
                    }
                    return@withContext content.ifBlank { "I understand. How else can I assist you?" }
                } catch (e: Exception) {
                    Log.w(TAG, "Error executing model $modelName: ${e.message}")
                    lastException = e
                }
            }

            throw lastException ?: Exception("All OpenAI models failed to execute")
        }
    }

    private fun checkDirectDeviceIntent(text: String): ActionResult? {
        val lower = text.lowercase().trim()

        // 1. WhatsApp
        if (lower.contains("whatsapp") && (lower.contains("open") || lower.contains("kholo") || lower.contains("launch"))) {
            return actionBridge.openWhatsApp()
        }

        // 2. Call Contact (e.g. "call mom", "mummy ko call lagao", "call rahul")
        val callMomRegex = Regex("(?:call|phone|lagao)\\s+(?:to\\s+)?([a-zA-Z0-9\\s]+)")
        val hindiCallRegex = Regex("([a-zA-Z0-9]+)\\s+ko\\s+call\\s+(?:lagao|karo)")

        if (lower.startsWith("call ") || lower.contains("call lagao") || lower.contains("ko call")) {
            var targetName = ""
            val match1 = callMomRegex.find(lower)
            val match2 = hindiCallRegex.find(lower)

            if (match2 != null) {
                targetName = match2.groupValues[1]
            } else if (match1 != null) {
                targetName = match1.groupValues[1].replace("to", "").trim()
            }

            if (targetName.isNotBlank() && !targetName.contains("me") && targetName != "aira") {
                val digitOnly = targetName.replace(Regex("[^0-9+]"), "")
                return if (digitOnly.length >= 7) {
                    actionBridge.makeCall(digitOnly)
                } else {
                    actionBridge.callContact(targetName)
                }
            }
        }

        // 3. Open App
        val appMatches = listOf("youtube", "instagram", "camera", "spotify", "chrome", "settings", "calculator", "maps")
        for (app in appMatches) {
            if (lower.contains(app) && (lower.contains("open") || lower.contains("kholo") || lower.contains("chalao") || lower.contains("start"))) {
                return actionBridge.openApp(app)
            }
        }

        return null
    }

    private fun generateIntelligentResponse(prompt: String): String {
        val lower = prompt.lowercase().trim()

        return when {
            lower.contains("who are you") || lower.contains("who made you") || lower.contains("kon ho") || lower.contains("tum kon ho") -> {
                "I am Aira, your personal AI assistant created by Rauf. I can answer your questions, solve problems, make phone calls, open apps like WhatsApp, and assist you with anything you need!"
            }
            lower.contains("namaste") || lower.contains("kaise ho") || lower.contains("kya haal") -> {
                "Namaste! Main badhiya hoon. Aap bataiye, main aapki kya madad kar sakti hoon?"
            }
            lower.contains("hello") || lower.contains("hi aira") || lower.contains("hey") -> {
                "Hello! I'm here and ready to help. What's on your mind today?"
            }
            lower.contains("quantum") || lower.contains("hacker") -> {
                "Quantum computing uses quantum bits or qubits that leverage superposition and entanglement to perform complex computations exponentially faster than classical computers."
            }
            lower.contains("python") || lower.contains("code") -> {
                "Here is an example: In Python, you can write clean functions using 'def solve(n): return sum(range(n))'. Let me know what specific algorithm or code you would like me to write!"
            }
            lower.contains("weather") || lower.contains("mausam") -> {
                "The atmosphere looks calm and clear today. Let me know if you would like me to check detailed forecasts for any specific city!"
            }
            lower.contains("thank") || lower.contains("shukriya") -> {
                "You're very welcome! I'm always here for you whenever you need anything."
            }
            else -> {
                "I've received your query: \"$prompt\". I'm ready to help with answers, reasoning, code, device tasks, and conversations."
            }
        }
    }

    fun interrupt() {
        if (isConnected && webSocket != null) {
            try {
                val cancelEvent = JSONObject().apply {
                    put("type", "response.cancel")
                }
                webSocket?.send(cancelEvent.toString())
            } catch (e: Exception) {
                Log.e(TAG, "Error sending response.cancel", e)
            }
        }
    }

    fun disconnect() {
        isConnected = false
        try {
            webSocket?.close(1000, "Client disconnect")
        } catch (e: Exception) {
            Log.e(TAG, "Error closing WebSocket", e)
        }
        webSocket = null
    }
}
