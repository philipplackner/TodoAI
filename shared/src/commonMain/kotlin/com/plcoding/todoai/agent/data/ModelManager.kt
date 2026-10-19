package com.plcoding.todoai.agent.data

import com.plcoding.todoai.agent.domain.LlmEngine
import com.plcoding.todoai.agent.domain.ModelError
import com.plcoding.todoai.agent.domain.ModelState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem

/**
 * Owns the model's lifecycle: download it once, then load it into the [LlmEngine]. Work runs in
 * [applicationScope] so a download keeps going when the chat sheet is closed.
 */
class ModelManager(
    private val engine: LlmEngine,
    private val downloader: ModelDownloader,
    private val paths: ModelPaths,
    private val applicationScope: CoroutineScope
) {
    private val _state = MutableStateFlow<ModelState>(ModelState.NotDownloaded)
    val state = _state.asStateFlow()

    private var job: Job? = null

    /**
     * Loads the model if it's already on the device. Does nothing while a download or load runs.
     */
    fun prepare() {
        if (job?.isActive == true || engine.isLoaded) {
            if (engine.isLoaded) _state.value = ModelState.Ready
            return
        }
        if (SystemFileSystem.exists(Path(paths.modelFile))) {
            load()
        } else {
            _state.value = ModelState.NotDownloaded
        }
    }

    fun download() {
        if (job?.isActive == true) return
        job = applicationScope.launch {
            try {
                downloader.download(
                    url = LocalModel.DOWNLOAD_URL,
                    targetFile = paths.modelFile,
                    expectedSizeBytes = LocalModel.SIZE_BYTES
                ).collect { progress ->
                    _state.value = ModelState.Downloading(progress.downloadedBytes, progress.totalBytes)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                println("Model download failed: $e")
                _state.value = ModelState.Failed(ModelError.DOWNLOAD_FAILED)
                return@launch
            }
            loadModel()
        }
    }

    private fun load() {
        job = applicationScope.launch { loadModel() }
    }

    private suspend fun loadModel() {
        _state.value = ModelState.Loading
        try {
            engine.load(modelPath = paths.modelFile, cacheDir = paths.cacheDir)
            _state.value = ModelState.Ready
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            println("Model load failed: $e")
            _state.value = ModelState.Failed(ModelError.LOAD_FAILED)
        }
    }
}
