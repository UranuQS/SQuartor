package com.squartor.reader.model

import androidx.compose.ui.text.font.FontWeight
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StyleModelsTest {

    @Test
    fun glassClarityMode_fromNameResolvesKnownValuesAndFallsBackToRegular() {
        assertEquals(GlassClarityMode.clear, GlassClarityMode.fromName("clear"))
        assertEquals(GlassClarityMode.regular, GlassClarityMode.fromName("regular"))
        assertEquals(GlassClarityMode.tinted, GlassClarityMode.fromName("tinted"))
        assertEquals(GlassClarityMode.regular, GlassClarityMode.fromName(null))
        assertEquals(GlassClarityMode.regular, GlassClarityMode.fromName(""))
        assertEquals(GlassClarityMode.regular, GlassClarityMode.fromName("CLEAR"))
        assertEquals(GlassClarityMode.regular, GlassClarityMode.fromName("unknown_mode"))
    }

    @Test
    fun readingStyle_defaultsAndFontWeightClampingStayWithinBounds() {
        val defaults = ReadingStyle()
        assertEquals(GlassClarityMode.regular, defaults.glassClarityMode)
        assertEquals(0.5f, defaults.glassOpacityLevel, 0.0001f)
        assertEquals(FontWeight(400), defaults.fontWeight)

        assertEquals(FontWeight(100), ReadingStyle(fontWeightValue = 10).fontWeight)
        assertEquals(FontWeight(900), ReadingStyle(fontWeightValue = 1200).fontWeight)
        assertEquals(FontWeight(600), ReadingStyle(fontWeightValue = 600).fontWeight)
    }

    @Test
    fun readingStyle_isScheduledLightHandlesStandardAndOvernightRanges() {
        val dayStyle = ReadingStyle(dayStartHour = 7, nightStartHour = 22)
        assertTrue(dayStyle.isScheduledLight(7))
        assertTrue(dayStyle.isScheduledLight(15))
        assertFalse(dayStyle.isScheduledLight(22))
        assertFalse(dayStyle.isScheduledLight(6))

        val wrapStyle = ReadingStyle(dayStartHour = 20, nightStartHour = 6)
        assertTrue(wrapStyle.isScheduledLight(21))
        assertTrue(wrapStyle.isScheduledLight(3))
        assertFalse(wrapStyle.isScheduledLight(6))
        assertFalse(wrapStyle.isScheduledLight(12))

        val equalStyle = ReadingStyle(dayStartHour = 8, nightStartHour = 8)
        assertTrue(equalStyle.isScheduledLight(0))
        assertTrue(equalStyle.isScheduledLight(14))
    }

    @Test
    fun readingStyle_toJsonAndFromJsonRoundTripsAllCustomFields() {
        val original = ReadingStyle(
            brightnessMode = AppBrightnessMode.dark,
            dayStartHour = 6,
            nightStartHour = 23,
            autoNightPaper = false,
            appTheme = AppThemeId.green,
            customThemeColorValue = 0xFF336699.toInt(),
            readerBackground = ReaderBackgroundId.green,
            fontSize = 22.5f,
            lineHeight = 2.0f,
            paragraphSpacing = 18f,
            letterSpacing = 0.8f,
            pageMargin = 32f,
            verticalMargin = 52f,
            readingFlow = ReadingFlowMode.scroll,
            reverseTapPageTurn = true,
            firstLineIndent = false,
            dimJapaneseText = false,
            pageTurnAnimation = true,
            fontWeightValue = 600,
            fontName = "SourceHanSerif",
            fontPath = "/books/fonts/serif.otf",
            appFontName = "NotoSansSC",
            appFontPath = "/books/fonts/ui.ttf",
            glassClarityMode = GlassClarityMode.clear,
            glassOpacityLevel = 0.82f,
        )

        val restored = ReadingStyle.fromJson(original.toJson())
        assertEquals(original, restored)
    }

    @Test
    fun cloudSyncSettings_toJsonAndFromJsonHandlesNullAndTimestamps() {
        val withNulls = CloudSyncSettings(
            enabled = true,
            endpoint = "https://dav.example.com/squartor",
            username = "reader",
            password = "secret",
            lastUploadAtMillis = null,
            lastDownloadAtMillis = null,
        )
        val restoredNulls = CloudSyncSettings.fromJson(withNulls.toJson())
        assertEquals(withNulls.enabled, restoredNulls.enabled)
        assertEquals(withNulls.endpoint, restoredNulls.endpoint)
        assertNull(restoredNulls.lastUploadAtMillis)
        assertNull(restoredNulls.lastDownloadAtMillis)

        val fallback = ReadingStyle.fromJson(JSONObject("{}"))
        assertEquals(ReadingStyle(), fallback)
    }

    @Test
    fun `ambient glow source defaults to bookCover and survives json roundtrip`() {
        assertEquals(AmbientGlowSource.bookCover, ReadingStyle().ambientGlowSource)
        assertEquals(AmbientGlowSource.bookCover, AmbientGlowSource.fromName(null))
        assertEquals(AmbientGlowSource.bookCover, AmbientGlowSource.fromName("unknown"))
        assertEquals(AmbientGlowSource.appTheme, AmbientGlowSource.fromName("appTheme"))

        val themed = ReadingStyle(ambientGlowSource = AmbientGlowSource.appTheme)
        val restored = ReadingStyle.fromJson(themed.toJson())
        assertEquals(AmbientGlowSource.appTheme, restored.ambientGlowSource)
    }
}
