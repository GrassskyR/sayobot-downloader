package com.example.sayobotdownloader.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import android.app.Application
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.example.sayobotdownloader.model.BeatmapDetail
import com.example.sayobotdownloader.model.BeatmapDifficulty
import com.example.sayobotdownloader.theme.StarBlue
import com.example.sayobotdownloader.theme.StarGreen
import com.example.sayobotdownloader.theme.StarOrange
import com.example.sayobotdownloader.theme.StarRed
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    sid: Int,
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val app = LocalContext.current.applicationContext as Application
    val viewModel: DetailViewModel = viewModel(key = "detail-$sid") { DetailViewModel(app, sid) }
    val uiState by viewModel.uiState.collectAsState()
    val downloadState by viewModel.downloadState.collectAsState()
    val showDownloadDialog by viewModel.showDownloadDialog.collectAsState()
    val leaveDetail = {
        viewModel.stopPreview()
        onBack()
    }

    BackHandler(onBack = leaveDetail)

    DisposableEffect(viewModel) {
        onDispose {
            viewModel.stopPreview()
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Text(
                    text = when (val state = uiState) {
                        is DetailUiState.Success -> state.detail.title
                        else -> title
                    },
                    maxLines = 1
                )
            },
            navigationIcon = {
                IconButton(onClick = leaveDetail) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                IconButton(onClick = { viewModel.showDownloadDialog() }) {
                    Icon(Icons.Default.Download, contentDescription = "Download")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        )

        when (val state = uiState) {
            is DetailUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is DetailUiState.Error -> {
                Text(
                    text = state.message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(32.dp)
                )
            }
            is DetailUiState.Success -> {
                DetailContent(
                    detail = state.detail,
                    sid = sid,
                    viewModel = viewModel
                )
            }
        }
    }

    if (showDownloadDialog) {
        DownloadDialog(
            hasVideo = (uiState as? DetailUiState.Success)?.detail?.video == 1,
            downloadState = downloadState,
            onSelect = { viewModel.startDownload(it) },
            onDismiss = { viewModel.dismissDownloadDialog() }
        )
    }
}

@Composable
private fun DetailContent(
    detail: BeatmapDetail,
    sid: Int,
    viewModel: DetailViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Cover image
        AsyncImage(
            model = "https://a.sayobot.cn/beatmaps/$sid/covers/cover.webp?0",
            contentDescription = detail.title,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            contentScale = ContentScale.Crop
        )

        // Audio controls
        AudioControls(viewModel)

        // Info area
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = detail.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            if (detail.titleU.isNotEmpty() && detail.titleU != detail.title) {
                Text(
                    text = detail.titleU,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = buildString {
                    if (detail.artist.isNotEmpty()) append(detail.artist)
                    if (detail.artist.isNotEmpty() && detail.creator.isNotEmpty()) append(" · ")
                    if (detail.creator.isNotEmpty()) append(detail.creator)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Status badge
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = approvedColor(detail.approved)
                )
            ) {
                Text(
                    text = approvedText(detail.approved),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (detail.bpm > 0) {
                    InfoChip("BPM: ${detail.bpm}")
                }
                if (detail.tags.isNotEmpty()) {
                    InfoChip("Tags: ${detail.tags.take(30)}")
                }
                if (detail.source.isNotEmpty()) {
                    InfoChip("Source: ${detail.source}")
                }
            }
        }

        // Difficulties
        if (detail.bid_data.isNotEmpty()) {
            Text(
                text = "Difficulties (${detail.bid_data.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            detail.bid_data.forEach { diff ->
                DifficultyCard(diff)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun AudioControls(viewModel: DetailViewModel) {
    var isPlaying by remember { mutableStateOf(viewModel.isPlaying) }
    var sliderPosition by remember { mutableFloatStateOf(0f) }
    var currentPosition by remember { mutableStateOf(0) }
    var duration by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            isPlaying = viewModel.isPlaying
            currentPosition = viewModel.currentPosition
            duration = viewModel.playerDuration
            if (duration > 0) {
                sliderPosition = currentPosition.toFloat() / duration.toFloat()
            }
            delay(200)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = {
            if (viewModel.audioPlayer.duration == 0) {
                viewModel.playPreview()
            }
            isPlaying = viewModel.togglePlayback()
        }) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play"
            )
        }

        Slider(
            value = sliderPosition,
            onValueChange = { sliderPosition = it },
            onValueChangeFinished = {
                viewModel.seekTo((sliderPosition * duration).toInt())
            },
            modifier = Modifier.weight(1f)
        )

        Text(
            text = "${formatTime(currentPosition / 1000)} / ${formatTime(duration / 1000)}",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@Composable
private fun DifficultyCard(diff: BeatmapDifficulty) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = diff.version,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = modeName(diff.mode),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Star rating
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "★",
                    style = MaterialTheme.typography.bodyMedium,
                    color = starColor(diff.star)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = String.format("%.2f", diff.star),
                    style = MaterialTheme.typography.bodyMedium,
                    color = starColor(diff.star),
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // AR / CS / OD / HP
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatChip("AR", diff.ar)
                if (diff.mode == 3) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Text(
                            text = "Keys: ${diff.cs.toInt()}Key",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                } else {
                    StatChip("CS", diff.cs)
                }
                StatChip("OD", diff.od)
                StatChip("HP", diff.hp)
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Object counts + length
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "${diff.circles} circles",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${diff.sliders} sliders",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${diff.spinners} spinners",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatTime(diff.length),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun StatChip(label: String, value: Double) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Text(
            text = "$label: ${String.format("%.1f", value)}",
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
private fun InfoChip(text: String) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1
        )
    }
}

@Composable
private fun DownloadDialog(
    hasVideo: Boolean,
    downloadState: DetailViewModel.DownloadState?,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Download Beatmap") },
        text = {
            Column {
                if (downloadState != null && downloadState.progress > 0f && !downloadState.isComplete) {
                    LinearProgressIndicator(
                        progress = { downloadState.progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    )
                    Text(
                        text = "${(downloadState.progress * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                if (downloadState?.isComplete == true) {
                    Text(
                        text = "Download complete",
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        confirmButton = {
            Column {
                TextButton(onClick = { onSelect("full") }) {
                    Text("Full (with video)")
                }
                TextButton(onClick = { onSelect("novideo") }) {
                    Text("No video")
                }
                TextButton(onClick = { onSelect("mini") }) {
                    Text("Mini (audio only)")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun formatTime(seconds: Int): String {
    if (seconds < 0) return "0:00"
    val min = seconds / 60
    val sec = seconds % 60
    return "%d:%02d".format(min, sec)
}

private fun approvedText(approved: Int): String = when (approved) {
    1 -> "Ranked"
    2 -> "Approved"
    3 -> "Qualified"
    4 -> "Loved"
    0 -> "Pending"
    -1 -> "WIP"
    -2 -> "Graveyard"
    else -> "Unknown"
}

private fun approvedColor(approved: Int): Color = when (approved) {
    1 -> Color(0xFF4CAF50)
    2 -> Color(0xFF4CAF50)
    3 -> Color(0xFF2196F3)
    4 -> Color(0xFFE91E63)
    0 -> Color(0xFFFFC107)
    -1 -> Color(0xFFFF9800)
    -2 -> Color(0xFF9E9E9E)
    else -> Color(0xFF9E9E9E)
}

private fun starColor(star: Double): Color = when {
    star < 2 -> StarBlue
    star < 4 -> StarGreen
    star < 6 -> StarOrange
    else -> StarRed
}

private fun modeName(mode: Int): String = when (mode) {
    0 -> "osu!standard"
    1 -> "osu!taiko"
    2 -> "osu!catch"
    3 -> "osu!mania"
    else -> "Unknown"
}
