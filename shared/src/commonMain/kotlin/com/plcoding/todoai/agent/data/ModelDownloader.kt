package com.plcoding.todoai.agent.data

import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentLength
import io.ktor.http.isSuccess
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem

data class DownloadProgress(
    val downloadedBytes: Long,
    val totalBytes: Long
)

class ModelDownloader(
    private val httpClient: HttpClient
) {

    /**
     * Downloads [url] to [targetFile]. Data goes to a `.part` file first, so an interrupted
     * download resumes where it stopped on the next call.
     */
    fun download(url: String, targetFile: String, expectedSizeBytes: Long): Flow<DownloadProgress> = flow {
        val target = Path(targetFile)
        val partFile = Path("$targetFile.part")
        target.parent?.let { SystemFileSystem.createDirectories(it) }

        val existingBytes = SystemFileSystem.metadataOrNull(partFile)?.size ?: 0L

        httpClient.prepareGet(url) {
            if (existingBytes > 0) {
                header(HttpHeaders.Range, "bytes=$existingBytes-")
            }
        }.execute { response ->
            if (!response.status.isSuccess()) {
                throw ModelDownloadException("Download failed with HTTP ${response.status.value}")
            }
            val isResuming = response.status == HttpStatusCode.PartialContent
            var downloadedBytes = if (isResuming) existingBytes else 0L
            val totalBytes = response.contentLength()?.let { it + downloadedBytes } ?: expectedSizeBytes
            emit(DownloadProgress(downloadedBytes, totalBytes))

            val channel = response.bodyAsChannel()
            val buffer = ByteArray(BUFFER_SIZE)
            var lastEmittedBytes = downloadedBytes
            SystemFileSystem.sink(partFile, append = isResuming).buffered().use { sink ->
                while (true) {
                    val read = channel.readAvailable(buffer, 0, buffer.size)
                    if (read == -1) break
                    sink.write(buffer, 0, read)
                    downloadedBytes += read
                    if (downloadedBytes - lastEmittedBytes >= EMIT_EVERY_BYTES) {
                        lastEmittedBytes = downloadedBytes
                        emit(DownloadProgress(downloadedBytes, totalBytes))
                    }
                }
            }
            if (downloadedBytes != totalBytes) {
                throw ModelDownloadException("Download ended early at $downloadedBytes of $totalBytes bytes")
            }
            emit(DownloadProgress(downloadedBytes, totalBytes))
        }

        SystemFileSystem.atomicMove(partFile, target)
    }.flowOn(Dispatchers.IO)

    private companion object {
        const val BUFFER_SIZE = 256 * 1024
        const val EMIT_EVERY_BYTES = 4L * 1024 * 1024
    }
}

class ModelDownloadException(message: String) : Exception(message)
