package com.example.data

import android.content.Context
import com.example.model.DownloadHistoryItem
import com.example.model.SampleDownloadLink
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

class DownloadRepository(private val context: Context) {

    private val historyFile = File(context.filesDir, "download_history.json")
    private val _history = MutableStateFlow<List<DownloadHistoryItem>>(emptyList())
    val history: StateFlow<List<DownloadHistoryItem>> = _history.asStateFlow()

    val sampleLinks: List<SampleDownloadLink> = listOf(
        SampleDownloadLink(
            title = "Sample PDF (3.2 MB)",
            description = "Standard PDF document test file",
            url = "https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf",
            expectedType = "PDF"
        ),
        SampleDownloadLink(
            title = "High-Res Image (1.5 MB)",
            description = "Sample landscape JPG image",
            url = "https://images.unsplash.com/photo-1506744038136-46273834b3fb?w=1200",
            expectedType = "JPG"
        ),
        SampleDownloadLink(
            title = "Sample Audio MP3 (2.5 MB)",
            description = "Open source audio test file",
            url = "https://actions.google.com/sounds/v1/ambiences/outdoor_rain.ogg",
            expectedType = "OGG"
        ),
        SampleDownloadLink(
            title = "Test File 10MB (Speed Test)",
            description = "Speed test binary payload",
            url = "https://speed.hetzner.de/10MB.bin",
            expectedType = "BIN"
        ),
        SampleDownloadLink(
            title = "Sample Short Link",
            description = "TinyURL redirect to test redirect resolution",
            url = "https://tinyurl.com/2p992cvw",
            expectedType = "REDIRECT"
        )
    )

    suspend fun loadHistory() = withContext(Dispatchers.IO) {
        if (!historyFile.exists()) {
            _history.value = emptyList()
            return@withContext
        }

        try {
            val content = historyFile.readText()
            val jsonArray = JSONArray(content)
            val list = mutableListOf<DownloadHistoryItem>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    DownloadHistoryItem(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        fileName = obj.optString("fileName", "download"),
                        filePath = obj.optString("filePath", ""),
                        fileSize = obj.optLong("fileSize", 0L),
                        sourceUrl = obj.optString("sourceUrl", ""),
                        completedAt = obj.optLong("completedAt", System.currentTimeMillis()),
                        mimeType = obj.optString("mimeType", null)
                    )
                )
            }
            _history.value = list.sortedByDescending { it.completedAt }
        } catch (_: Exception) {
            _history.value = emptyList()
        }
    }

    suspend fun addCompletedDownload(
        fileName: String,
        filePath: String,
        fileSize: Long,
        sourceUrl: String,
        mimeType: String?
    ) = withContext(Dispatchers.IO) {
        val newItem = DownloadHistoryItem(
            id = UUID.randomUUID().toString(),
            fileName = fileName,
            filePath = filePath,
            fileSize = fileSize,
            sourceUrl = sourceUrl,
            completedAt = System.currentTimeMillis(),
            mimeType = mimeType
        )
        val updated = listOf(newItem) + _history.value.filter { it.filePath != filePath }
        _history.value = updated
        saveHistoryToDisk(updated)
    }

    suspend fun deleteHistoryItem(item: DownloadHistoryItem, deleteFileFromDisk: Boolean = true) = withContext(Dispatchers.IO) {
        if (deleteFileFromDisk) {
            try {
                val file = File(item.filePath)
                if (file.exists()) file.delete()
            } catch (_: Exception) {}
        }
        val updated = _history.value.filter { it.id != item.id }
        _history.value = updated
        saveHistoryToDisk(updated)
    }

    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        _history.value = emptyList()
        if (historyFile.exists()) {
            historyFile.delete()
        }
    }

    private fun saveHistoryToDisk(items: List<DownloadHistoryItem>) {
        try {
            val jsonArray = JSONArray()
            for (item in items) {
                val obj = JSONObject()
                obj.put("id", item.id)
                obj.put("fileName", item.fileName)
                obj.put("filePath", item.filePath)
                obj.put("fileSize", item.fileSize)
                obj.put("sourceUrl", item.sourceUrl)
                obj.put("completedAt", item.completedAt)
                obj.put("mimeType", item.mimeType ?: "")
                jsonArray.put(obj)
            }
            historyFile.writeText(jsonArray.toString())
        } catch (_: Exception) {}
    }
}
