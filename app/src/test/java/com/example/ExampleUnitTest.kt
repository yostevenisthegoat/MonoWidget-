package com.example

import com.example.data.model.WidgetCategory
import com.example.data.model.WidgetConfig
import com.example.data.model.WidgetPayloadCodec
import com.example.data.model.WidgetType
import com.example.util.QrCodeGenerator
import com.example.util.QuotesBank
import com.example.util.SunCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun widgetCatalog_containsAll39UniqueWidgetsAcross5Categories() {
        assertEquals(39, WidgetType.entries.size)
        assertEquals(5, WidgetCategory.entries.size)
        val uniqueIds = WidgetType.entries.map { it.id }.toSet()
        assertEquals(39, uniqueIds.size)
    }

    @Test
    fun widgetConfig_createDefaultAndPayloadRoundTrip() {
        WidgetType.entries.forEach { type ->
            val cfg = WidgetConfig.createDefault(type)
            assertEquals(type, cfg.widgetType)
            val decoded = cfg.decodedPayload()
            val reEncoded = WidgetPayloadCodec.encode(decoded)
            assertTrue(reEncoded.isNotEmpty())
        }
    }

    @Test
    fun qrCodeGenerator_producesValidMatrix() {
        val qr = QrCodeGenerator.encode("https://github.com/widgetlite/widgetlite-android")
        assertTrue(qr.size >= 21)
        assertEquals(qr.size * qr.size, qr.modules.size)
        // Top-left finder corner must be dark
        assertTrue(qr.get(0, 0))
        assertTrue(qr.get(3, 3))
    }

    @Test
    fun sunCalculator_computesValidSolarTimes() {
        val sun = SunCalculator.calculate(37.7749, -122.4194)
        assertTrue(sun.sunriseText.matches(Regex("\\d{2}:\\d{2}")))
        assertTrue(sun.sunsetText.matches(Regex("\\d{2}:\\d{2}")))
        assertTrue(sun.daylightProgress in 0f..1f)
    }

    @Test
    fun quotesBank_cyclesSafely() {
        assertTrue(QuotesBank.quotes.isNotEmpty())
        val wrapped = QuotesBank.get(QuotesBank.quotes.size + 1)
        assertEquals(QuotesBank.get(1), wrapped)
    }
}
