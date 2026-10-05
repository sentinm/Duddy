package com.example.model

/**
 * Represents the state of the active download.
 * Note: Duddy does not include pause/resume functionality by design.
 */
sealed interface DownloadState {
    data object Idle : DownloadState

    data class Validating(
        val url: String,
        val message: String = "Inspecting link and resolving redirects..."
    ) : DownloadState

    data class Downloading(
        val url: String,
        val fileName: String,
        val bytesDownloaded: Long,
        val totalBytes: Long,
        val progress: Float, // 0f to 1f, or -1f if total size is unknown
        val speedBytesPerSec: Long,
        val etaSeconds: Long,
        val mimeType: String?
    ) : DownloadState

    data class Completed(
        val fileName: String,
        val filePath: String,
        val fileSize: Long,
        val completedAt: Long = System.currentTimeMillis(),
        val mimeType: String?
    ) : DownloadState

    data class Failed(
        val url: String,
        val fileName: String?,
        val reason: String,
        val isShortLinkError: Boolean = false,
        val suggestedAction: String? = null
    ) : DownloadState
}

/**
 * Historical record of a successfully downloaded file.
 */
data class DownloadHistoryItem(
    val id: String,
    val fileName: String,
    val filePath: String,
    val fileSize: Long,
    val sourceUrl: String,
    val completedAt: Long,
    val mimeType: String?
)

/**
 * Result of validating the URL text input.
 */
sealed interface UrlValidationResult {
    data class Valid(
        val normalizedUrl: String,
        val isShortLink: Boolean = false,
        val shortLinkDomain: String? = null
    ) : UrlValidationResult

    data class Invalid(
        val reason: String,
        val suggestion: String? = null
    ) : UrlValidationResult
}

/**
 * Sample links for rapid testing.
 */
data class SampleDownloadLink(
    val title: String,
    val description: String,
    val url: String,
    val expectedType: String
)
