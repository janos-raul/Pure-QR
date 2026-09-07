package com.pureqr.app.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintManager
import android.provider.MediaStore
//import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

object PdfExporter {

    fun printPdf(context: Context, uri: Uri, jobName: String) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
        try {
            printManager.print(jobName, object : PrintDocumentAdapter() {
                override fun onWrite(
                    pages: Array<out PageRange>?,
                    destination: ParcelFileDescriptor?,
                    cancellationSignal: CancellationSignal?,
                    callback: WriteResultCallback?
                ) {
                    var input: InputStream? = null
                    var output: OutputStream? = null
                    try {
                        input = context.contentResolver.openInputStream(uri)
                        output = FileOutputStream(destination?.fileDescriptor)
                        val buf = ByteArray(1024)
                        var bytesRead: Int
                        while (input?.read(buf).also { bytesRead = it ?: -1 }!! > 0) {
                            output.write(buf, 0, bytesRead)
                        }
                        callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                    } catch (e: Exception) {
                        callback?.onWriteFailed(e.message)
                    } finally {
                        try {
                            input?.close()
                            output?.close()
                        } catch (e: IOException) {
                        }
                    }
                }

                override fun onLayout(
                    oldAttributes: PrintAttributes?,
                    newAttributes: PrintAttributes?,
                    cancellationSignal: CancellationSignal?,
                    callback: LayoutResultCallback?,
                    extras: Bundle?
                ) {
                    if (cancellationSignal?.isCanceled == true) {
                        callback?.onLayoutCancelled()
                        return
                    }
                    val pdi = android.print.PrintDocumentInfo.Builder(jobName)
                        .setContentType(android.print.PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                        .build()
                    callback?.onLayoutFinished(pdi, true)
                }
            }, null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun generateQrPdf(context: Context, bitmap: Bitmap, title: String?, fileName: String): Uri? {
        val pdfDocument = PdfDocument()
        
        // A4 size in points (1/72 inch)
        val pageWidth = 595
        val pageHeight = 842
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        // Draw Title if exists
        title?.let {
            val paint = Paint().apply {
                textSize = 24f
                isFakeBoldText = true
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(it, pageWidth / 2f, 80f, paint)
        }

        // Draw QR Bitmap centered
        val qrSize = (pageWidth * 0.7f) // 70% of page width
        val left = (pageWidth - qrSize) / 2f
        val top = (pageHeight - qrSize) / 2f
        val destRect = RectF(left, top, left + qrSize, top + qrSize)
        canvas.drawBitmap(bitmap, null, destRect, null)

        pdfDocument.finishPage(page)

        val resolver = context.contentResolver
        val contentCollection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Files.getContentUri("external")
        }

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "$fileName.pdf")
            put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS + "/Pure-QR")
            }
        }

        val pdfUri = resolver.insert(contentCollection, contentValues) ?: return null

        return try {
            val outputStream: OutputStream? = resolver.openOutputStream(pdfUri)
            outputStream?.use {
                pdfDocument.writeTo(it)
            }
            pdfDocument.close()
            pdfUri
        } catch (e: Exception) {
            e.printStackTrace()
            resolver.delete(pdfUri, null, null)
            pdfDocument.close()
            null
        }
    }
}
