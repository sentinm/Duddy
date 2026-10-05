package com.example.util

import com.example.model.UrlValidationResult
import java.net.URI
import java.util.Locale

object UrlValidator {

    private val KNOWN_SHORTENERS = setOf(
        "bit.ly",
        "tinyurl.com",
        "t.co",
        "goo.gl",
        "ow.ly",
        "is.gd",
        "buff.ly",
        "cutt.ly",
        "rb.gy",
        "shorturl.at",
        "rebrand.ly",
        "adf.ly",
        "bl.ink",
        "linktr.ee",
        "snip.ly"
    )

    /**
     * Validates whether the given string is a valid web link that can be downloaded.
     */
    fun validate(input: String): UrlValidationResult {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) {
            return UrlValidationResult.Invalid(
                reason = "URL cannot be empty",
                suggestion = "Paste or enter a valid web download link"
            )
        }

        // Check for disallowed non-web schemes
        val lower = trimmed.lowercase(Locale.ROOT)
        if (lower.startsWith("javascript:") || lower.startsWith("data:") ||
            lower.startsWith("file:") || lower.startsWith("content:") || lower.startsWith("intent:")
        ) {
            return UrlValidationResult.Invalid(
                reason = "Invalid protocol. Only web links (http:// or https://) are supported.",
                suggestion = "Please enter a web link starting with https://"
            )
        }

        // Normalize URL if missing scheme
        val candidate = when {
            trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true) -> trimmed
            trimmed.contains("://") -> {
                return UrlValidationResult.Invalid(
                    reason = "Unsupported URL protocol. Only HTTP and HTTPS links are allowed.",
                    suggestion = "Use a standard https:// web address"
                )
            }
            else -> "https://$trimmed"
        }

        return try {
            val uri = URI(candidate)
            val host = uri.host

            if (host.isNullOrBlank()) {
                return UrlValidationResult.Invalid(
                    reason = "Invalid link: Missing host name or domain",
                    suggestion = "Ensure your link includes a domain like 'example.com/file.zip'"
                )
            }

            // Verify host has at least one dot or is localhost
            if (!host.contains(".") && host != "localhost") {
                return UrlValidationResult.Invalid(
                    reason = "Invalid web address: '$host' is not a valid domain",
                    suggestion = "Enter a complete URL with a valid domain (e.g. https://domain.com/file)"
                )
            }

            // Check if it's a known URL shortener
            val isShortener = KNOWN_SHORTENERS.any { host.equals(it, ignoreCase = true) || host.endsWith(".$it", ignoreCase = true) }

            UrlValidationResult.Valid(
                normalizedUrl = candidate,
                isShortLink = isShortener,
                shortLinkDomain = if (isShortener) host else null
            )
        } catch (e: Exception) {
            UrlValidationResult.Invalid(
                reason = "Malformed link: ${e.message ?: "Invalid character or format"}",
                suggestion = "Check the link for typos or invalid characters"
            )
        }
    }

    /**
     * Determines whether a host matches a known short link service.
     */
    fun isShortLinkHost(host: String?): Boolean {
        if (host.isNullOrBlank()) return false
        val lower = host.lowercase(Locale.ROOT)
        return KNOWN_SHORTENERS.any { lower == it || lower.endsWith(".$it") }
    }
}
