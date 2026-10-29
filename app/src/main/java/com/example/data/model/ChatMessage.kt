package com.example.data.model

import java.util.UUID

enum class MessageRole {
    USER,
    ASSISTANT,
    SYSTEM,
    TOOL
}

enum class ToolStatus {
    RUNNING,
    SUCCESS,
    FAILED,
    WAITING_APPROVAL,
    DENIED
}

data class ToolCallInfo(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val arguments: Map<String, Any>,
    val result: String? = null,
    val status: ToolStatus = ToolStatus.RUNNING,
    val isDangerous: Boolean = false
)

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: MessageRole,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val toolCall: ToolCallInfo? = null,
    val imageBase64: String? = null,
    val imageFilePath: String? = null
)

data class DangerActionRequest(
    val actionId: String = UUID.randomUUID().toString(),
    val toolName: String,
    val arguments: Map<String, Any>,
    val humanReadableSummary: String
)
