package com.example.sayobotdownloader.download

import android.content.Context
import android.media.MediaScannerConnection
import android.os.Environment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

class DownloadHelper(private val context: Context) {
    private val client = OkHttpClient.Builder().build()
    private val downloadStates = ConcurrentHashMap<Long, MutableStateFlow<DownloadState>>()
    private val notificationHelper = DownloadNotificationHelper(context)

    data class DownloadState(
        val status: Int,
        val bytesDownloaded: Long,
        val totalBytes: Long,
        val fileName: String = ""
    ) {
        val progress get() = if (totalBytes > 0) bytesDownloaded.toFloat() / totalBytes else 0f
        val isComplete get() = status == STATUS_SUCCESSFUL
        val isFailed get() = status == STATUS_FAILED

        companion object {
            const val STATUS_RUNNING = 0
            const val STATUS_SUCCESSFUL = 1
            const val STATUS_FAILED = 2
        }
    }

    fun download(sid: Int, artist: String, title: String, type: String = "full"): Long {
        val url = "https://txy1.sayobot.cn/beatmaps/download/$type/$sid?server=auto&t=${System.currentTimeMillis()}"
        val safeName = buildOszFileName(sid, artist, title)

        val downloadId = nextDownloadId.getAndIncrement()
        currentDownloadId = downloadId
        val progress = MutableStateFlow(DownloadState(DownloadState.STATUS_RUNNING, 0, 0, safeName))
        downloadStates[downloadId] = progress
        notificationHelper.notifyStarted(downloadId, safeName)

        Thread {
            var downloaded = 0L
            var totalBytes = 0L
            try {
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                response.use { resp ->
                    if (!resp.isSuccessful) {
                        progress.value = DownloadState(DownloadState.STATUS_FAILED, 0, 0, safeName)
                        notificationHelper.notifyFailed(downloadId, safeName)
                        return@use
                    }
                    val body = resp.body ?: run {
                        progress.value = DownloadState(DownloadState.STATUS_FAILED, 0, 0, safeName)
                        notificationHelper.notifyFailed(downloadId, safeName)
                        return@use
                    }
                    totalBytes = body.contentLength()
                    val extDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    val dir = File(extDir, "osu!")
                    dir.mkdirs()
                    val file = File(dir, safeName)
                    var lastNotifyAt = 0L
                    var lastNotifyPercent = -1
                    body.byteStream().use { input ->
                        file.outputStream().use { output ->
                            val buffer = ByteArray(8192)
                            var read: Int
                            while (input.read(buffer).also { read = it } != -1) {
                                output.write(buffer, 0, read)
                                downloaded += read
                                progress.value = DownloadState(
                                    DownloadState.STATUS_RUNNING, downloaded, totalBytes, safeName
                                )
                                val percent = if (totalBytes > 0L) ((downloaded * 100L) / totalBytes).toInt() else -1
                                val now = System.currentTimeMillis()
                                if (percent != lastNotifyPercent || now - lastNotifyAt >= NOTIFICATION_UPDATE_INTERVAL_MS) {
                                    notificationHelper.notifyProgress(downloadId, safeName, downloaded, totalBytes)
                                    lastNotifyPercent = percent
                                    lastNotifyAt = now
                                }
                            }
                        }
                    }
                    MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), null, null)
                    progress.value = DownloadState(DownloadState.STATUS_SUCCESSFUL, downloaded, totalBytes, safeName)
                    notificationHelper.notifyCompleted(downloadId, safeName, downloaded, totalBytes)
                }
            } catch (e: Exception) {
                progress.value = DownloadState(DownloadState.STATUS_FAILED, downloaded, totalBytes, safeName)
                notificationHelper.notifyFailed(downloadId, safeName, downloaded, totalBytes)
            }
        }.start()

        return downloadId
    }

    fun observeDownload(downloadId: Long): Flow<DownloadState> = flow {
        val progress = downloadStates[downloadId] ?: run {
            emit(DownloadState(DownloadState.STATUS_FAILED, 0, 0))
            return@flow
        }
        while (true) {
            val state = progress.value
            emit(state)
            if (state.isComplete || state.isFailed) {
                downloadStates.remove(downloadId)
                return@flow
            }
            delay(500)
        }
    }.flowOn(Dispatchers.IO)

    companion object {
        private const val NOTIFICATION_UPDATE_INTERVAL_MS = 500L
        private val nextDownloadId = AtomicLong(1L)
        @Volatile var currentDownloadId = 0L
            private set
    }
}

internal fun buildOszFileName(sid: Int, artist: String, title: String): String {
    val baseName = "$sid $artist - $title"
        .replace("[\\\\/:*?\"<>|]".toRegex(), "_")
        .trim()
        .removeSuffix(".zip")
        .removeSuffix(".osz")
        .trimEnd()
        .ifEmpty { sid.toString() }

    val maxBaseLength = 200 - ".osz".length
    return baseName.take(maxBaseLength).trimEnd() + ".osz"
}
