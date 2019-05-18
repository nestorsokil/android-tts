package com.example.mobile.kotlin_android_draft

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.os.Environment

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.IntBuffer
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

/**
 * Created by nsokil on 10.02.2017.
 */

object ImageUtils {
    fun decodeYuv(data: ByteArray, width: Int, height: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val numPixels = width * height

        // the buffer we fill up which we then fill the bitmap with
        val intBuffer = IntBuffer.allocate(width * height)
        // If you're reusing a buffer, next line imperative to refill from the start,
        // if not good practice
        intBuffer.position(0)

        // Set the alpha for the image: 0 is transparent, 255 fully opaque
        val alpha = 255.toByte()

        // Get each pixel, one at a time
        for (y in 0..height - 1) {
            for (x in 0..width - 1) {
                // Get the Y value, stored in the first block of data
                // The logical "AND 0xff" is needed to deal with the signed issue
                val Y = (data[y * width + x].toInt() and 0xff).toByte()

                // Get U and V values, stored after Y values, one per 2x2 block
                // of pixels, interleaved. Prepare them as floats with correct range
                // ready for calculation later.
                val xby2 = x / 2
                val yby2 = y / 2

                // make this V for NV12/420SP
                val U = (data[numPixels + 2 * xby2 + yby2 * width].toInt() and 0xff).toFloat() - 128.0f

                // make this U for NV12/420SP
                val V = (data[numPixels + 2 * xby2 + 1 + yby2 * width].toInt() and 0xff).toFloat() - 128.0f

                // Do the YUV -> RGB conversion
                val Yf = 1.164f * Y.toFloat() - 16.0f
                var R = (Yf + 1.596f * V).toInt()
                var G = (Yf - 0.813f * V - 0.391f * U).toInt()
                var B = (Yf + 2.018f * U).toInt()

                // Clip rgb values to 0-255
                R = if (R < 0) 0 else if (R > 255) 255 else R
                G = if (G < 0) 0 else if (G > 255) 255 else G
                B = if (B < 0) 0 else if (B > 255) 255 else B

                // Put that pixel in the buffer
                intBuffer.put(alpha * 16777216 + R * 65536 + G * 256 + B)
            }
        }

        // Get buffer ready to be read
        intBuffer.flip()

        // Push the pixel information from the buffer onto the bitmap.
        bitmap.copyPixelsFromBuffer(intBuffer)
        return bitmap
    }

    fun yuv2jpeg(data: ByteArray, width: Int, height: Int): Bitmap? {
        try {
            ByteArrayOutputStream().use { out ->
                val yuv = YuvImage(data, ImageFormat.NV21, width, height, null)
                yuv.compressToJpeg(Rect(0, 0, width, height), 50, out)
                val array = out.toByteArray()
                return BitmapFactory.decodeByteArray(array, 0, array.size)
            }
        } catch (ioe: IOException) {
            return null
        }

    }

    fun binarize(source: Bitmap): Bitmap {
        //this is the image that is binarized
        val binary = convertToMutable(source)
        // I will look at each pixel and use the function isDarkEnough to decide
        // whether to make it black or otherwise white
        for (i in 0..binary.width - 1) {
            for (c in 0..binary.height - 1) {
                val pixel = binary.getPixel(i, c)
                val nextColor = if (isDarkEnough(pixel)) Color.BLACK else Color.WHITE
                binary.setPixel(i, c, nextColor)
            }
        }
        return binary
    }

    private fun isDarkEnough(pixel: Int): Boolean {
        val alpha = Color.alpha(pixel)
        val redValue = Color.red(pixel)
        val blueValue = Color.blue(pixel)
        val greenValue = Color.green(pixel)
        if (alpha == 0x00)
        //if this pixel is transparent let me use TRASNPARENT_IS_BLACK
            return false
        // distance from the white extreme
        val distanceFromWhite = Math.sqrt(Math.pow((0xff - redValue).toDouble(), 2.0) + Math.pow((0xff - blueValue).toDouble(), 2.0) + Math.pow((0xff - greenValue).toDouble(), 2.0))
        // distance from the black extreme //this should not be computed and might be as well a function of distanceFromWhite and the whole distance
        val distanceFromBlack = Math.sqrt(Math.pow((0x00 - redValue).toDouble(), 2.0) + Math.pow((0x00 - blueValue).toDouble(), 2.0) + Math.pow((0x00 - greenValue).toDouble(), 2.0))
        // distance between the extremes //this is a constant that should not be computed :p
        val distance = distanceFromBlack + distanceFromWhite
        // distance between the extremes
        return distanceFromWhite / distance > (13.0 / 30.0)
    }

    private fun convertToMutable(imgIn: Bitmap): Bitmap {
        return imgIn.copy(Bitmap.Config.ARGB_8888, true)
    }
}
