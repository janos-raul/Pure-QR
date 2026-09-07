package com.pureqr.app.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import androidx.core.graphics.createBitmap
import androidx.core.graphics.set
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.pureqr.app.model.ContactData
import com.pureqr.app.model.QrFrame
import com.pureqr.app.model.QrType
import com.pureqr.app.model.WifiData

object QrGenerator {

    fun generateQrCode(
        content: String,
        sizePx: Int,
        foregroundColor: Int = Color.BLACK,
        backgroundColor: Int = Color.WHITE,
        frameType: QrFrame = QrFrame.ROUNDED,
        frameColor: Int = Color.BLACK,
        qrType: QrType = QrType.TEXT
    ): Bitmap? {
        if (content.isEmpty()) return null
        
        return try {
            val writer = QRCodeWriter()
            
            val hints = mutableMapOf<EncodeHintType, Any>(
                EncodeHintType.MARGIN to 1,
                EncodeHintType.CHARACTER_SET to "UTF-8"
            )
            
            val needsOverlay = qrType == QrType.WIFI || qrType == QrType.CONTACT || qrType == QrType.CRYPTO
            
            if (needsOverlay) {
                // Use Level H (30% recovery) to compensate for the center overlay and ensure complex data like V-Cards scan correctly
                hints[EncodeHintType.ERROR_CORRECTION] = ErrorCorrectionLevel.H
            }
            
            val hasFrame = frameType != QrFrame.NONE
            val framePadding = if (hasFrame) (sizePx * 0.15f).toInt() else (sizePx * 0.05f).toInt()
            val qrSize = sizePx - (framePadding * 2)
            
            val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, qrSize, qrSize, hints)
            
            val bitmap = createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            
            drawBackground(canvas, sizePx.toFloat(), sizePx.toFloat(), backgroundColor, frameType)

            if (hasFrame) {
                val borderMargin = sizePx * 0.05f
                val rect = RectF(borderMargin, borderMargin, sizePx - borderMargin, sizePx - borderMargin)
                drawGenericFrame(canvas, rect, frameType, frameColor, sizePx.toFloat())
            }

            for (x in 0 until qrSize) {
                for (y in 0 until qrSize) {
                    if (bitMatrix.get(x, y)) {
                        val pixelX = x + framePadding
                        val pixelY = y + framePadding
                        if (pixelX < sizePx && pixelY < sizePx) {
                            bitmap[pixelX, pixelY] = foregroundColor
                        }
                    }
                }
            }

            if (needsOverlay) {
                drawCenterOverlay(canvas, sizePx.toFloat(), foregroundColor, backgroundColor, qrType)
            }
            
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun drawCenterOverlay(canvas: Canvas, size: Float, color: Int, bgColor: Int, qrType: QrType) {
        val center = size / 2f
        val overlaySize = size * 0.18f
        val halfOverlay = overlaySize / 2f
        
        val clearPaint = Paint().apply {
            this.color = bgColor
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        val overlayRect = RectF(center - halfOverlay, center - halfOverlay, center + halfOverlay, center + halfOverlay)
        canvas.drawRoundRect(overlayRect, 12f, 12f, clearPaint)

        val paint = Paint().apply {
            this.color = color
            isAntiAlias = true
            strokeWidth = size * 0.008f
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
        }

        val textPaint = Paint().apply {
            this.color = color
            isAntiAlias = true
            textSize = overlaySize * 0.22f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        when (qrType) {
            QrType.WIFI -> {
                val iconSize = overlaySize * 0.45f
                val iconTop = center - halfOverlay + (overlaySize * 0.12f)
                paint.style = Paint.Style.FILL
                canvas.drawCircle(center, iconTop + iconSize * 0.85f, size * 0.006f, paint)
                paint.style = Paint.Style.STROKE
                val arcRect = RectF()
                for (i in 1..3) {
                    val r = (iconSize * 0.28f) * i
                    val baseY = iconTop + iconSize * 0.85f
                    arcRect.set(center - r, baseY - r, center + r, baseY + r)
                    canvas.drawArc(arcRect, -135f, 90f, false, paint)
                }
                canvas.drawText("Wi-Fi", center, center + halfOverlay - (overlaySize * 0.12f), textPaint)
            }
            QrType.CONTACT -> {
                val iconSize = overlaySize * 0.45f
                val iconTop = center - halfOverlay + (overlaySize * 0.15f)
                paint.style = Paint.Style.STROKE
                canvas.drawCircle(center, iconTop + iconSize * 0.3f, iconSize * 0.25f, paint)
                val bodyRect = RectF(center - iconSize * 0.4f, iconTop + iconSize * 0.6f, center + iconSize * 0.4f, iconTop + iconSize * 0.9f)
                canvas.drawArc(bodyRect, 180f, 180f, false, paint)
                canvas.drawText("V-Card", center, center + halfOverlay - (overlaySize * 0.12f), textPaint)
            }
            QrType.CRYPTO -> {
                val iconSize = overlaySize * 0.45f
                val iconTop = center - halfOverlay + (overlaySize * 0.15f)
                paint.style = Paint.Style.STROKE
                canvas.drawCircle(center, iconTop + iconSize * 0.4f, iconSize * 0.65f, paint)
                paint.style = Paint.Style.FILL
                canvas.drawText("₿", center, iconTop + iconSize * 0.65f, textPaint.apply { textSize = overlaySize * 0.4f })
                canvas.drawText("Crypto", center, center + halfOverlay - (overlaySize * 0.12f), textPaint.apply { textSize = overlaySize * 0.22f })
            }
            else -> {}
        }
    }

    private fun drawBackground(canvas: Canvas, width: Float, height: Float, backgroundColor: Int, frameType: QrFrame) {
        val paint = Paint().apply {
            color = backgroundColor
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        when (frameType) {
            QrFrame.ROUNDED, QrFrame.THICK_ROUNDED, QrFrame.DOUBLE_BORDER, QrFrame.DOTS -> {
                canvas.drawRoundRect(RectF(0f, 0f, width, height), 50f, 50f, paint)
            }
            else -> {
                canvas.drawColor(backgroundColor)
            }
        }
    }

    private fun drawGenericFrame(canvas: Canvas, rect: RectF, frameType: QrFrame, frameColor: Int, referenceSize: Float) {
        val paint = Paint().apply {
            color = frameColor
            style = Paint.Style.STROKE
            isAntiAlias = true
        }
        when (frameType) {
            QrFrame.ROUNDED -> {
                paint.strokeWidth = (referenceSize * 0.015f).coerceAtLeast(6f)
                canvas.drawRoundRect(rect, 40f, 40f, paint)
            }
            QrFrame.THICK_ROUNDED -> {
                paint.strokeWidth = (referenceSize * 0.04f).coerceAtLeast(12f)
                canvas.drawRoundRect(rect, 50f, 50f, paint)
            }
            QrFrame.DOUBLE_BORDER -> {
                paint.strokeWidth = (referenceSize * 0.01f).coerceAtLeast(4f)
                canvas.drawRoundRect(rect, 40f, 40f, paint)
                val innerMargin = referenceSize * 0.025f
                val innerRect = RectF(rect.left + innerMargin, rect.top + innerMargin, rect.right - innerMargin, rect.bottom - innerMargin)
                canvas.drawRoundRect(innerRect, 30f, 30f, paint)
            }
            QrFrame.DOTS -> {
                paint.strokeWidth = (referenceSize * 0.015f).coerceAtLeast(6f)
                paint.pathEffect = DashPathEffect(floatArrayOf(20f, 20f), 0f)
                canvas.drawRoundRect(rect, 40f, 40f, paint)
            }
            QrFrame.MODERN -> {
                paint.strokeWidth = (referenceSize * 0.02f).coerceAtLeast(8f)
                val cornerLength = referenceSize * 0.2f
                canvas.drawLine(rect.left, rect.top, rect.left + cornerLength, rect.top, paint)
                canvas.drawLine(rect.left, rect.top, rect.left, rect.top + cornerLength, paint)
                canvas.drawLine(rect.right, rect.top, rect.right - cornerLength, rect.top, paint)
                canvas.drawLine(rect.right, rect.top, rect.right, rect.top + cornerLength, paint)
                canvas.drawLine(rect.left, rect.bottom, rect.left + cornerLength, rect.bottom, paint)
                canvas.drawLine(rect.left, rect.bottom, rect.left, rect.bottom - cornerLength, paint)
                canvas.drawLine(rect.right, rect.bottom, rect.right - cornerLength, rect.bottom, paint)
                canvas.drawLine(rect.right, rect.bottom, rect.right, rect.bottom - cornerLength, paint)
            }
            QrFrame.NONE -> {}
        }
    }

    fun generateBarcode(
        content: String,
        width: Int,
        height: Int,
        foregroundColor: Int = Color.BLACK,
        backgroundColor: Int = Color.WHITE,
        frameType: QrFrame = QrFrame.ROUNDED,
        frameColor: Int = Color.BLACK
    ): Bitmap? {
        if (content.isEmpty()) return null
        return try {
            val writer = MultiFormatWriter()
            val textAreaHeight = 60
            val verticalPadding = 60
            val horizontalPadding = 60
            val finalWidth = width + (horizontalPadding * 2)
            val finalHeight = height + textAreaHeight + (verticalPadding * 2)
            val bitMatrix = writer.encode(content, BarcodeFormat.CODE_128, width, height)
            val bitmap = createBitmap(finalWidth, finalHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawBackground(canvas, finalWidth.toFloat(), finalHeight.toFloat(), backgroundColor, frameType)
            if (frameType != QrFrame.NONE) {
                val borderMargin = 15f
                val rect = RectF(borderMargin, borderMargin, finalWidth - borderMargin, finalHeight - borderMargin)
                drawGenericFrame(canvas, rect, frameType, frameColor, finalHeight.toFloat())
            }
            for (x in 0 until width) {
                for (y in 0 until height) {
                    if (bitMatrix.get(x, y)) {
                        bitmap[x + horizontalPadding, y + verticalPadding] = foregroundColor
                    }
                }
            }
            val textPaint = Paint().apply {
                color = foregroundColor
                textSize = 35f
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
                typeface = Typeface.MONOSPACE
            }
            canvas.drawText(content, (finalWidth / 2).toFloat(), (verticalPadding + height + 50).toFloat(), textPaint)
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun formatWifiContent(data: WifiData): String {
        return "WIFI:S:${data.ssid};T:${data.encryption};P:${data.password};;"
    }

    fun formatVCardContent(data: ContactData): String {
        val builder = StringBuilder("BEGIN:VCARD\r\nVERSION:3.0\r\n")
        if (data.lastName.isNotBlank() || data.firstName.isNotBlank()) {
            builder.append("N:${data.lastName};${data.firstName};;;\r\n")
            builder.append("FN:${data.firstName} ${data.lastName}\r\n")
        }
        if (data.phone.isNotBlank()) builder.append("TEL:${data.phone}\r\n")
        if (data.email.isNotBlank()) builder.append("EMAIL:${data.email}\n")
        if (data.organization.isNotBlank()) builder.append("ORG:${data.organization}\r\n")
        if (data.jobTitle.isNotBlank()) builder.append("TITLE:${data.jobTitle}\r\n")
        if (data.address.isNotBlank()) builder.append("ADR:;;${data.address};;;;\r\n")
        if (data.website.isNotBlank()) builder.append("URL:${data.website}\r\n")
        if (data.note.isNotBlank()) builder.append("NOTE:${data.note}\r\n")
        builder.append("END:VCARD")
        return builder.toString()
    }
}
