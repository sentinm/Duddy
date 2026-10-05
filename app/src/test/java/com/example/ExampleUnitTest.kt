package com.example

import com.example.model.UrlValidationResult
import com.example.util.FileHelpers
import com.example.util.UrlValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun urlValidator_validHttpsUrl_returnsValid() {
        val result = UrlValidator.validate("https://example.com/files/archive.zip")
        assertTrue(result is UrlValidationResult.Valid)
        val valid = result as UrlValidationResult.Valid
        assertEquals("https://example.com/files/archive.zip", valid.normalizedUrl)
        assertFalse(valid.isShortLink)
    }

    @Test
    fun urlValidator_shortLink_detectsShortener() {
        val result = UrlValidator.validate("https://tinyurl.com/2p992cvw")
        assertTrue(result is UrlValidationResult.Valid)
        val valid = result as UrlValidationResult.Valid
        assertTrue(valid.isShortLink)
        assertEquals("tinyurl.com", valid.shortLinkDomain)
    }

    @Test
    fun urlValidator_plainText_returnsInvalid() {
        val result = UrlValidator.validate("just some random text")
        assertTrue(result is UrlValidationResult.Invalid)
    }

    @Test
    fun urlValidator_empty_returnsInvalid() {
        val result = UrlValidator.validate("   ")
        assertTrue(result is UrlValidationResult.Invalid)
    }

    @Test
    fun urlValidator_disallowedScheme_returnsInvalid() {
        val result = UrlValidator.validate("javascript:alert(1)")
        assertTrue(result is UrlValidationResult.Invalid)
    }

    @Test
    fun fileHelpers_formatBytes_formatsCorrectly() {
        assertEquals("0 B", FileHelpers.formatBytes(0))
        assertEquals("1 KB", FileHelpers.formatBytes(1024))
        assertEquals("10 MB", FileHelpers.formatBytes(10 * 1024 * 1024))
    }

    @Test
    fun fileHelpers_extractFileName_fromUrl() {
        val name = FileHelpers.extractFileName("https://example.com/images/nature.png")
        assertEquals("nature.png", name)
    }
}
