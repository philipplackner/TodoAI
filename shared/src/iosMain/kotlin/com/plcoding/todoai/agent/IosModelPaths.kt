package com.plcoding.todoai.agent

import com.plcoding.todoai.agent.data.ModelPaths
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSNumber
import platform.Foundation.NSURL
import platform.Foundation.NSURLIsExcludedFromBackupKey
import platform.Foundation.NSUserDomainMask

/**
 * The model lives in Application Support and is excluded from iCloud backups, since it's a
 * 2.6 GB file the app can download again.
 */
@OptIn(ExperimentalForeignApi::class)
fun iosModelPaths(): ModelPaths {
    val fileManager = NSFileManager.defaultManager
    val modelsDir = directory(NSApplicationSupportDirectory).URLByAppendingPathComponent("models")!!
    val cacheDir = directory(NSCachesDirectory).URLByAppendingPathComponent("litertlm")!!
    listOf(modelsDir, cacheDir).forEach { url ->
        fileManager.createDirectoryAtURL(url, withIntermediateDirectories = true, attributes = null, error = null)
    }
    modelsDir.setResourceValue(NSNumber(bool = true), forKey = NSURLIsExcludedFromBackupKey, error = null)
    return ModelPaths(
        modelsDir = modelsDir.path!!,
        cacheDir = cacheDir.path!!
    )
}

@OptIn(ExperimentalForeignApi::class)
private fun directory(directory: ULong): NSURL {
    return NSFileManager.defaultManager.URLForDirectory(
        directory = directory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = true,
        error = null
    )!!
}
