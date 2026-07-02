package com.klic.mobile.app.data

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import kotlin.math.max
import kotlin.math.roundToInt

data class EncodedImage(
    val bytes: ByteArray,
    val contentType: String,
    val width: Int,
    val height: Int,
)

object ImageUploads {
    fun encodeImage(
        context: Context,
        uri: Uri,
        maxDimension: Int = 2048,
        quality: Int = 85,
    ): EncodedImage? {
        val bitmap = decodeBitmap(context.contentResolver, uri) ?: return null
        val scaled = scaleDown(bitmap, maxDimension)
        val bytes = java.io.ByteArrayOutputStream().use { out ->
            if (!scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)) return null
            out.toByteArray()
        }
        val result = EncodedImage(
            bytes = bytes,
            contentType = "image/jpeg",
            width = scaled.width,
            height = scaled.height,
        )
        if (scaled !== bitmap) scaled.recycle()
        bitmap.recycle()
        return result
    }

    fun encodeAvatar(
        context: Context,
        uri: Uri,
        maxDimension: Int = 2048,
        quality: Int = 85,
    ): EncodedImage? = encodeImage(context, uri, maxDimension, quality)

    private fun decodeBitmap(resolver: ContentResolver, uri: Uri): Bitmap? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(resolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                decoder.isMutableRequired = false
                val maxSide = max(info.size.width, info.size.height)
                if (maxSide > 4096) decoder.setTargetSampleSize((maxSide / 4096f).roundToInt().coerceAtLeast(1))
            }
        } else {
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
        }

    private fun scaleDown(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val maxSide = max(bitmap.width, bitmap.height)
        if (maxSide <= maxDimension) return bitmap
        val scale = maxDimension.toFloat() / maxSide.toFloat()
        val width = (bitmap.width * scale).roundToInt().coerceAtLeast(1)
        val height = (bitmap.height * scale).roundToInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, width, height, true)
    }
}
