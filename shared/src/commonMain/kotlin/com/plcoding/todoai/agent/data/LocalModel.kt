package com.plcoding.todoai.agent.data

/**
 * The model the agent runs. Gemma 4 E2B is small enough for phones and supports tool calling.
 */
object LocalModel {
    const val DISPLAY_NAME = "Gemma 4 E2B"
    const val FILE_NAME = "gemma-4-E2B-it.litertlm"
    const val DOWNLOAD_URL =
        "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/main/$FILE_NAME"
    const val SIZE_BYTES = 2_588_147_712L
}
