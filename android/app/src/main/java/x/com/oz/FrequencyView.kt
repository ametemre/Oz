package x.com.oz

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.math.PI
import kotlin.math.sin

class FrequencyView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    private val paint = Paint().apply {
        color = Color.GREEN
        strokeWidth = 4f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }

    var frequency: Float = 440f
        set(value) {
            field = value
            invalidate() // frekans değiştiğinde yeniden çiz
        }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val width = width.toFloat()
        val height = height.toFloat()
        val centerY = height / 2

        val amplitude = height / 4
        val waveLength = width / (frequency / 10f) // frekans büyüdükçe dalgalar sıklaşır

        var prevX = 0f
        var prevY = centerY

        var x = 0f
        while (x <= width) {
            val y = centerY + amplitude * sin(2 * PI * x / waveLength).toFloat()
            canvas.drawLine(prevX, prevY, x, y, paint)
            prevX = x
            prevY = y
            x += 5f
        }
    }
}
