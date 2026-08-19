package com.anyonehub.abl.utils

object EngineLogger {
    fun logInfo(message: String) {
        println("[INFO] $message")
    }

    fun logError(message: String, throwable: Throwable? = null) {
        System.err.println("[ERROR] $message")
        throwable?.printStackTrace(System.err)
    }
}
