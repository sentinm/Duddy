package com.example.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.DownloadState
import com.example.ui.components.DownloadHistorySection
import com.example.ui.components.DownloadTrackerCard
import com.example.ui.components.DuddyTopBar
import com.example.ui.components.UrlInputSection
import kotlinx.coroutines.launch

@Composable
fun DuddyApp(
    viewModel: DownloadViewModel = viewModel()
) {
    val urlInput by viewModel.urlInput.collectAsState()
    val validationResult by viewModel.validationResult.collectAsState()
    val downloadState by viewModel.downloadState.collectAsState()
    val history by viewModel.history.collectAsState()
    val sampleLinks = viewModel.sampleLinks

    val isDownloading = downloadState is DownloadState.Downloading || downloadState is DownloadState.Validating
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("duddy_scaffold"),
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            DuddyTopBar()
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 680.dp)
                    .testTag("main_scrollable_column"),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // 1. Text Field Input with link validation and short link handling
                item(key = "url_input_section") {
                    UrlInputSection(
                        urlInput = urlInput,
                        validationResult = validationResult,
                        isDownloading = isDownloading,
                        sampleLinks = sampleLinks,
                        onUrlChange = { viewModel.onUrlInputChanged(it) },
                        onPaste = { pasted ->
                            viewModel.pasteUrl(pasted)
                            scope.launch {
                                snackbarHostState.showSnackbar("Link pasted from clipboard")
                            }
                        },
                        onClear = { viewModel.clearInput() },
                        onSampleClick = { sample ->
                            viewModel.useSample(sample)
                            scope.launch {
                                snackbarHostState.showSnackbar("Loaded '${sample.title}'")
                            }
                        },
                        onStartDownload = {
                            viewModel.startDownload()
                        }
                    )
                }

                // 2. Active Download Progress Tracker Card (shows file name badge directly on top of the progress bar)
                item(key = "download_tracker_card") {
                    DownloadTrackerCard(
                        downloadState = downloadState,
                        onCancel = { viewModel.cancelDownload() },
                        onRetry = { viewModel.retryDownload() },
                        onDismiss = { viewModel.dismissDownloadState() }
                    )
                }

                // 3. Download History & Library
                item(key = "download_history_section") {
                    DownloadHistorySection(
                        history = history,
                        onDeleteItem = { item ->
                            viewModel.deleteHistoryItem(item)
                            scope.launch {
                                snackbarHostState.showSnackbar("Removed '${item.fileName}'")
                            }
                        },
                        onClearAll = {
                            viewModel.clearAllHistory()
                            scope.launch {
                                snackbarHostState.showSnackbar("Download library cleared")
                            }
                        }
                    )
                }
            }
        }
    }
}
