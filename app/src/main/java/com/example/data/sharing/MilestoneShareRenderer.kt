package com.example.data.sharing

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.util.Log
import androidx.core.content.FileProvider
import com.example.R
import com.example.data.model.ActivityRecord
import com.example.data.model.DistanceUnit
import com.example.data.model.MilestoneRecord
import com.example.data.model.SummitFormatters
import java.io.File
import java.io.FileOutputStream

private const val TAG = "MilestoneShareRenderer"

object MilestoneShareRenderer {

    fun shareMilestone(
        context: Context,
        milestone: MilestoneRecord,
        athleteName: String
    ): Result<Unit> {
        return try {
            val bitmap = renderCardBitmap(
                context = context,
                eyebrow = "SUMMITPACE MILESTONE UNLOCKED",
                title = milestone.title,
                metricBig = milestone.metricHighlight,
                subtitle = milestone.subtitle,
                footerLeft = athleteName.ifBlank { "Trail Explorer" },
                footerRight = SummitFormatters.formatDate(milestone.unlockedAtEpochMs)
            )
            val shareText = "Unlocked \"${milestone.title}\" (${milestone.metricHighlight}) on SummitPace! ${milestone.subtitle}"
            launchShareIntent(context, bitmap, "milestone_${milestone.badgeKey}.png", shareText)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to share milestone card", e)
            Result.failure(e)
        }
    }

    fun shareActivity(
        context: Context,
        activity: ActivityRecord,
        unit: DistanceUnit,
        athleteName: String
    ): Result<Unit> {
        return try {
            val dist = SummitFormatters.formatDistance(activity.distanceMiles, unit)
            val pace = SummitFormatters.formatPace(activity.avgPaceSecPerMile, unit)
            val elev = SummitFormatters.formatElevation(activity.elevationGainFeet, unit)
            val bitmap = renderCardBitmap(
                context = context,
                eyebrow = "SUMMITPACE ${activity.typeEnum().displayName.uppercase()} RECAP",
                title = activity.title,
                metricBig = "$dist · $pace",
                subtitle = "Elevation $elev · Duration ${SummitFormatters.formatDuration(activity.durationSeconds)}",
                footerLeft = athleteName.ifBlank { "Trail Explorer" },
                footerRight = SummitFormatters.formatDate(activity.startedAtEpochMs)
            )
            val shareText = "Just finished ${activity.title} on SummitPace: $dist at $pace ($elev)! #SummitPace"
            launchShareIntent(context, bitmap, "activity_${activity.id.ifBlank { "recap" }}.png", shareText)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to share activity card", e)
            Result.failure(e)
        }
    }

    private fun renderCardBitmap(
        context: Context,
        eyebrow: String,
        title: String,
        metricBig: String,
        subtitle: String,
        footerLeft: String,
        footerRight: String
    ): Bitmap {
        val width = 1080
        val height = 1350
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Deep Alpine Pine backdrop gradient
        val bgPaint = Paint().apply {
            shader = LinearGradient(
                0f, 0f, width.toFloat(), height.toFloat(),
                intArrayOf(
                    Color.parseColor("#112D23"),
                    Color.parseColor("#193F32"),
                    Color.parseColor("#0F241C")
                ),
                null,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Crest artwork in upper half
        val crestBmp = BitmapFactory.decodeResource(context.resources, R.drawable.img_milestone_crest_1795302665933)
        if (crestBmp != null) {
            val destRect = Rect(60, 120, width - 60, 740)
            val imgPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            canvas.drawBitmap(crestBmp, null, destRect, imgPaint)
        }

        // Subtle warm border frame
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#44E9C46A")
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }
        canvas.drawRoundRect(RectF(36f, 36f, width - 36f, height - 36f), 42f, 42f, borderPaint)

        // Eyebrow pill
        val eyebrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F2A65A")
            textSize = 32f
            letterSpacing = 0.14f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(eyebrow, width / 2f, 110f, eyebrowPaint)

        // Milestone Title
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FFF8F0")
            textSize = 72f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        drawWrappedCenteredText(canvas, title, width / 2f, 835f, width - 160f, titlePaint, 84f)

        // Metric Highlight Pill
        val pillRect = RectF(160f, 950f, width - 160f, 1065f)
        val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#D95D2B")
        }
        canvas.drawRoundRect(pillRect, 56f, 56f, pillPaint)

        val metricPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 48f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(metricBig, width / 2f, 1024f, metricPaint)

        // Encouraging Subtitle
        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#D4E4DC")
            textSize = 36f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
        drawWrappedCenteredText(canvas, subtitle, width / 2f, 1145f, width - 180f, subtitlePaint, 48f)

        // Footer divider & athlete signature
        val divPaint = Paint().apply {
            color = Color.parseColor("#33FFFFFF")
            strokeWidth = 2f
        }
        canvas.drawLine(90f, 1245f, width - 90f, 1245f, divPaint)

        val footerLeftPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FFE4B5")
            textSize = 32f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.LEFT
        }
        canvas.drawText(footerLeft, 92f, 1298f, footerLeftPaint)

        val footerRightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#A7C4B8")
            textSize = 30f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText(footerRight, width - 92f, 1298f, footerRightPaint)

        return bitmap
    }

    private fun drawWrappedCenteredText(
        canvas: Canvas,
        text: String,
        centerX: Float,
        startY: Float,
        maxWidth: Float,
        paint: Paint,
        lineHeight: Float
    ) {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var current = ""
        for (word in words) {
            val candidate = if (current.isEmpty()) word else "$current $word"
            if (paint.measureText(candidate) <= maxWidth) {
                current = candidate
            } else {
                if (current.isNotEmpty()) lines += current
                current = word
            }
        }
        if (current.isNotEmpty()) lines += current

        lines.take(2).forEachIndexed { index, line ->
            canvas.drawText(line, centerX, startY + index * lineHeight, paint)
        }
    }

    private fun launchShareIntent(
        context: Context,
        bitmap: Bitmap,
        fileName: String,
        shareText: String
    ) {
        val dir = File(context.cacheDir, "shared_milestones").apply { if (!exists()) mkdirs() }
        val file = File(dir, fileName)
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 95, out)
        }
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, shareText)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, "Share Milestone Card").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}
