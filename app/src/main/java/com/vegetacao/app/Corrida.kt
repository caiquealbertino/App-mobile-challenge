package com.vegetacao.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.location.Location
import android.os.SystemClock
import androidx.camera.core.ImageProxy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

class Corrida(
    ctx: Context,
    private val id: String,
    private val dao: CapturaDao,
    private val filtro: Filtro,
    private val salvas: MutableStateFlow<Int>,
    private val descartadas: MutableStateFlow<Int>,
    private val local: () -> Location?
) {
    private val dir = File(ctx.filesDir, "corridas/$id").apply { mkdirs() }
    private val io = CoroutineScope(Dispatchers.IO)
    private var ultimoProcessado = 0L
    private val intervaloMs = 1_000L
    private val IDADE_MAX_GPS_NS = 3_000_000_000L
    private val PRECISAO_MAX_M = 30f

    fun analisar(img: ImageProxy) {
        val agora = System.currentTimeMillis()
        if (agora - ultimoProcessado < intervaloMs) {
            img.close()
            return
        }
        ultimoProcessado = agora
        img.use { processar(it.toBitmap().girar(it.imageInfo.rotationDegrees)) }
    }

    fun processar(bmp: Bitmap) {
        val loc = local()?.takeIf { gpsValido(it) }
        if (loc == null) {
            descartadas.update { it + 1 }
            return
        }
        if (filtro.obstruida(bmp)) {
            descartadas.update { it + 1 }
            return
        }
        val ts = System.currentTimeMillis()
        val arq = File(dir, "$ts.jpg")
        arq.outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 85, it) }
        io.launch { dao.inserir(Captura(corrida = id, caminho = arq.path, lat = loc.latitude, lng = loc.longitude, timestamp = ts)) }
        salvas.update { it + 1 }
    }

    private fun gpsValido(l: Location) =
        SystemClock.elapsedRealtimeNanos() - l.elapsedRealtimeNanos < IDADE_MAX_GPS_NS && l.hasAccuracy() && l.accuracy <= PRECISAO_MAX_M

    private fun Bitmap.girar(g: Int) =
        if (g == 0) this else Bitmap.createBitmap(this, 0, 0, width, height, Matrix().apply { postRotate(g.toFloat()) }, true)
}
