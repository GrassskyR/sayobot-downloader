package com.example.sayobotdownloader

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.content.ContextCompat
import com.example.sayobotdownloader.download.DownloadNotificationHelper
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.sayobotdownloader.theme.SayobotDownloaderTheme

class MainActivity : ComponentActivity() {
  private val notificationPermissionLauncher =
    registerForActivityResult(ActivityResultContracts.RequestPermission()) {
      // Downloads continue normally when the permission is denied.
    }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    DownloadNotificationHelper.createNotificationChannel(this)
    requestNotificationPermissionIfNeeded()

    enableEdgeToEdge()
    setContent {
      SayobotDownloaderTheme { Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { MainNavigation() } }
    }
  }

  private fun requestNotificationPermissionIfNeeded() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

    val permission = Manifest.permission.POST_NOTIFICATIONS
    if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
      notificationPermissionLauncher.launch(permission)
    }
  }
}
