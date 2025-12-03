package com.ulcer.care.detection.ulcare.tflite

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import androidx.annotation.WorkerThread
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.math.min

data class TfResult(
    val classification: String,
    val details: String,
    val action: String,
    val raw: FloatArray
)

object TfliteRunner {

    private const val MODEL_FILE = "ULCARE.tflite"
    private const val TAG = "ULCARE_TFLITE"

    /**
     * Decode gambar dengan sampling ringan.
     * targetSize = ukuran sisi terpendek yang diinginkan (misal 256).
     */
    @WorkerThread
    fun decodeBitmapSmall(
        context: Context,
        uri: Uri,
        targetSize: Int = 256
    ): Bitmap {
        val resolver = context.contentResolver

        return if (android.os.Build.VERSION.SDK_INT >= 28) {

            val src = ImageDecoder.createSource(resolver, uri)
            ImageDecoder.decodeBitmap(src) { decoder, info, _ ->
                val (w, h) = info.size.width to info.size.height
                val minSide = min(w, h)
                val scale = targetSize / minSide.toFloat()

                val targetW = (w * scale).toInt().coerceAtLeast(32)
                val targetH = (h * scale).toInt().coerceAtLeast(32)

                decoder.setTargetSize(targetW, targetH)
                decoder.isMutableRequired = false
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }

        } else {
            // --- API < 28 ---
            val optsBound = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input, null, optsBound)
            }

            val (w, h) = optsBound.outWidth to optsBound.outHeight
            if (w <= 0 || h <= 0)
                throw IllegalArgumentException("Tidak bisa membaca ukuran gambar: $uri")

            val sampleSize = computeInSampleSize(w, h, targetSize)

            val opts = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

            val bmp = resolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input, null, opts)
            }

            bmp ?: throw IllegalArgumentException("Gagal decode gambar dari $uri")
        }
    }


    private fun computeInSampleSize(
        srcW: Int,
        srcH: Int,
        targetSize: Int
    ): Int {
        var sample = 1
        val minSide = min(srcW, srcH)
        while (minSide / (sample * 2) >= targetSize) {
            sample *= 2
        }
        return sample
    }
    //  INPUT CONVERSION

    /**
     * Konversi bitmap ke ByteBuffer float32.
     * Bitmap harus SUDAH berukuran (dstWidth x dstHeight).
     */
    @WorkerThread
    fun bitmapToFloatBuffer(
        src: Bitmap,
        dstWidth: Int,
        dstHeight: Int,
        mean: Float = 0f,
        std: Float = 255f
    ): ByteBuffer {

        val buf = ByteBuffer.allocateDirect(4 * dstWidth * dstHeight * 3)
        buf.order(ByteOrder.nativeOrder())

        val pixels = IntArray(dstWidth * dstHeight)
        src.getPixels(pixels, 0, dstWidth, 0, 0, dstWidth, dstHeight)

        var i = 0
        while (i < pixels.size) {
            val p = pixels[i]
            val r = (p ushr 16) and 0xFF
            val g = (p ushr 8) and 0xFF
            val b = p and 0xFF

            buf.putFloat((r - mean) / std)
            buf.putFloat((g - mean) / std)
            buf.putFloat((b - mean) / std)
            i++
        }
        buf.rewind()
        return buf
    }
    //  LOAD MODEL (mmap)

    private fun mmapModel(context: Context): MappedByteBuffer {
        val afd = context.assets.openFd(MODEL_FILE)
        FileInputStream(afd.fileDescriptor).channel.use { fc ->
            return fc.map(FileChannel.MapMode.READ_ONLY, afd.startOffset, afd.declaredLength)
        }
    }
    //  INTERPRETER CACHE

    private var cachedInterpreter: Interpreter? = null

    @Synchronized
    private fun getInterpreter(context: Context): Interpreter {
        cachedInterpreter?.let { return it }

        val mapped = mmapModel(context)
        val opts = Interpreter.Options().apply {
            setNumThreads(maxOf(1, Runtime.getRuntime().availableProcessors() / 2))
        }

        val interpreter = Interpreter(mapped, opts)
        cachedInterpreter = interpreter
        return interpreter
    }

    @Synchronized
    fun closeInterpreter() {
        try {
            cachedInterpreter?.close()
        } catch (_: Exception) {}
        cachedInterpreter = null
    }

    //  RUN MODEL

    @WorkerThread
    fun runWithInterpreter(context: Context, bitmap: Bitmap): TfResult {
        val interpreter = getInterpreter(context)

        // Ambil ukuran input dari model
        val inShape = interpreter.getInputTensor(0).shape()  // [1, h, w, 3]
        val ih = inShape.getOrNull(1) ?: 224
        val iw = inShape.getOrNull(2) ?: 224

        // Paksa ukuran sesuai model
        val resized = if (bitmap.width != iw || bitmap.height != ih) {
            Bitmap.createScaledBitmap(bitmap, iw, ih, true)
        } else bitmap

        // Buat input buffer
        val input = bitmapToFloatBuffer(resized, iw, ih)

        // Siapkan output array [1, num_classes]
        val outShape = interpreter.getOutputTensor(0).shape()
        val outSize = outShape.last()
        val outputArray = Array(1) { FloatArray(outSize) }

        // Jalankan inferensi
        interpreter.run(input, outputArray)

        val raw = outputArray[0]

        // Label
        val labelList = listOf(
            "light",
            "light - medium",
            "medium",
            "medium - urgent",
            "urgent"
        )

        // Ambil kelas terbaik
        val bestIdx = raw.indices.maxByOrNull { raw[it] } ?: -1
        val classification = labelList.getOrElse(bestIdx) { "Unknown" }

        return TfResult(
            classification = classification,
            details = "",
            action = "",
            raw = raw
        )
    }
    //  DEBUG

    @WorkerThread
    fun debugLogModelIO(context: Context) {
        val mapped = mmapModel(context)
        val interpreter = Interpreter(mapped)

        val inCount = interpreter.inputTensorCount
        android.util.Log.d(TAG, "INPUT COUNT = $inCount")
        for (i in 0 until inCount) {
            val t = interpreter.getInputTensor(i)
            android.util.Log.d(TAG, "INPUT[$i] shape=${t.shape().contentToString()} type=${t.dataType()}")
        }

        val outCount = interpreter.outputTensorCount
        android.util.Log.d(TAG, "OUTPUT COUNT = $outCount")
        for (i in 0 until outCount) {
            val t = interpreter.getOutputTensor(i)
            android.util.Log.d(TAG, "OUTPUT[$i] shape=${t.shape().contentToString()} type=${t.dataType()}")
        }

        interpreter.close()
    }
}
