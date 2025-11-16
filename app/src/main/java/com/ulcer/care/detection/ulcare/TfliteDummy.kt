package com.ulcer.care.detection.ulcare//@file:Suppress("DEPRECATION")
//
//package com.ulcer.care.detection.ulcare.tflite
//
//import android.content.Context
//import android.content.res.AssetFileDescriptor
//import android.util.Log
//import androidx.room.jarjarred.org.antlr.v4.gui.Interpreter
//import org.tensorflow.lite.InterpreterApi
//import org.tensorflow.lite.InterpreterFactory
//import java.io.FileInputStream
//import java.nio.ByteBuffer
//import java.nio.ByteOrder
//import java.nio.MappedByteBuffer
//import java.nio.channels.FileChannel
//
//object TfliteDummy {
//    private const val TAG = "TFLiteDummy"
//    private const val MODEL_NAME = "model.tflite"
//
//    data class IOInfo(
//        val inputName: String,
//        val inputShape: IntArray,
//        val inputType: String,
//        val outputName: String,
//        val outputShape: IntArray,
//        val outputType: String
//    )
//
//    data class Result(
//        val exudate: Float,
//        val area: Float,
//        val tissue: Float,
//        val total: Float,
//        val raw: FloatArray
//    )
//
//    private fun loadModelFileFromAssets(context: Context): MappedByteBuffer {
//        val afd: AssetFileDescriptor = context.assets.openFd(MODEL_NAME)
//        FileInputStream(afd.fileDescriptor).use { fis ->
//            val fc: FileChannel = fis.channel
//            return fc.map(FileChannel.MapMode.READ_ONLY, afd.startOffset, afd.declaredLength)
//        }
//    }
//
//    private fun loadModelFromRaw(context: Context, resId: Int): ByteBuffer {
//        context.resources.openRawResource(resId).use { input ->
//            val bytes = input.readBytes()
//            val buffer = ByteBuffer.allocateDirect(bytes.size)
//            buffer.order(ByteOrder.nativeOrder())
//            buffer.put(bytes)
//            buffer.rewind()
//            return buffer
//        }
//    }
//
//    private fun newInterpreter(context: Context): Pair<InterpreterApi, MappedByteBuffer> {
//        val buffer = loadModelFileFromAssets(context)
//        val options = InterpreterApi.Options()
//        val interpreter = InterpreterFactory().create(buffer, options)
//        return interpreter to buffer
//    }
//
//    fun inspectIO(context: Context): IOInfo {
//        val (interpreterApi, _) = newInterpreter(context)
//        val interp = interpreterApi as? Interpreter
//            ?: error("Need org.tensorflow.lite.Interpreter for tensor metadata")
//
//        val inTensor = interp.getInputTensor(0)
//        val outTensor = interp.getOutputTensor(0)
//
//        val info = IOInfo(
//            inputName = inTensor.name(),
//            inputShape = inTensor.shape(),
//            inputType = inTensor.dataType().name,
//            outputName = outTensor.name(),
//            outputShape = outTensor.shape(),
//            outputType = outTensor.dataType().name
//        )
//        Log.i(TAG, "INPUT name=${info.inputName} shape=${info.inputShape.contentToString()} type=${info.inputType}")
//        Log.i(TAG, "OUTPUT name=${info.outputName} shape=${info.outputShape.contentToString()} type=${info.outputType}")
//        return info
//    }
//
//    fun runOnceDummy(context: Context): Result {
//        val (interpreterApi, _) = newInterpreter(context)
//        val interp = interpreterApi as? Interpreter
//            ?: error("Need org.tensorflow.lite.Interpreter for run()")
//
//        val inShape = interp.getInputTensor(0).shape()   // [1, N] (contoh)
//        val outShape = interp.getOutputTensor(0).shape() // [1, M] (contoh)
//
//        val nFeatures = if (inShape.isNotEmpty()) inShape.last() else 1
//        val input1D = FloatArray(nFeatures) { 0f }
//        val input = arrayOf(input1D) // [1, N]
//
//        val outSize = if (outShape.isNotEmpty()) outShape.last() else 1
//        val output1D = FloatArray(outSize)
//        val output = arrayOf(output1D) // [1, M]
//
//        interp.run(input, output)
//
//        val raw = output1D
//        val e = raw.getOrElse(0) { 0f }
//        val a = raw.getOrElse(1) { 0f }
//        val t = raw.getOrElse(2) { 0f }
//        val total = e + a + t
//
//        Log.i(TAG, "RAW out=${raw.joinToString(prefix = "[", postfix = "]")}")
//        return Result(exudate = e, area = a, tissue = t, total = total, raw = raw)
//    }
//}
