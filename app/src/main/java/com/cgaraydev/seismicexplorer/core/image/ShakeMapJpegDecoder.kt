package com.cgaraydev.seismicexplorer.core.image

import android.graphics.Bitmap
import android.graphics.BitmapFactory

/** El mismo límite de decodificación se aplica antes de cachear y antes de presentar. */
object ShakeMapJpegDecoder {
    fun decode(bytes: ByteArray): Bitmap? {
        // Marcadores SOI y EOI: un encabezado JPEG aislado no es una imagen completa.
        if (bytes.size < 4 ||
            (bytes[0].toInt() and 255) != 0xFF ||
            (bytes[1].toInt() and 255) != 0xD8 ||
            (bytes[bytes.lastIndex - 1].toInt() and 255) != 0xFF ||
            (bytes[bytes.lastIndex].toInt() and 255) != 0xD9
        ) return null

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        val width = bounds.outWidth
        val height = bounds.outHeight
        if (bounds.outMimeType != "image/jpeg" ||
            width !in 1..16_384 || height !in 1..16_384 ||
            width.toLong() * height.toLong() > 100_000_000L
        ) return null

        var sample = 1
        while (width / sample > 2048 || height / sample > 2048 ||
            (width.toLong() / sample) * (height.toLong() / sample) > 4_000_000L
        ) sample *= 2

        val bitmap = BitmapFactory.decodeByteArray(
            bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample }
        ) ?: return null
        if (bitmap.width > 2048 || bitmap.height > 2048 ||
            bitmap.width.toLong() * bitmap.height.toLong() > 4_000_000L
        ) {
            bitmap.recycle()
            return null
        }
        return bitmap
    }

    fun isValid(bytes: ByteArray): Boolean {
        val decoded = decode(bytes) ?: return false
        decoded.recycle()
        return true
    }
}
