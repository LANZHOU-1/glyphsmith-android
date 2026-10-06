package com.lanzhou.zj

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import kotlin.math.max
import kotlin.math.roundToInt

enum class Mode { CHINESE, ASCII, COLOR }

data class Params(
    val mode: Mode = Mode.CHINESE,
    val width: Int = 90,
    val contrast: Float = 1.0f,
    val autoLevel: Boolean = true,
    val invert: Boolean = false,
    val backgroundBlack: Boolean = true,
    val blockStyle: Boolean = false,
    val fontSize: Int = 11,
)

sealed interface ArtResult {
    val cols: Int
    val rows: Int

    data class Plain(
        val text: String,
        override val cols: Int,
        override val rows: Int,
    ) : ArtResult

    data class Colored(
        val annotated: AnnotatedString,
        val html: String,
        val cells: String,
        val colors: IntArray,
        override val cols: Int,
        override val rows: Int,
    ) : ArtResult
}

object Converter {
    private const val CHINESE_RAMP = " 丶一十口日目田回無疆龘"
    private const val ASCII_RAMP = " .:-=+*#%@"
    private const val HEX_DIGITS = "0123456789abcdef"

    fun convert(source: Bitmap, params: Params): ArtResult {
        val cols = params.width.coerceIn(8, 400)
        val aspect = if (params.mode == Mode.CHINESE) 1.0f else 0.5f
        val rows = (cols * source.height.toFloat() / source.width * aspect)
            .roundToInt().coerceIn(1, 600)
        val scaled = Bitmap.createScaledBitmap(source, cols, rows, true)
        val pixels = IntArray(cols * rows)
        scaled.getPixels(pixels, 0, cols, 0, 0, cols, rows)
        if (scaled !== source) scaled.recycle()

        val lum = FloatArray(pixels.size)
        for (i in pixels.indices) {
            val c = pixels[i]
            lum[i] = 0.2126f * ((c shr 16) and 0xFF) +
                0.7152f * ((c shr 8) and 0xFF) +
                0.0722f * (c and 0xFF)
        }
        if (params.contrast != 1.0f) {
            var mean = 0f
            for (v in lum) mean += v
            mean /= lum.size
            for (i in lum.indices) {
                lum[i] = (mean + (lum[i] - mean) * params.contrast).coerceIn(0f, 255f)
            }
        }
        if (params.autoLevel) autoContrast(lum)

        return when (params.mode) {
            Mode.CHINESE -> buildPlain(lum, cols, rows, CHINESE_RAMP, params.invert)
            Mode.ASCII -> buildPlain(lum, cols, rows, ASCII_RAMP, params.invert)
            Mode.COLOR -> buildColored(pixels, lum, cols, rows, params)
        }
    }

    private fun autoContrast(lum: FloatArray) {
        val hist = IntArray(256)
        for (v in lum) hist[v.roundToInt().coerceIn(0, 255)]++
        val cutoff = max(1, (lum.size * 0.01f).roundToInt())
        var lo = 0
        var acc = 0
        for (v in 0..255) {
            acc += hist[v]
            if (acc >= cutoff) {
                lo = v
                break
            }
        }
        var hi = 255
        acc = 0
        for (v in 255 downTo 0) {
            acc += hist[v]
            if (acc >= cutoff) {
                hi = v
                break
            }
        }
        if (hi - lo < 8) return
        val scale = 255f / (hi - lo)
        for (i in lum.indices) lum[i] = ((lum[i] - lo) * scale).coerceIn(0f, 255f)
    }

    private fun charFor(value: Float, ramp: String, invert: Boolean): Char {
        val levels = ramp.length - 1
        val k = (if (invert) value else 255f - value) / 255f
        return ramp[(k * levels).roundToInt().coerceIn(0, levels)]
    }

    private fun buildPlain(
        lum: FloatArray,
        cols: Int,
        rows: Int,
        ramp: String,
        invert: Boolean,
    ): ArtResult.Plain {
        val sb = StringBuilder(cols * (rows + 1))
        for (y in 0 until rows) {
            for (x in 0 until cols) sb.append(charFor(lum[y * cols + x], ramp, invert))
            sb.append('\n')
        }
        return ArtResult.Plain(sb.toString(), cols, rows)
    }

    private fun buildColored(
        pixels: IntArray,
        lum: FloatArray,
        cols: Int,
        rows: Int,
        params: Params,
    ): ArtResult.Colored {
        val invert = params.backgroundBlack
        val builder = AnnotatedString.Builder()
        val html = StringBuilder()
        val cells = StringBuilder(cols * rows)
        val colors = IntArray(cols * rows)
        for (y in 0 until rows) {
            for (x in 0 until cols) {
                val i = y * cols + x
                val ch = if (params.blockStyle) '\u2588' else charFor(lum[i], ASCII_RAMP, invert)
                cells.append(ch)
                colors[i] = pixels[i]
                val start = builder.length
                builder.append(ch)
                if (ch == ' ') {
                    html.append(' ')
                } else {
                    builder.addStyle(SpanStyle(color = Color(pixels[i])), start, start + 1)
                    html.append("<span style=\"color:#")
                        .append(hex(pixels[i]))
                        .append("\">")
                        .append(ch)
                        .append("</span>")
                }
            }
            builder.append('\n')
            html.append('\n')
        }
        return ArtResult.Colored(
            builder.toAnnotatedString(),
            html.toString(),
            cells.toString(),
            colors,
            cols,
            rows,
        )
    }

    fun htmlPage(body: String, backgroundBlack: Boolean, fontSize: Int): String {
        val bg = if (backgroundBlack) "#000000" else "#ffffff"
        return buildString {
            append("<!doctype html>\n<html lang=\"zh-CN\">\n<head>\n<meta charset=\"utf-8\">\n")
            append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n")
            append("<title>Glyphsmith </title>\n<style>\n")
            append("html,body{margin:0;padding:0;}\n")
            append("body{background:").append(bg)
            append(";display:flex;justify-content:center;padding:24px;}\n")
            append("pre{font-family:\"Cascadia Mono\",\"Consolas\",\"Sarasa Mono SC\",monospace;font-size:")
            append(fontSize).append("px;line-height:1;letter-spacing:0;white-space:pre;}\n")
            append("</style>\n</head>\n<body>\n<pre>").append(body).append("</pre>\n</body>\n</html>\n")
        }
    }

    private fun hex(color: Int): String {
        val r = (color shr 16) and 0xFF
        val g = (color shr 8) and 0xFF
        val b = color and 0xFF
        val out = CharArray(6)
        out[0] = HEX_DIGITS[r shr 4]
        out[1] = HEX_DIGITS[r and 0xF]
        out[2] = HEX_DIGITS[g shr 4]
        out[3] = HEX_DIGITS[g and 0xF]
        out[4] = HEX_DIGITS[b shr 4]
        out[5] = HEX_DIGITS[b and 0xF]
        return String(out)
    }

    fun makeSample(): Bitmap {
        val w = 900
        val h = 506
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        paint.shader = LinearGradient(
            0f, 0f, 0f, 314f,
            intArrayOf(
                0xFF0A1230.toInt(), 0xFF62398A.toInt(), 0xFFD66258.toInt(),
                0xFFFF9E54.toInt(), 0xFFFFD898.toInt(),
            ),
            floatArrayOf(0f, 0.45f, 0.68f, 0.88f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, w.toFloat(), 314f, paint)

        paint.shader = RadialGradient(
            648f, 202f, 190f,
            intArrayOf(0xE6FFBE6E.toInt(), 0x00FFBE6E.toInt()),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(648f, 202f, 190f, paint)

        paint.shader = null
        paint.color = 0xFFFFF3D0.toInt()
        canvas.drawCircle(648f, 202f, 47f, paint)

        paint.color = 0xFF60507A.toInt()
        val far = Path()
        far.moveTo(0f, 314f)
        far.lineTo(90f, 237f)
        far.lineTo(180f, 283f)
        far.lineTo(306f, 202f)
        far.lineTo(414f, 278f)
        far.lineTo(522f, 222f)
        far.lineTo(630f, 288f)
        far.lineTo(756f, 232f)
        far.lineTo(864f, 278f)
        far.lineTo(900f, 314f)
        far.close()
        canvas.drawPath(far, paint)

        paint.color = 0xFF22203A.toInt()
        val near = Path()
        near.moveTo(0f, 314f)
        near.lineTo(144f, 268f)
        near.lineTo(270f, 314f)
        near.lineTo(450f, 152f)
        near.lineTo(612f, 314f)
        near.lineTo(738f, 253f)
        near.lineTo(900f, 314f)
        near.close()
        canvas.drawPath(near, paint)

        paint.color = 0xFFF6D6BE.toInt()
        val snow = Path()
        snow.moveTo(450f, 152f)
        snow.lineTo(392f, 195f)
        snow.lineTo(416f, 188f)
        snow.lineTo(430f, 198f)
        snow.lineTo(450f, 186f)
        snow.lineTo(470f, 198f)
        snow.lineTo(484f, 188f)
        snow.lineTo(508f, 195f)
        snow.close()
        canvas.drawPath(snow, paint)

        paint.shader = LinearGradient(
            0f, 314f, 0f, h.toFloat(),
            intArrayOf(0xFFFFB478.toInt(), 0xFFA66874.toInt(), 0xFF10142E.toInt()),
            floatArrayOf(0f, 0.3f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 314f, w.toFloat(), h.toFloat(), paint)

        paint.shader = null
        paint.color = 0x8CFFD696.toInt()
        val reflection = Path()
        reflection.moveTo(578f, 314f)
        reflection.lineTo(718f, 314f)
        reflection.lineTo(664f, 506f)
        reflection.lineTo(632f, 506f)
        reflection.close()
        canvas.drawPath(reflection, paint)

        return bmp
    }
}
