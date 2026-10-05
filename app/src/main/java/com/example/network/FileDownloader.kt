package com.example.network

import android.content.Context
import android.os.Environment
import com.example.model.DownloadState
import com.example.util.FileHelpers
import com.example.util.UrlValidator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLException
import kotlin.coroutines.coroutineContext

class FileDownloader(private val context: Context) {

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    /**
     * Downloads a file from the provided URL, emitting progressive states.
     * Pause/Resume is deliberately omitted per specification.
     */
    fun download(url: String): Flow<DownloadState> = flow {
        val isShortLink = UrlValidator.validate(url) is com.example.model.UrlValidationResult.Valid &&
                UrlValidator.isShortLinkHost(java.net.URI(url).host)

        emit(DownloadState.Validating(url, if (isShortLink) "Resolving shortened link & redirects..." else "Connecting to download server..."))

        var targetFile: File? = null

        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Duddy-Android-Downloader/1.0 (Linux; Android)")
                .header("Accept", "*/*")
                .build()

            val response: Response = okHttpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                val code = response.code
                val reason = when (code) {
                    404 -> if (isShortLink) "Short link target not found (HTTP 404). The link may have expired or been deleted." else "File not found on server (HTTP 404)."
                    403 -> "Access denied (HTTP 403). The file may require authentication or token."
                    401 -> "Unauthorized (HTTP 401). Authentication credentials are required."
                    410 -> "Link has expired and is no longer available (HTTP 410 Gone)."
                    500, 502, 503 -> "Server error (HTTP $code). The host server is temporarily unavailable."
                    else -> "Download request failed with HTTP $code: ${response.message}"
                }
                emit(
                    DownloadState.Failed(
                        url = url,
                        fileName = null,
                        reason = reason,
                        isShortLinkError = isShortLink,
                        suggestedAction = if (isShortLink) "Check if the original short link is still active in a web browser" else "Verify the URL address"
                    )
                )
                response.close()
                return@flow
            }

            val body = response.body
            if (body == null) {
                emit(
                    DownloadState.Failed(
                        url = url,
                        fileName = null,
                        reason = "Server returned an empty response body.",
                        suggestedAction = "Check if the link points to a downloadable file."
                    )
                )
                return@flow
            }

            // Inspect Content-Type & Content-Disposition
            val contentType = body.contentType()?.toString()
            val contentDisposition = response.header("Content-Disposition")
            val finalUrl = response.request.url.toString()
            val totalBytes = body.contentLength() // can be -1 if chunked transfer

            val resolvedFileName = FileHelpers.extractFileName(
                url = finalUrl,
                contentDisposition = contentDisposition,
                mimeType = contentType
            )

            // Warning if response is an HTML web page rather than a binary file
            val isHtmlPage = contentType?.contains("text/html", ignoreCase = true) == true
            if (isHtmlPage && totalBytes in 1..65536) {
                // Peek if it might be an HTML landing page
                val peekString = try {
                    response.peekBody(2048).string()
                } catch (_: Exception) { "" }

                if (peekString.contains("<html", ignoreCase = true) || peekString.contains("<!DOCTYPE", ignoreCase = true)) {
                    emit(
                        DownloadState.Failed(
                            url = url,
                            fileName = resolvedFileName,
                            reason = "This link points to an HTML webpage instead of a direct file download.",
                            isShortLinkError = isShortLink,
                            suggestedAction = "Ensure the link is a direct download link (e.g. ending in .pdf, .zip, .mp3) and not a preview/landing webpage."
                        )
                    )
                    response.close()
                    return@flow
                }
            }

            // Target storage directory
            val downloadDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                ?: File(context.filesDir, "downloads").apply { mkdirs() }

            if (!downloadDir.exists()) {
                downloadDir.mkdirs()
            }

            // Create unique file if name collision exists
            targetFile = getUniqueTargetFile(downloadDir, resolvedFileName)

            val inputStream = body.byteStream()
            val outputStream = FileOutputStream(targetFile)

            val buffer = ByteArray(32 * 1024)
            var totalRead: Long = 0

            var lastEmissionTime = System.currentTimeMillis()
            var bytesSinceLastEmission: Long = 0
            var currentSpeed: Long = 0

            emit(
                DownloadState.Downloading(
                    url = url,
                    fileName = targetFile.name,
                    bytesDownloaded = 0L,
                    totalBytes = totalBytes,
                    progress = if (totalBytes > 0) 0f else -1f,
                    speedBytesPerSec = 0L,
                    etaSeconds = if (totalBytes > 0) 0L else -1L,
                    mimeType = contentType
                )
            )

            inputStream.use { input ->
                outputStream.use { output ->
                    while (coroutineContext.isActive) {
                        val bytesRead = input.read(buffer)
                        if (bytesRead == -1) break

                        output.write(buffer, 0, bytesRead)
                        totalRead += bytesRead
                        bytesSinceLastEmission += bytesRead

                        val now = System.currentTimeMillis()
                        val elapsed = now - lastEmissionTime

                        // Emit progress roughly every 150-250ms or when complete
                        if (elapsed >= 200 || (totalBytes > 0 && totalRead == totalBytes)) {
                            if (elapsed > 0) {
                                val instantSpeed = (bytesSinceLastEmission * 1000L) / elapsed
                                currentSpeed = if (currentSpeed == 0L) {
                                    instantSpeed
                                } else {
                                    (currentSpeed * 0.7 + instantSpeed * 0.3).toLong()
                                }
                            }
                            lastEmissionTime = now
                            bytesSinceLastEmission = 0

                            val progress = if (totalBytes > 0) {
                                (totalRead.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
                            } else {
                                -1f
                            }

                            val etaSeconds = if (totalBytes > 0 && currentSpeed > 0) {
                                ((totalBytes - totalRead) / currentSpeed).coerceAtLeast(0L)
                            } else {
                                -1L
                            }

                            emit(
                                DownloadState.Downloading(
                                    url = url,
                                    fileName = targetFile.name,
                                    bytesDownloaded = totalRead,
                                    totalBytes = totalBytes,
                                    progress = progress,
                                    speedBytesPerSec = currentSpeed,
                                    etaSeconds = etaSeconds,
                                    mimeType = contentType
                                )
                            )
                        }
                    }
                }
            }

            if (!coroutineContext.isActive) {
                targetFile.delete()
                emit(DownloadState.Idle)
                return@flow
            }

            // Download completed successfully
            emit(
                DownloadState.Completed(
                    fileName = targetFile.name,
                    filePath = targetFile.absolutePath,
                    fileSize = targetFile.length(),
                    completedAt = System.currentTimeMillis(),
                    mimeType = contentType ?: FileHelpers.getMimeTypeFromFile(targetFile)
                )
            )

        } catch (e: CancellationException) {
            targetFile?.delete()
            emit(DownloadState.Idle)
            throw e
        } catch (e: UnknownHostException) {
            targetFile?.delete()
            emit(
                DownloadState.Failed(
                    url = url,
                    fileName = null,
                    reason = if (isShortLink) "Could not resolve shortened link domain. The host does not exist or device is offline." else "Server hostname '${e.message}' could not be resolved. Check internet connection.",
                    isShortLinkError = isShortLink,
                    suggestedAction = "Check your Wi-Fi or mobile data, or verify the domain spelling."
                )
            )
        } catch (e: SocketTimeoutException) {
            targetFile?.delete()
            emit(
                DownloadState.Failed(
                    url = url,
                    fileName = null,
                    reason = "Connection timed out while waiting for server response.",
                    suggestedAction = "The download server might be overloaded or slow. Please try again."
                )
            )
        } catch (e: ConnectException) {
            targetFile?.delete()
            emit(
                DownloadState.Failed(
                    url = url,
                    fileName = null,
                    reason = "Failed to connect to the download server.",
                    suggestedAction = "Ensure the port is open and server is accepting connections."
                )
            )
        } catch (e: SSLException) {
            targetFile?.delete()
            emit(
                DownloadState.Failed(
                    url = url,
                    fileName = null,
                    reason = "SSL/TLS Security Handshake failed: ${e.message}",
                    suggestedAction = "The server's SSL certificate may be invalid, self-signed, or expired."
                )
            )
        } catch (e: IOException) {
            targetFile?.delete()
            emit(
                DownloadState.Failed(
                    url = url,
                    fileName = null,
                    reason = "Network or storage error: ${e.localizedMessage ?: "Unknown I/O error"}",
                    suggestedAction = "Check your storage space or try again."
                )
            )
        } catch (e: Exception) {
            targetFile?.delete()
            emit(
                DownloadState.Failed(
                    url = url,
                    fileName = null,
                    reason = "Unexpected error: ${e.localizedMessage ?: e.javaClass.simpleName}",
                    suggestedAction = "Please check the link and retry."
                )
            )
        }
    }.flowOn(Dispatchers.IO)

    private fun getUniqueTargetFile(dir: File, fileName: String): File {
        var file = File(dir, fileName)
        if (!file.exists()) return file

        val nameWithoutExt = fileName.substringBeforeLast('.', fileName)
        val ext = if (fileName.contains('.')) ".${fileName.substringAfterLast('.')}" else ""

        var counter = 1
        while (file.exists()) {
            file = File(dir, "$nameWithoutExt($counter)$ext")
            counter++
        }
        return file
    }
}
