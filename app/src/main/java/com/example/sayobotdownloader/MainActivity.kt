package com.example.sayobotdownloader

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.sayobotdownloader.download.DownloadNotificationHelper
import com.example.sayobotdownloader.theme.SayobotDownloaderTheme

class MainActivity : ComponentActivity() {
  private var showNotificationPermissionDialog by mutableStateOf(false)

  private val notificationPermissionLauncher =
    registerForActivityResult(ActivityResultContracts.RequestPermission()) {
      showNotificationPermissionDialog = !canPostNotifications()
    }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    DownloadNotificationHelper.createNotificationChannel(this)
    checkNotificationPermission()

    enableEdgeToEdge()
    setContent {
      SayobotDownloaderTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
          MainNavigation()
          if (showNotificationPermissionDialog) {
            NotificationPermissionDialog(
              onRequestPermission = ::requestNotificationPermission,
              onOpenSettings = ::openNotificationSettings,
              onDismiss = { showNotificationPermissionDialog = false }
            )
          }
        }
      }
    }
  }

  override fun onResume() {
    super.onResume()
    checkNotificationPermission()
  }

  private fun checkNotificationPermission() {
    showNotificationPermissionDialog = !canPostNotifications()
  }

  private fun requestNotificationPermission() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    } else {
      openNotificationSettings()
    }
  }

  private fun canPostNotifications(): Boolean {
    if (!NotificationManagerCompat.from(this).areNotificationsEnabled()) return false
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
    return ContextCompat.checkSelfPermission(
      this,
      Manifest.permission.POST_NOTIFICATIONS
    ) == PackageManager.PERMISSION_GRANTED
  }

  private fun openNotificationSettings() {
    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
        putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
      }
    } else {
      Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", packageName, null)
      }
    }
    startActivity(intent)
  }
}

@Composable
private fun NotificationPermissionDialog(
  onRequestPermission: () -> Unit,
  onOpenSettings: () -> Unit,
  onDismiss: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Enable notifications") },
    text = { Text("Sayobot Downloader uses notifications to show download progress and completion results.") },
    confirmButton = {
      TextButton(onClick = onRequestPermission) {
        Text("Allow")
      }
    },
    dismissButton = {
      TextButton(onClick = onOpenSettings) {
        Text("Open settings")
      }
    }
  )
}
