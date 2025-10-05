package com.belluxx.transfire.data

enum class MessageAuthor {
    SYSTEM,
    USER,
    ASSISTANT
}

data class Message(
    val name: MessageAuthor,
    val content: String
)

fun chatToJSON(chat: List<Message>, model: String): String {
    val messages = chat.joinToString(",") { message ->
        val role = message.name.toString().lowercase()
        val escapedContent = message.content
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
        """{"role":"$role","content":"$escapedContent"}"""
    }
    return """{"model":"$model","messages":[$messages]}"""
}