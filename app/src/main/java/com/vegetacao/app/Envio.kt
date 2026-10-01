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
                val codigo = withContext(Dispatchers.IO) {
                    runCatching {
                        val req = Request.Builder()
                            .url(BuildConfig.API_URL)
                            .header("X-API-Key", BuildConfig.API_TOKEN)
                            .post(corpo)
                            .build()
                        http.newCall(req).execute().use { it.code }
                    }.getOrDefault(-1)
                }
                if (codigo == -1 || codigo == 401 || codigo == 403 || codigo == 408 || codigo == 429 || codigo >= 500) return Result.retry()
                if (codigo in 200..299) arq.delete()
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
