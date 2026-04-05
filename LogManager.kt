package com.example.bandmapper

import androidx.compose.runtime.mutableStateListOf

object LogManager {
    val logs = mutableStateListOf<String>()

    fun log(message: String) {
        if (logs.size > 500) {
            logs.removeAt(0)
        }
        logs.add("${System.currentTimeMillis()}: $message")
    }

    fun clear() {
        logs.clear()
    }
}
