package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.DownloadRepository
import com.example.model.DownloadHistoryItem
import com.example.model.DownloadState
import com.example.model.SampleDownloadLink
import com.example.model.UrlValidationResult
import com.example.network.FileDownloader
import com.example.util.UrlValidator
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DownloadViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DownloadRepository(application)
    private val downloader = FileDownloader(application)

    private val _urlInput = MutableStateFlow("")
    val urlInput: StateFlow<String> = _urlInput.asStateFlow()

    private val _validationResult = MutableStateFlow<UrlValidationResult?>(null)
    val validationResult: StateFlow<UrlValidationResult?> = _validationResult.asStateFlow()

    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    val history: StateFlow<List<DownloadHistoryItem>> = repository.history
    val sampleLinks: List<SampleDownloadLink> = repository.sampleLinks

    private var downloadJob: Job? = null

    init {
        viewModelScope.launch {
            repository.loadHistory()
        }
    }

    fun onUrlInputChanged(input: String) {
        _urlInput.value = input
        if (input.isBlank()) {
            _validationResult.value = null
        } else {
            _validationResult.value = UrlValidator.validate(input)
        }
    }

    fun pasteUrl(pasted: String) {
        onUrlInputChanged(pasted)
    }

    fun clearInput() {
        _urlInput.value = ""
        _validationResult.value = null
    }

    fun useSample(sample: SampleDownloadLink) {
        onUrlInputChanged(sample.url)
    }

    fun startDownload() {
        val currentInput = _urlInput.value.trim()
        val validation = UrlValidator.validate(currentInput)
        _validationResult.value = validation

        if (validation !is UrlValidationResult.Valid) {
            return
        }

        val targetUrl = validation.normalizedUrl

        // Cancel any active job
        downloadJob?.cancel()

        downloadJob = viewModelScope.launch {
            downloader.download(targetUrl).collect { state ->
                _downloadState.value = state

                if (state is DownloadState.Completed) {
                    repository.addCompletedDownload(
                        fileName = state.fileName,
                        filePath = state.filePath,
                        fileSize = state.fileSize,
                        sourceUrl = targetUrl,
                        mimeType = state.mimeType
                    )
                }
            }
        }
    }

    /**
     * Cancels the active download immediately.
     * Pause/Resume is deliberately omitted per specification.
     */
    fun cancelDownload() {
        downloadJob?.cancel()
        downloadJob = null
        _downloadState.value = DownloadState.Idle
    }

    fun retryDownload() {
        startDownload()
    }

    fun dismissDownloadState() {
        _downloadState.value = DownloadState.Idle
    }

    fun deleteHistoryItem(item: DownloadHistoryItem) {
        viewModelScope.launch {
            repository.deleteHistoryItem(item)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }
}
