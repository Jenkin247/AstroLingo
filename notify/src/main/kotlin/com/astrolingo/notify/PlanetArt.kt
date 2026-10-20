// The calm planet pictures in the note: drawn on the phone, so there are no image files to ship or download.
// A soft night sky, a few faint stars and the day's planet glowing gently. Same picture every week for a planet.
package com.astrolingo.notify

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import kotlin.random.Random

object PlanetArt {
    /** Width and height of the wide picture shown when the note is opened out, in pixels (2:1, as Android asks). */
    const val SCENE_WIDTH = 1024
    const val SCENE_HEIGHT = 512

    /** The small round picture beside the note's title. */
    const val ICON_SIZE = 192

    /**
     * Colours for each planet, after the weekday colours on the Today card but softened for a night sky.
     *
     * @property light the lit side of the planet.
     * @property dark its shaded side.
     * @property glow the haze around it.
     */
    private data class Palette(val light: Int, val dark: Int, val glow: Int)

    private fun palette(planet: Planet): Palette = when (planet) {
        Planet.SUN -> Palette(Color.rgb(255, 214, 140), Color.rgb(232, 132, 54), Color.rgb(255, 178, 92))
        Planet.MOON -> Palette(Color.rgb(246, 244, 236), Color.rgb(176, 182, 200), Color.rgb(214, 222, 255))
        Planet.MARS -> Palette(Color.rgb(236, 140, 112), Color.rgb(150, 58, 50), Color.rgb(220, 110, 96))
        Planet.MERCURY -> Palette(Color.rgb(176, 222, 190), Color.rgb(70, 132, 108), Color.rgb(140, 210, 176))
        Planet.JUPITER -> Palette(Color.rgb(250, 226, 150), Color.rgb(196, 150, 70), Color.rgb(246, 212, 120))
        Planet.VENUS -> Palette(Color.rgb(252, 222, 232), Color.rgb(206, 140, 170), Color.rgb(250, 190, 214))
        Planet.SATURN -> Palette(Color.rgb(150, 170, 230), Color.rgb(48, 60, 126), Color.rgb(120, 140, 220))
    }

    private val SKY_TOP = Color.rgb(16, 20, 46)
    private val SKY_BOTTOM = Color.rgb(44, 38, 84)

    /** The wide picture: the planet a little right of centre, with room on the left for the eye to rest. */
    fun scene(planet: Planet): Bitmap {
        val bitmap = Bitmap.createBitmap(SCENE_WIDTH, SCENE_HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        sky(canvas, SCENE_WIDTH.toFloat(), SCENE_HEIGHT.toFloat(), planet, stars = 46)
        drawPlanet(canvas, planet, cx = SCENE_WIDTH * 0.62f, cy = SCENE_HEIGHT * 0.5f, r = SCENE_HEIGHT * 0.26f)
        return bitmap
    }

    /** The small picture: the planet alone on its sky, in a circle. */
    fun icon(planet: Planet): Bitmap {
        val size = ICON_SIZE.toFloat()
        val bitmap = Bitmap.createBitmap(ICON_SIZE, ICON_SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val clip = Path().apply { addCircle(size / 2, size / 2, size / 2, Path.Direction.CW) }
        canvas.clipPath(clip)
        sky(canvas, size, size, planet, stars = 8)
        drawPlanet(canvas, planet, cx = size / 2, cy = size / 2, r = size * 0.3f)
        return bitmap
    }

    private fun sky(canvas: Canvas, w: Float, h: Float, planet: Planet, stars: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.shader = LinearGradient(0f, 0f, 0f, h, SKY_TOP, SKY_BOTTOM, Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null
        // The same faint stars each time for a planet, so the picture feels familiar rather than busy.
        val random = Random(planet.ordinal + 1)
        repeat(stars) {
            paint.color = Color.argb(60 + random.nextInt(110), 255, 255, 255)
            canvas.drawCircle(random.nextFloat() * w, random.nextFloat() * h, 0.6f + random.nextFloat() * 1.6f, paint)
        }
    }

    private fun drawPlanet(canvas: Canvas, planet: Planet, cx: Float, cy: Float, r: Float) {
        val colours = palette(planet)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        // A soft haze, wider for the Sun.
        val haze = r * if (planet == Planet.SUN) 2.6f else 1.9f
        paint.shader = RadialGradient(cx, cy, haze, intArrayOf(withAlpha(colours.glow, 110), withAlpha(colours.glow, 0)), null, Shader.TileMode.CLAMP)
        canvas.drawCircle(cx, cy, haze, paint)
        if (planet == Planet.SATURN) ring(canvas, colours, cx, cy, r, back = true)
        // The body, lit from the upper left.
        paint.shader = RadialGradient(cx - r * 0.35f, cy - r * 0.35f, r * 1.5f, colours.light, colours.dark, Shader.TileMode.CLAMP)
        canvas.drawCircle(cx, cy, r, paint)
        paint.shader = null
        when (planet) {
            Planet.JUPITER -> bands(canvas, colours, cx, cy, r)
            Planet.MOON -> craters(canvas, colours, cx, cy, r)
            else -> Unit
        }
        if (planet == Planet.SATURN) ring(canvas, colours, cx, cy, r, back = false)
    }

    /** Saturn's ring, drawn in two halves so the planet sits inside it. */
    private fun ring(canvas: Canvas, colours: Palette, cx: Float, cy: Float, r: Float, back: Boolean) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = r * 0.12f
            color = withAlpha(colours.light, if (back) 120 else 200)
        }
        val oval = RectF(cx - r * 1.8f, cy - r * 0.45f, cx + r * 1.8f, cy + r * 0.45f)
        canvas.drawArc(oval, if (back) 180f else 0f, 180f, false, paint)
    }

    private fun bands(canvas: Canvas, colours: Palette, cx: Float, cy: Float, r: Float) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = withAlpha(colours.dark, 70) }
        canvas.save()
        val clip = Path().apply { addCircle(cx, cy, r, Path.Direction.CW) }
        canvas.clipPath(clip)
        listOf(-0.45f, -0.1f, 0.3f).forEach { at -> canvas.drawRect(cx - r, cy + r * at, cx + r, cy + r * (at + 0.12f), paint) }
        canvas.restore()
    }

    private fun craters(canvas: Canvas, colours: Palette, cx: Float, cy: Float, r: Float) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = withAlpha(colours.dark, 60) }
        canvas.drawCircle(cx - r * 0.3f, cy + r * 0.1f, r * 0.16f, paint)
        canvas.drawCircle(cx + r * 0.25f, cy - r * 0.3f, r * 0.1f, paint)
        canvas.drawCircle(cx + r * 0.35f, cy + r * 0.35f, r * 0.12f, paint)
    }

    private fun withAlpha(colour: Int, alpha: Int): Int = Color.argb(alpha, Color.red(colour), Color.green(colour), Color.blue(colour))
}
