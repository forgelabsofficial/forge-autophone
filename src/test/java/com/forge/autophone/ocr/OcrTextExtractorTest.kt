package com.forge.autophone.ocr

import android.graphics.Bitmap
import android.graphics.Rect
import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for OcrTextExtractor.
 */
class OcrTextExtractorTest {

    @Test
    fun `getCenterPoint returns correct coordinates`() {
        val block = OcrTextBlock(
            text = "Test",
            bounds = Rect(100, 200, 300, 400),
            confidence = 0.95f
        )

        val (x, y) = OcrTextExtractor().getCenterPoint(block)

        assertEquals(200f, x, 0.1f)
        assertEquals(300f, y, 0.1f)
    }

    @Test
    fun `OcrTextBlock centerX and centerY properties work correctly`() {
        val block = OcrTextBlock(
            text = "Button",
            bounds = Rect(50, 100, 150, 200),
            confidence = 0.9f
        )

        assertEquals(100f, block.centerX, 0.1f)
        assertEquals(150f, block.centerY, 0.1f)
    }

    @Test
    fun `OcrTextBlock with lines preserves hierarchy`() {
        val line1 = OcrTextLine("Line 1", Rect(0, 0, 100, 50), 0.95f)
        val line2 = OcrTextLine("Line 2", Rect(0, 50, 100, 100), 0.92f)
        
        val block = OcrTextBlock(
            text = "Line 1\nLine 2",
            bounds = Rect(0, 0, 100, 100),
            confidence = 0.93f,
            lines = listOf(line1, line2)
        )

        assertEquals(2, block.lines.size)
        assertEquals("Line 1", block.lines[0].text)
        assertEquals("Line 2", block.lines[1].text)
    }
}
