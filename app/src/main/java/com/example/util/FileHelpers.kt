package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.text.DecimalFormat
import java.util.Locale
import java.util.regex.Pattern

object FileHelpers {

    fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
            .coerceIn(0, units.size - 1)
        val value = bytes / Math.pow(1024.0, digitGroups.toDouble())
        val df = DecimalFormat("#,##0.#")
        return "${df.format(value)} ${units[digitGroups]}"
    }

    fun formatSpeed(bytesPerSec: Long): String {
        if (bytesPerSec <= 0) return "0 KB/s"
        return "${formatBytes(bytesPerSec)}/s"
    }

    fun formatEta(seconds: Long): String {
        if (seconds <= 0) return "Finishing..."
        if (seconds < 60) return "~${seconds}s remaining"
        val mins = seconds / 60
        val remainingSecs = seconds % 60
        return if (mins < 60) {
            "~${mins}m ${remainingSecs}s remaining"
        } else {
            val hours = mins / 60
            val remainingMins = mins % 60
            "~${hours}h ${remainingMins}m remaining"
        }
    }

    /**
     * Determines the best file name based on Content-Disposition header, URL path, and fallback.
     */
    fun extractFileName(url: String, contentDisposition: String? = null, mimeType: String? = null): String {
        // Try content disposition first
        if (!contentDisposition.isNullOrBlank()) {
            val cdFileName = parseContentDisposition(contentDisposition)
            if (!cdFileName.isNullOrBlank()) {
                return sanitizeFileName(cdFileName)
            }
        }

        // Try extracting from URL path
        try {
            val cleanUrl = url.substringBefore('?').substringBefore('#')
            val rawPathSegment = cleanUrl.substringAfterLast('/', "")
            if (rawPathSegment.isNotBlank()) {
                val decoded = URLDecoder.decode(rawPathSegment, StandardCharsets.UTF_8.name())
                if (decoded.isNotBlank() && decoded != "/") {
                    val sanitized = sanitizeFileName(decoded)
                    if (sanitized.contains(".")) {
                        return sanitized
                    }
                    // If no extension, try adding extension from mime
                    val ext = getExtensionFromMime(mimeType)
                    return if (ext != null) "$sanitized.$ext" else sanitized
                }
            }
        } catch (_: Exception) {}

        // Fallback name with extension if known
        val ext = getExtensionFromMime(mimeType) ?: "bin"
        return "download_${System.currentTimeMillis()}.$ext"
    }

    private fun parseContentDisposition(header: String): String? {
        try {
            // Check filename*=UTF-8''encoded_name
            val utf8Matcher = Pattern.compile("filename\\*=(?:UTF-8''|utf-8'')([^;]+)", Pattern.CASE_INSENSITIVE).matcher(header)
            if (utf8Matcher.find()) {
                val raw = utf8Matcher.group(1)?.trim('"', '\'', ' ')
                if (!raw.isNullOrBlank()) {
                    return URLDecoder.decode(raw, StandardCharsets.UTF_8.name())
                }
            }

            // Check filename="name" or filename=name
            val matcher = Pattern.compile("filename=([^;]+)", Pattern.CASE_INSENSITIVE).matcher(header)
            if (matcher.find()) {
                var name = matcher.group(1)?.trim('"', '\'', ' ')
                if (!name.isNullOrBlank()) {
                    return name
                }
            }
        } catch (_: Exception) {}
        return null
    }

    private fun sanitizeFileName(name: String): String {
        return name.replace(Regex("[\\\\/:*?\"<>|]"), "_")
            .trim()
            .take(120)
    }

    fun getExtensionFromMime(mime: String?): String? {
        if (mime.isNullOrBlank()) return null
        val cleanMime = mime.substringBefore(';').trim().lowercase(Locale.ROOT)
        return MimeTypeMap.getSingleton().getExtensionFromMimeType(cleanMime)
    }

    fun getMimeTypeFromFile(file: File): String {
        val extension = file.extension.lowercase(Locale.ROOT)
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "application/octet-stream"
    }

    /**
     * Attempts to open the file with an external viewer app using FileProvider.
     */
    fun openFile(context: Context, filePath: String) {
        val file = File(filePath)
        if (!file.exists()) {
            Toast.makeText(context, "File no longer exists on disk", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val mime = getMimeTypeFromFile(file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mime)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Open file with..."))
        } catch (e: Exception) {
            Toast.makeText(context, "No app available to open this file type (${file.extension})", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Shares the downloaded file using Android Share Sheet.
     */
    fun shareFile(context: Context, filePath: String) {
        val file = File(filePath)
        if (!file.exists()) {
            Toast.makeText(context, "File not found", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val mime = getMimeTypeFromFile(file)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = mime
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, file.name)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share file via"))
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to share file: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
