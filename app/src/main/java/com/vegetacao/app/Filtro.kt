package com.vegetacao.app

import android.content.Context
import android.graphics.Bitmap
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.objectdetector.ObjectDetector
import com.google.mediapipe.tasks.vision.objectdetector.ObjectDetector.ObjectDetectorOptions

class Filtro(ctx: Context) {
    private val detector = ObjectDetector.createFromOptions(
        ctx,
        ObjectDetectorOptions.builder()
            .setBaseOptions(BaseOptions.builder().setModelAssetPath("efficientdet_lite0.tflite").build())
            .setRunningMode(RunningMode.IMAGE)
            .setMaxResults(6)
            .setScoreThreshold(0.4f)
            .build()
    )
    private val bloqueios = setOf("car", "truck", "bus", "motorcycle", "stop sign", "traffic light")

    fun obstruida(bmp: Bitmap) = escuro(bmp) || coberto(bmp)

    private fun escuro(bmp: Bitmap): Boolean {
        val p = Bitmap.createScaledBitmap(bmp, 32, 18, true)
        var soma = 0L
        for (y in 0 until 18) for (x in 0 until 32) {
            val c = p.getPixel(x, y)
            soma += (54 * (c shr 16 and 255) + 183 * (c shr 8 and 255) + 19 * (c and 255)) shr 8
        }
        return soma / (32 * 18) < LIMIAR_ESCURO
    }

    private fun coberto(bmp: Bitmap): Boolean {
        val area = (bmp.width * bmp.height).toFloat()
        val resultado = detector.detect(BitmapImageBuilder(bmp).build())
        return resultado.detections().any { d ->
            val categoria = d.categories().firstOrNull()?.categoryName()
            categoria in bloqueios &&
                d.boundingBox().width() * d.boundingBox().height() / area > LIMIAR_AREA
        }
    }

    companion object {
        const val LIMIAR_ESCURO = 45
        const val LIMIAR_AREA = 0.04f
    }
}
