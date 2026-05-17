package com.example.sayobotdownloader.ui.detail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.sayobotdownloader.data.BeatmapRepository
import com.example.sayobotdownloader.download.DownloadHelper
import com.example.sayobotdownloader.model.BeatmapDetail
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface DetailUiState {
    data object Loading : DetailUiState
    data class Success(val detail: BeatmapDetail) : DetailUiState
    data class Error(val message: String) : DetailUiState
}

class DetailViewModel(
    application: Application,
    private val sid: Int
) : AndroidViewModel(application) {

    private val repository = BeatmapRepository()
    val downloadHelper = DownloadHelper(application)
    val audioPlayer = AudioPreviewPlayer()

    private val _uiState = MutableStateFlow<DetailUiState>(DetailUiState.Loading)
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    private val _downloadState = MutableStateFlow<DownloadState?>(null)
    val downloadState: StateFlow<DownloadState?> = _downloadState.asStateFlow()

    private val _showDownloadDialog = MutableStateFlow(false)
    val showDownloadDialog: StateFlow<Boolean> = _showDownloadDialog.asStateFlow()

    init {
        loadDetail()
    }

    private fun loadDetail() {
        viewModelScope.launch {
            try {
                val response = repository.getDetail(sid)
                if (response.status == 0 && response.data != null) {
                    _uiState.value = DetailUiState.Success(response.data)
                } else {
                    _uiState.value = DetailUiState.Error("Failed to load beatmap")
                }
            } catch (e: Exception) {
                _uiState.value = DetailUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun playPreview() {
        audioPlayer.play(sid)
    }

    fun togglePlayback(): Boolean = audioPlayer.toggle()

    val isPlaying get() = audioPlayer.isPlaying
    val currentPosition get() = audioPlayer.currentPosition
    val playerDuration get() = audioPlayer.duration

    fun seekTo(positionMs: Int) = audioPlayer.seekTo(positionMs)

    fun showDownloadDialog() { _showDownloadDialog.value = true }
    fun dismissDownloadDialog() { _showDownloadDialog.value = false }

    fun startDownload(type: String) {
        val detail = (_uiState.value as? DetailUiState.Success)?.detail ?: return
        _showDownloadDialog.value = false
        val downloadId = downloadHelper.download(sid, detail.artist, detail.title, type)
        viewModelScope.launch {
            downloadHelper.observeDownload(downloadId).collect { state ->
                _downloadState.value = DownloadState(
                    progress = state.progress,
                    isComplete = state.isComplete,
                    isFailed = state.isFailed
                )
            }
        }
    }

    data class DownloadState(
        val progress: Float = 0f,
        val isComplete: Boolean = false,
        val isFailed: Boolean = false
    )

    override fun onCleared() {
        super.onCleared()
        audioPlayer.release()
    }
}
