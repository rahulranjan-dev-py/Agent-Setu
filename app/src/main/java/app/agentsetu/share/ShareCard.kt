package app.agentsetu.share

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import app.agentsetu.BuildConfig
import java.io.File

/**
 * Draws a simple navy/white card with a sea-green divider (title, body lines, footer) as a PNG and opens the share sheet,
 * for WhatsApp. Uses the system font, which covers Hindi. No logos or Agent Setu marks.
 */
object ShareCard {
    private const val WIDTH = 1080
    private const val PAD = 64
    private val NAVY = Color.rgb(0x1F, 0x4E, 0x79)
    private val SEA_GREEN = Color.rgb(0x2A, 0x9D, 0x8F)
    private val INK = Color.rgb(0x1C, 0x1B, 0x1F)
    private val MUTED = Color.rgb(0x5F, 0x5E, 0x66)

    fun share(context: Context, title: String, body: List<String>, footer: List<String>) {
        val bitmap = render(title, body, footer)
        val dir = File(context.cacheDir, "share").apply { mkdirs() }
        val file = File(dir, "agent-setu-card.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        val uri = FileProvider.getUriForFile(context, "${BuildConfig.APPLICATION_ID}.fileprovider", file)
        val send = Intent(Intent.ACTION_SEND)
            .setType("image/png")
            .putExtra(Intent.EXTRA_STREAM, uri)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        try {
            context.startActivity(Intent.createChooser(send, null))
        } catch (_: ActivityNotFoundException) {
        }
    }

    private fun render(title: String, body: List<String>, footer: List<String>): Bitmap {
        val contentWidth = WIDTH - 2 * PAD
        val titlePaint = paint(58f, Color.WHITE, bold = true)
        val bodyPaint = paint(40f, INK)
        val footerPaint = paint(30f, MUTED)

        val titleLayout = layout(title, titlePaint, contentWidth)
        val bodyLayouts = body.map { layout("•  $it", bodyPaint, contentWidth) }
        val footerLayouts = footer.map { layout(it, footerPaint, contentWidth) }

        val headerHeight = titleLayout.height + 2 * PAD
        val bodyHeight = bodyLayouts.sumOf { it.height + 24 }
        val footerHeight = footerLayouts.sumOf { it.height + 12 }
        val height = headerHeight + PAD + bodyHeight + PAD + footerHeight + PAD

        val bitmap = Bitmap.createBitmap(WIDTH, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        canvas.drawRect(0f, 0f, WIDTH.toFloat(), headerHeight.toFloat(), Paint().apply { color = NAVY })

        var y = PAD.toFloat()
        y = draw(canvas, titleLayout, y) + PAD * 2
        bodyLayouts.forEach { y = draw(canvas, it, y) + 24 }
        y += PAD
        canvas.drawLine(
            PAD.toFloat(), y - PAD / 2, (WIDTH - PAD).toFloat(), y - PAD / 2,
            Paint().apply { color = SEA_GREEN; strokeWidth = 3f },
        )
        footerLayouts.forEach { y = draw(canvas, it, y) + 12 }
        return bitmap
    }

    private fun paint(size: Float, color: Int, bold: Boolean = false) = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = size
        this.color = color
        typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
    }

    private fun layout(text: String, paint: TextPaint, width: Int): StaticLayout =
        StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(0f, 1.15f)
            .build()

    private fun draw(canvas: Canvas, layout: StaticLayout, top: Float): Float {
        canvas.save()
        canvas.translate(PAD.toFloat(), top)
        layout.draw(canvas)
        canvas.restore()
        return top + layout.height
    }
}
