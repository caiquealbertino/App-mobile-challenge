package com.vegetacao.app

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

class EnvioWorker(ctx: Context, p: WorkerParameters) : CoroutineWorker(ctx, p) {
    private val http = OkHttpClient()

    override suspend fun doWork(): Result {
        val dao = Banco.get(applicationContext).dao()
        while (true) {
            val lote = dao.pendentes(20)
            if (lote.isEmpty()) return Result.success()
            for (c in lote) {
                val arq = File(c.caminho)
                if (!arq.exists()) {
                    dao.marcarEnviada(c.id)
                    continue
                }
                val corpo = MultipartBody.Builder().setType(MultipartBody.FORM)
                    .addFormDataPart("corridaId", c.corrida)
                    .addFormDataPart("lat", c.lat.toString())
                    .addFormDataPart("lng", c.lng.toString())
                    .addFormDataPart("timestamp", c.timestamp.toString())
                    .addFormDataPart("foto", arq.name, arq.asRequestBody("image/jpeg".toMediaType()))
                    .build()
                val ok = withContext(Dispatchers.IO) {
                    runCatching {
                        http.newCall(Request.Builder().url(BuildConfig.API_URL).post(corpo).build()).execute().use { it.isSuccessful }
                    }.getOrDefault(false)
                }
                if (!ok) return Result.retry()
                arq.delete()
                dao.marcarEnviada(c.id)
            }
        }
    }
}

fun agendarEnvio(ctx: Context) {
    val req = OneTimeWorkRequestBuilder<EnvioWorker>()
        .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
        .build()
    WorkManager.getInstance(ctx).enqueueUniqueWork("envio", ExistingWorkPolicy.KEEP, req)
}
