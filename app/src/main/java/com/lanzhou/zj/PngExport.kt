package com.lanzhou.zj

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import kotlin.math.max
import kotlin.math.roundToInt


object PngExport {
    private const val MAX_EDGE = 4096f

    fun render(result: ArtResult, params: Params): Bitmap {
        val fontSize = params.fontSize.toFloat()
        val blockStyle = params.mode == Mode.COLOR && params.blockStyle
        val cellW = if (params.mode == Mode.CHINESE || blockStyle) fontSize else fontSize * 0.6f
        val cellH = fontSize
        val pad = cellH * 0.6f

        val baseW = pad * 2f + cellW * result.cols
        val baseH = pad * 2f + cellH * result.rows
        val scale = (MAX_EDGE / max(baseW, baseH)).coerceAtMost(2f)

        val width = max(1, (baseW * scale).roundToInt())
        val height = max(1, (baseH * scale).roundToInt())
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        canvas.drawColor(
            when (result) {
                is ArtResult.Colored -> if (params.backgroundBlack) Color.BLACK else Color.WHITE
                is ArtResult.Plain -> if (params.invert) 0xFF101010.toInt() else Color.WHITE
            },
        )

        val stepX = cellW * scale
        val stepY = cellH * scale
        val origin = pad * scale
        paint.textSize = fontSize * scale
        paint.typeface = if (params.mode == Mode.CHINESE) Typeface.DEFAULT else Typeface.MONOSPACE
        val baseline = stepY / 2f - (paint.descent() + paint.ascent()) / 2f

        when (result) {
            is ArtResult.Plain -> {
                paint.color = if (params.invert) 0xFFF2F2F2.toInt() else 0xFF111111.toInt()
                result.text.lineSequence().take(result.rows).forEachIndexed { y, line ->
                    val y0 = origin + y * stepY + baseline
                    line.forEachIndexed { x, ch ->
                        if (x >= result.cols || ch == ' ') return@forEachIndexed
                        val glyph = ch.toString()
                        val x0 = origin + x * stepX + (stepX - paint.measureText(glyph)) / 2f
                        canvas.drawText(glyph, x0, y0, paint)
                    }
                }
            }
            is ArtResult.Colored -> {
                if (blockStyle) {
                    paint.style = Paint.Style.FILL
                    paint.isAntiAlias = false
                    for (y in 0 until result.rows) {
                        val top = (origin + y * stepY).roundToInt().toFloat()
                        val bottom = (origin + (y + 1) * stepY).roundToInt().toFloat()
                        for (x in 0 until result.cols) {
                            paint.color = result.colors[y * result.cols + x]
                            val left = (origin + x * stepX).roundToInt().toFloat()
                            val right = (origin + (x + 1) * stepX).roundToInt().toFloat()
                            canvas.drawRect(left, top, right, bottom, paint)
                        }
                    }
                    paint.isAntiAlias = true
                } else {
                    val cells = result.cells
                    for (y in 0 until result.rows) {
                        val y0 = origin + y * stepY + baseline
                        for (x in 0 until result.cols) {
                            val i = y * result.cols + x
                            val ch = cells[i]
                            if (ch == ' ') continue
                            paint.color = result.colors[i]
                            val glyph = ch.toString()
                            val x0 = origin + x * stepX + (stepX - paint.measureText(glyph)) / 2f
                            canvas.drawText(glyph, x0, y0, paint)
                        }
                    }
                }
            }
        }
        return bitmap
    }
}
