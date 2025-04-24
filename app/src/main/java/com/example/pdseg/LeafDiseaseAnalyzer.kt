package com.example.pdseg

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.os.SystemClock
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.TensorImage.createFrom
import org.tensorflow.lite.support.image.ops.ResizeOp

class LeafDiseaseSegmentationHelper(
    context: Context,
    private val listener: SegmentationListener
) {
    companion object {
        private const val TAG = "LeafDiseaseSegmentation"
    }

    private var interpreter: Interpreter? = null
    private var interpreter2: Interpreter? = null

    init {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val model = FileUtil.loadMappedFile(context, "espnet-leaf.tflite")
                val options = Interpreter.Options().apply {
                    numThreads = 4
                    setUseXNNPACK(false)
                }
                interpreter = Interpreter(model, options)
            } catch (e: Exception) {
                Log.e(TAG, "Model initialization failed: ${e.message}")
                listener.onError("Failed to initialize model: ${e.message}")
            }
            try {
                val model = FileUtil.loadMappedFile(context, "espnet-disease.tflite")
                val options = Interpreter.Options().apply {
                    numThreads = 4
                    setUseXNNPACK(false)
                }
                interpreter2 = Interpreter(model, options)
            } catch (e: Exception) {
                Log.e(TAG, "Model initialization failed: ${e.message}")
                listener.onError("Failed to initialize model: ${e.message}")
            }
        }
    }

    fun processImage(bitmap: Bitmap) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (interpreter == null) {
                    listener.onError("Interpreter is not initialized")
                    return@launch
                }

                val startTime = SystemClock.uptimeMillis()

                val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 256, 256, true)
                val tensorImage = createFrom(TensorImage.fromBitmap(resizedBitmap), DataType.FLOAT32)
                val inputBuffer = tensorImage.tensorBuffer.buffer.rewind()


                val output = Array(256) { FloatArray(256) }
                interpreter?.run(inputBuffer, output)

                val (maskedOriginal, leafCount) = applyMaskToOriginal(resizedBitmap, output)

                val tensorImage2 = createFrom(TensorImage.fromBitmap(resizedBitmap), DataType.FLOAT32)
                val inputBuffer2 = tensorImage2.tensorBuffer.buffer.rewind()


                val output2 = Array(256) { FloatArray(256) }
                interpreter2?.run(inputBuffer2, output2)

                val (maskedLeaf, diseaseCount) = applyDiseaseMask(resizedBitmap, output, output2)


                val inferenceTime = SystemClock.uptimeMillis() - startTime

                Log.i(TAG, "Active leaf mask pixels: $leafCount")
                Log.i(TAG, "Active disease mask pixels: $diseaseCount")
                val ratio = diseaseCount.toFloat() / leafCount.toFloat()

                listener.onResults(maskedLeaf, inferenceTime, ratio)
            } catch (e: Exception) {
                Log.e(TAG, "Processing error: ${e.message}")
                listener.onError("Processing error: ${e.message}")
            }
        }
    }

    private fun applyMaskToOriginal(resized: Bitmap, output: Array<FloatArray>): Pair<Bitmap, Int> {
        val resultBitmap = Bitmap.createBitmap(resized.width, resized.height, Bitmap.Config.ARGB_8888)
        var count = 0

        for (y in 0 until resized.height) {
            for (x in 0 until resized.width) {
                val maskValue = output[y][x]
                if (maskValue < 0.5f) {
                    resultBitmap.setPixel(x, y, Color.BLACK)
                } else {
                    resultBitmap.setPixel(x, y, resized.getPixel(x, y))
                    count++
                }
            }
        }

        return Pair(resultBitmap, count)
    }

    private fun applyDiseaseMask(resized: Bitmap, output: Array<FloatArray>, output2: Array<FloatArray>): Pair<Bitmap, Int> {
        val resultBitmap = Bitmap.createBitmap(resized.width, resized.height, Bitmap.Config.ARGB_8888)
        var count = 0

        for (y in 0 until resized.height) {
            for (x in 0 until resized.width) {
                if (output[y][x] > 0.5f && output2[y][x] > 0.5f) {
                    val color = resized.getPixel(x, y)

                    val factor = 1.5
                    val a = Color.alpha(color)
                    val r = (Color.red(color) * factor).toInt().coerceIn(0, 255)
                    val g = Color.green(color)
                    val b = Color.blue(color)

                    val newColor = Color.argb(a, r, g, b)
                    resultBitmap.setPixel(x, y, newColor)
                    count++
                }
                else {
                    resultBitmap.setPixel(x, y, resized.getPixel(x, y))
                }
            }
        }

        return Pair(resultBitmap, count)
    }

    interface SegmentationListener {
        fun onResults(resultImage: Bitmap, inferenceTime: Long, ratio: Float)
        fun onError(error: String)
    }
}