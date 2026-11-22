package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.WidgetConfig
import com.example.data.model.WidgetPayloadCodec
import com.example.data.model.WidgetType
import com.example.util.QrCodeGenerator
import com.example.util.SunCalculator
import com.example.util.SystemInfoCollector
import com.example.widget.WidgetCanvasRenderer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun appName_isWidgetLite() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("WidgetLite", appName)
    }

    @Test
    fun all39WidgetTypes_renderToBitmapWithoutCrashing() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val telemetry = SystemInfoCollector.collect(context)
        assertEquals(39, WidgetType.entries.size)

        WidgetType.entries.forEach { type ->
            val cfg = WidgetConfig.createDefault(type)
            val bmp = WidgetCanvasRenderer.render(context, cfg, telemetry, 260, 180)
            assertTrue("Expected positive bitmap width for ${type.id}", bmp.width > 0)
            assertTrue("Expected positive bitmap height for ${type.id}", bmp.height > 0)
            bmp.recycle()
        }
    }

    @Test
    fun payloadCodec_roundTrips() {
        val original = WidgetPayloadCodec.defaultFor(WidgetType.TODO_LIST)
        val json = WidgetPayloadCodec.encode(original)
        val decoded = WidgetPayloadCodec.decode(json)
        assertEquals(original.todos, decoded.todos)
    }

    @Test
    fun qrAndSunUtilities_produceValidOutput() {
        val qr = QrCodeGenerator.encode("https://widgetlite.org")
        assertTrue(qr.size >= 21)
        val sun = SunCalculator.calculate(37.7749, -122.4194)
        assertTrue(sun.sunriseText.contains(":"))
        assertTrue(sun.sunsetText.contains(":"))
    }
}
