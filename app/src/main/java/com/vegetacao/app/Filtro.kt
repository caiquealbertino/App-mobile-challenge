package com.vegetacao.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
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

    fun obstruida(bmp: Bitmap) = escuro(bmp) || !temGrama(bmp) || coberto(bmp)

    private fun temGrama(bmp: Bitmap): Boolean {
        val hsv = FloatArray(3)
        var pixelsVerdes = 0
        val totalAmostras = LARGURA_AMOSTRA * ALTURA_AMOSTRA

        for (y in 0 until ALTURA_AMOSTRA) {
            val py = y * bmp.height / ALTURA_AMOSTRA
            for (x in 0 until LARGURA_AMOSTRA) {
                val px = x * bmp.width / LARGURA_AMOSTRA
                val cor = bmp.getPixel(px, py)
                Color.colorToHSV(cor, hsv)
                val hueOpenCv = hsv[0] / 2f
                if (hueOpenCv in MATIZ_MIN..MATIZ_MAX &&
                    hsv[1] >= SATURACAO_MIN && hsv[2] >= VALOR_MIN
                ) {
                    pixelsVerdes++
                }
            }
        }

        return pixelsVerdes.toFloat() / totalAmostras >= FRACAO_MINIMA_GRAMA
    }

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
        private const val LARGURA_AMOSTRA = 64
        private const val ALTURA_AMOSTRA = 36
        private const val MATIZ_MIN = 30f
        private const val MATIZ_MAX = 95f
        private const val SATURACAO_MIN = 30f / 255f
        private const val VALOR_MIN = 45f / 255f
        private const val FRACAO_MINIMA_GRAMA = 0.02f
    }
}
