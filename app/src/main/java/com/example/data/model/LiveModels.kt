package com.example.data.model

import org.json.JSONArray
import org.json.JSONObject

enum class AssistantVoiceState {
    DISCONNECTED,
    CONNECTING,
    LISTENING,
    THINKING,
    SPEAKING,
    EXECUTING_ACTION,
    ERROR
}

data class ContactInfo(
    val id: String,
    val name: String,
    val phoneNumber: String
)

data class ActionResult(
    val success: Boolean,
    val actionName: String,
    val message: String,
    val data: Map<String, Any?> = emptyMap(),
    val contactsList: List<ContactInfo> = emptyList()
) {
    fun toJson(): JSONObject {
        val json = JSONObject()
        json.put("status", if (success) "success" else "failure")
        json.put("action", actionName)
        json.put("message", message)
        
        val dataObj = JSONObject()
        data.forEach { (k, v) -> dataObj.put(k, v) }
        if (contactsList.isNotEmpty()) {
            val arr = JSONArray()
            contactsList.forEach { contact ->
                val c = JSONObject()
                c.put("name", contact.name)
                c.put("phoneNumber", contact.phoneNumber)
                arr.put(c)
            }
            dataObj.put("contacts", arr)
        }
        json.put("data", dataObj)
        return json
    }
}

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: MessageSender,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val actionDetails: ActionBadge? = null,
    val audioDurationMs: Long = 0
)

enum class MessageSender {
    USER,
    AIRA,
    SYSTEM
}

data class ActionBadge(
    val toolName: String,
    val summary: String,
    val success: Boolean
)
