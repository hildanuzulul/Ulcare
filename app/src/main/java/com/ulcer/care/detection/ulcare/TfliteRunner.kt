package com.ulcer.care.detection.ulcare.tflite

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import androidx.annotation.WorkerThread
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

data class TfResult(
    val classification: String,  // label top-1 dari model
    val details: String,         // dikosongkan (kita tidak menambah info di luar tflite)
    val action: String,          // dikosongkan (idem)
    val raw: FloatArray          // skor mentah dari output model
)

object TfliteRunner {

    private const val TAG = "ULCARE_TFLITE"
    private const val MODEL_FILE = "ULCARE.tflite"

    /* ======================== Image utils ======================== */

    @WorkerThread
    fun decodeBitmapFromUri(context: Context, uri: Uri): Bitmap {
        return if (android.os.Build.VERSION.SDK_INT >= 28) {
            val src = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(src) { decoder, _, _ ->
                decoder.isMutableRequired = false
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        } else {
            context.contentResolver.openInputStream(uri).use { input ->
                val opts = BitmapFactory.Options().apply {
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }
                BitmapFactory.decodeStream(input, null, opts)
                    ?: throw IllegalArgumentException("Gagal decode bitmap dari $uri")
            }
        }
    }

    @WorkerThread
    fun bitmapToFloatBuffer(
        src: Bitmap,
        dstWidth: Int,
        dstHeight: Int,
        mean: Float = 0f,
        std: Float = 255f
    ): ByteBuffer {
        val scaled = Bitmap.createScaledBitmap(src, dstWidth, dstHeight, true)
        val buf = ByteBuffer.allocateDirect(4 * dstWidth * dstHeight * 3)
            .order(ByteOrder.nativeOrder())

        val pixels = IntArray(dstWidth * dstHeight)
        scaled.getPixels(pixels, 0, dstWidth, 0, 0, dstWidth, dstHeight)

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

    /* Model loading */

    private fun mmapModel(context: Context): MappedByteBuffer {
        val afd = context.assets.openFd(MODEL_FILE)
        FileInputStream(afd.fileDescriptor).channel.use { fc ->
            return fc.map(FileChannel.MapMode.READ_ONLY, afd.startOffset, afd.declaredLength)
        }
    }

    /* Inference  */

    @WorkerThread
    fun runWithInterpreter(context: Context, bitmap: Bitmap): TfResult {
        val mapped = mmapModel(context)
        val interpreter = Interpreter(mapped)

        // Asumsi input [1,H,W,3]
        val inShape = interpreter.getInputTensor(0).shape()
        val h = inShape.getOrNull(1) ?: 224
        val w = inShape.getOrNull(2) ?: 224

        val inputBuffer = bitmapToFloatBuffer(bitmap, w, h)

        // Asumsi output tunggal: 5 skor kelas
        val outShape = interpreter.getOutputTensor(0).shape()
        val outSize = outShape.last()
        val outBuf = ByteBuffer.allocateDirect(outSize * 4).order(ByteOrder.nativeOrder())
        val outputs = hashMapOf<Int, Any>(0 to outBuf)

        interpreter.runForMultipleInputsOutputs(arrayOf(inputBuffer), outputs)

        outBuf.rewind()
        val raw = FloatArray(outSize)
        outBuf.asFloatBuffer().get(raw)
        interpreter.close()

        // Label urutan output model
        val labelList = listOf(
            "light",
            "light - medium",
            "medium",
            "medium - urgent",
            "urgent"
        )

        val bestIdx = raw.indices.maxByOrNull { raw[it] } ?: -1
        val classification = labelList.getOrElse(bestIdx) { "Unknown" }

        return TfResult(
            classification = classification,
            details = "",
            action = "",
            raw = raw
        )
    }

    /* ======================== DEBUG: log IO model ====== ================== */

    @WorkerThread
    fun debugLogModelIO(context: Context) {
        val mapped = mmapModel(context)
        val interpreter = Interpreter(mapped)

        // Input tensors
        val inCount = interpreter.inputTensorCount
        android.util.Log.d(TAG, "INPUT count = $inCount")
        for (i in 0 until inCount) {
            val t = interpreter.getInputTensor(i)
            val shape = t.shape().contentToString()
            val q = t.quantizationParams()
            android.util.Log.d(
                TAG,
                "in#$i type=${t.dataType()} shape=$shape quant(scale=${q.scale}, zp=${q.zeroPoint})"
            )
        }

        // Output tensors
        val outCount = interpreter.outputTensorCount
        android.util.Log.d(TAG, "OUTPUT count = $outCount")
        for (i in 0 until outCount) {
            val t = interpreter.getOutputTensor(i)
            val shape = t.shape().contentToString()
            val q = t.quantizationParams()
            android.util.Log.d(
                TAG,
                "out#$i type=${t.dataType()} shape=$shape numBytes=${t.numBytes()} quant(scale=${q.scale}, zp=${q.zeroPoint})"
            )
        }

        // Opsional: jalankan dummy inference untuk sample nilai float
        try {
            val inShape = interpreter.getInputTensor(0).shape()
            val h = inShape.getOrNull(1) ?: 224
            val w = inShape.getOrNull(2) ?: 224
            val dummy = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val inBuf = bitmapToFloatBuffer(dummy, w, h)

            val outputs = HashMap<Int, Any>()
            for (i in 0 until outCount) {
                val t = interpreter.getOutputTensor(i)
                if (t.dataType() == org.tensorflow.lite.DataType.FLOAT32) {
                    val outBuf = ByteBuffer.allocateDirect(t.numBytes()).order(ByteOrder.nativeOrder())
                    outputs[i] = outBuf
                } else {
                    android.util.Log.d(TAG, "out#$i non-float; skip sample values")
                }
            }
            interpreter.runForMultipleInputsOutputs(arrayOf(inBuf), outputs)

            for (i in 0 until outCount) {
                val any = outputs[i] ?: continue
                if (any is ByteBuffer) {
                    any.rewind()
                    val fb = any.asFloatBuffer()
                    val n = fb.remaining()
                    val sampleCount = minOf(10, n)
                    val arr = FloatArray(sampleCount)
                    fb.get(arr)
                    android.util.Log.d(TAG, "out#$i first $sampleCount values = ${arr.contentToString()}")
                }
            }
        } catch (e: Throwable) {
            android.util.Log.w(TAG, "Skip dummy inference preview: ${e.message}")
        } finally {
            interpreter.close()
        }
    }
}
