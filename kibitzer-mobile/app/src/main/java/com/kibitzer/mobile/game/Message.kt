package com.kibitzer.mobile.game

data class Message(
    val id: String,
    val data: Any
) {
    fun toJson(): String {
        val dataStr = when (data) {
            is String -> data
            else -> data.toString()
        }
        return """{"id":"$id","data":$dataStr}"""
    }

    override fun toString(): String = toJson()
}