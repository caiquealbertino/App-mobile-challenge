package com.vegetacao.app

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.util.Size
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.Executors

class CorridaViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = Banco.get(app).dao()
    private val filtro by lazy { Filtro(app) }
    private val fused = LocationServices.getFusedLocationProviderClient(app)
    private val cb = object : LocationCallback() {
        override fun onLocationResult(r: LocationResult) {
            ultima = r.lastLocation
        }
    }
    private var envio: Job? = null

    @Volatile private var ultima: Location? = null
    @Volatile private var corrida: Corrida? = null

    val executor = Executors.newSingleThreadExecutor()
    val ativa = MutableStateFlow(false)
    val salvas = MutableStateFlow(0)
    val descartadas = MutableStateFlow(0)
    val pendentes = dao.contarPendentes().stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    val analisador = ImageAnalysis.Analyzer { img ->
        val c = corrida
        if (c == null) img.close() else c.analisar(img)
    }

    @SuppressLint("MissingPermission")
    fun iniciar() {
        salvas.value = 0
        descartadas.value = 0
        fused.requestLocationUpdates(
            LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 500).build(),
            cb,
            Looper.getMainLooper()
        )
        corrida = Corrida(getApplication(), UUID.randomUUID().toString(), dao, filtro, salvas, descartadas) { ultima }
        ativa.value = true
        envio = viewModelScope.launch {
            while (isActive) {
                agendarEnvio(getApplication())
                delay(30_000)
            }
        }
    }

    fun finalizar() {
        corrida = null
        ativa.value = false
        envio?.cancel()
        fused.removeLocationUpdates(cb)
        agendarEnvio(getApplication())
    }
}

class MainActivity : ComponentActivity() {
    private val vm: CorridaViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContent { MaterialTheme { Tela(vm) } }
    }
}

@Composable
fun Tela(vm: CorridaViewModel) {
    val ctx = LocalContext.current
    val dono = LocalLifecycleOwner.current
    val perms = arrayOf(Manifest.permission.CAMERA, Manifest.permission.ACCESS_FINE_LOCATION)
    var ok by remember { mutableStateOf(perms.all { ContextCompat.checkSelfPermission(ctx, it) == PackageManager.PERMISSION_GRANTED }) }
    val pedir = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { r -> ok = r.values.all { it } }
    LaunchedEffect(Unit) { if (!ok) pedir.launch(perms) }
    val ativa by vm.ativa.collectAsState()
    val salvas by vm.salvas.collectAsState()
    val descartadas by vm.descartadas.collectAsState()
    val pendentes by vm.pendentes.collectAsState()

    Box(Modifier.fillMaxSize()) {
        if (ok) AndroidView(modifier = Modifier.fillMaxSize(), factory = { c ->
            PreviewView(c).also { pv ->
                val f = ProcessCameraProvider.getInstance(c)
                f.addListener({
                    val preview = Preview.Builder().build().also { it.setSurfaceProvider(pv.surfaceProvider) }
                    val analise = ImageAnalysis.Builder()
                        .setResolutionSelector(
                            ResolutionSelector.Builder()
                                .setResolutionStrategy(ResolutionStrategy(Size(1280, 720), ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER))
                                .build()
                        )
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                        .build()
                        .also { it.setAnalyzer(vm.executor, vm.analisador) }
                    f.get().apply {
                        unbindAll()
                        bindToLifecycle(dono, CameraSelector.DEFAULT_BACK_CAMERA, preview, analise)
                    }
                }, ContextCompat.getMainExecutor(c))
            }
        })
        Column(Modifier.align(Alignment.BottomCenter).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Salvas: $salvas   Descartadas: $descartadas   Pendentes de envio: $pendentes", color = Color.White)
            Button(onClick = { if (ativa) vm.finalizar() else vm.iniciar() }, enabled = ok) {
                Text(if (ativa) "Finalizar corrida" else "Iniciar corrida")
            }
        }
    }
}
