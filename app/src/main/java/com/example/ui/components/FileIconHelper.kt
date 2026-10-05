package com.example.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.ui.graphics.vector.ImageVector
import java.util.Locale

object FileIconHelper {
    fun getIconForFileName(fileName: String?): ImageVector {
        if (fileName.isNullOrBlank()) return Icons.Default.InsertDriveFile
        val ext = fileName.substringAfterLast('.', "").lowercase(Locale.ROOT)
        return when (ext) {
            "pdf" -> Icons.Default.Description
            "jpg", "jpeg", "png", "webp", "gif", "svg", "bmp" -> Icons.Default.Image
            "mp3", "ogg", "wav", "m4a", "flac", "aac" -> Icons.Default.AudioFile
            "mp4", "mkv", "avi", "mov", "webm", "3gp" -> Icons.Default.VideoFile
            "zip", "rar", "7z", "tar", "gz", "bz2" -> Icons.Default.FolderZip
            "apk" -> Icons.Default.Android
            else -> Icons.Default.InsertDriveFile
        }
    }
}
