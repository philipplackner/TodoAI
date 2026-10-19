package com.plcoding.todoai.agent.domain

sealed interface ModelState {
    data object NotDownloaded : ModelState
    data class Downloading(val downloadedBytes: Long, val totalBytes: Long) : ModelState {
        val progress: Float get() = if (totalBytes > 0) downloadedBytes.toFloat() / totalBytes else 0f
    }
    data object Loading : ModelState
    data object Ready : ModelState
    data class Failed(val error: ModelError) : ModelState
}

enum class ModelError {
    DOWNLOAD_FAILED,
    LOAD_FAILED
}
