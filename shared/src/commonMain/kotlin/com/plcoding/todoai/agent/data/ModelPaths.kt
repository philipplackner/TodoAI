package com.plcoding.todoai.agent.data

/**
 * Platform directories for the model file and the LiteRT-LM cache. Both must be writable and
 * survive app restarts.
 */
data class ModelPaths(
    val modelsDir: String,
    val cacheDir: String
) {
    val modelFile: String get() = "$modelsDir/${LocalModel.FILE_NAME}"
}
