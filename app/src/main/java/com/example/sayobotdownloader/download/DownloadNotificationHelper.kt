package com.example.sayobotdownloader.download

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.sayobotdownloader.MainActivity
import com.example.sayobotdownloader.R
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

class DownloadNotificationHelper(context: Context) {
    private val appContext = context.applicationContext
    private val notificationManager = NotificationManagerCompat.from(appContext)

    fun notifyStarted(downloadId: Long, fileName: String) {
        val task = TaskStatus(
            fileName = fileName,
            state = State.RUNNING,
            bytesDownloaded = 0L,
            totalBytes = 0L
        )
        tasks[downloadId] = task
        publish(downloadId, task)
    }

    fun notifyProgress(downloadId: Long, fileName: String, bytesDownloaded: Long, totalBytes: Long) {
        val task = TaskStatus(
            fileName = fileName,
            state = State.RUNNING,
            bytesDownloaded = bytesDownloaded,
            totalBytes = totalBytes
        )
        tasks[downloadId] = task
        publish(downloadId, task)
    }

    fun notifyCompleted(downloadId: Long, fileName: String, bytesDownloaded: Long, totalBytes: Long) {
        val task = TaskStatus(
            fileName = fileName,
            state = State.COMPLETED,
            bytesDownloaded = bytesDownloaded,
            totalBytes = totalBytes.takeIf { it > 0 } ?: bytesDownloaded
        )
        tasks[downloadId] = task
        publish(downloadId, task)
    }

    fun notifyFailed(downloadId: Long, fileName: String, bytesDownloaded: Long = 0L, totalBytes: Long = 0L) {
        val task = TaskStatus(
            fileName = fileName,
            state = State.FAILED,
            bytesDownloaded = bytesDownloaded,
            totalBytes = totalBytes
        )
        tasks[downloadId] = task
        publish(downloadId, task)
    }

    private fun publish(downloadId: Long, task: TaskStatus) {
        if (!canPostNotifications()) return

        try {
            notificationManager.notify(childNotificationId(downloadId), buildTaskNotification(task))
            notificationManager.notify(SUMMARY_ID, buildSummaryNotification())
        } catch (_: SecurityException) {
            // Android 13+ may revoke notification permission while a download is running.
        }
    }

    private fun buildTaskNotification(task: TaskStatus): Notification =
        NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_download)
            .setContentTitle(task.fileName)
            .setContentText(task.contentText())
            .setContentIntent(contentIntent())
            .setGroup(GROUP_KEY_DOWNLOADS)
            .setOnlyAlertOnce(true)
            .setOngoing(task.state == State.RUNNING)
            .setAutoCancel(task.state != State.RUNNING)
            .setLocalOnly(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .apply {
                when {
                    task.state == State.RUNNING && task.totalBytes > 0L -> {
                        setProgress(100, task.percent(), false)
                        setSubText("${task.percent()}%")
                    }
                    task.state == State.RUNNING -> setProgress(0, 0, true)
                }
            }
            .build()

    private fun buildSummaryNotification(): Notification {
        val snapshot = tasks.values.sortedWith(
            compareBy<TaskStatus> { it.state.order }.thenBy { it.fileName.lowercase(Locale.ROOT) }
        )
        val running = snapshot.count { it.state == State.RUNNING }
        val completed = snapshot.count { it.state == State.COMPLETED }
        val failed = snapshot.count { it.state == State.FAILED }
        val title = if (running > 0) {
            "\u6B63\u5728\u4E0B\u8F7D $running \u4E2A\u8C31\u9762"
        } else {
            "\u4E0B\u8F7D\u7ED3\u679C\uFF1A\u5B8C\u6210 $completed \u4E2A\uFF0C\u5931\u8D25 $failed \u4E2A"
        }
        val style = NotificationCompat.InboxStyle()
            .setBigContentTitle(title)

        snapshot.take(MAX_SUMMARY_LINES).forEach { task ->
            style.addLine(task.summaryLine())
        }
        if (snapshot.size > MAX_SUMMARY_LINES) {
            style.addLine("\u8FD8\u6709 ${snapshot.size - MAX_SUMMARY_LINES} \u4E2A\u4EFB\u52A1")
        }

        return NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_download)
            .setContentTitle(title)
            .setContentText(if (running > 0) "\u4E0B\u8F7D\u4E2D\u5FC3" else "\u70B9\u51FB\u6253\u5F00\u5E94\u7528")
            .setContentIntent(contentIntent())
            .setGroup(GROUP_KEY_DOWNLOADS)
            .setGroupSummary(true)
            .setOnlyAlertOnce(true)
            .setOngoing(running > 0)
            .setAutoCancel(running == 0)
            .setLocalOnly(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setStyle(style)
            .build()
    }

    private fun canPostNotifications(): Boolean {
        if (!notificationManager.areNotificationsEnabled()) return false
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun contentIntent(): PendingIntent {
        val intent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getActivity(appContext, 0, intent, flags)
    }

    private data class TaskStatus(
        val fileName: String,
        val state: State,
        val bytesDownloaded: Long,
        val totalBytes: Long
    ) {
        fun percent(): Int = if (totalBytes > 0L) {
            ((bytesDownloaded * 100L) / totalBytes).coerceIn(0L, 100L).toInt()
        } else {
            0
        }

        fun contentText(): String = when (state) {
            State.RUNNING -> if (totalBytes > 0L) {
                "${percent()}% \u2022 ${formatBytes(bytesDownloaded)} / ${formatBytes(totalBytes)}"
            } else {
                "\u5DF2\u4E0B\u8F7D ${formatBytes(bytesDownloaded)}"
            }
            State.COMPLETED -> "\u4E0B\u8F7D\u5B8C\u6210 \u2022 ${formatBytes(totalBytes.takeIf { it > 0 } ?: bytesDownloaded)}"
            State.FAILED -> "\u4E0B\u8F7D\u5931\u8D25"
        }

        fun summaryLine(): String = when (state) {
            State.RUNNING -> if (totalBytes > 0L) {
                "${percent()}% $fileName"
            } else {
                "\u4E0B\u8F7D\u4E2D $fileName"
            }
            State.COMPLETED -> "\u5B8C\u6210 $fileName"
            State.FAILED -> "\u5931\u8D25 $fileName"
        }
    }

    private enum class State(val order: Int) {
        RUNNING(0),
        FAILED(1),
        COMPLETED(2)
    }

    companion object {
        const val CHANNEL_ID = "sayobot_downloads"
        private const val CHANNEL_NAME = "\u8C31\u9762\u4E0B\u8F7D"
        private const val GROUP_KEY_DOWNLOADS = "com.example.sayobotdownloader.DOWNLOADS"
        private const val SUMMARY_ID = 9001
        private const val CHILD_ID_BASE = 10_000
        private const val MAX_SUMMARY_LINES = 7

        private val tasks = ConcurrentHashMap<Long, TaskStatus>()

        fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "\u663E\u793A\u8C31\u9762\u4E0B\u8F7D\u8FDB\u5EA6\u548C\u7ED3\u679C"
                setShowBadge(false)
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        private fun childNotificationId(downloadId: Long): Int =
            CHILD_ID_BASE + (downloadId % 1_000_000L).toInt()

        private fun formatBytes(bytes: Long): String {
            if (bytes <= 0L) return "0 B"
            val units = arrayOf("B", "KB", "MB", "GB")
            var value = bytes.toDouble()
            var unitIndex = 0
            while (value >= 1024.0 && unitIndex < units.lastIndex) {
                value /= 1024.0
                unitIndex++
            }
            return if (unitIndex == 0) {
                "${value.toLong()} ${units[unitIndex]}"
            } else {
                String.format(Locale.US, "%.1f %s", value, units[unitIndex])
            }
        }
    }
}
