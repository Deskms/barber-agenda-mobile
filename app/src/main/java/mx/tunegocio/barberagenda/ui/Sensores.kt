package mx.tunegocio.barberagenda.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

// =============================================================================
// Sensor 1: cámara. Lee el QR del mostrador para registrar la llegada.
// =============================================================================

/** Pide el permiso de cámara y, ya concedido, muestra la vista previa. */
@Composable
fun PantallaEscaner(alLeer: (String) -> Unit, alCerrar: () -> Unit) {
    val contexto = LocalContext.current
    var permiso by remember { mutableStateOf(tienePermisoCamara(contexto)) }
    val pedir = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        permiso = it
    }

    LaunchedEffect(Unit) {
        if (!permiso) pedir.launch(Manifest.permission.CAMERA)
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Apunta la cámara al QR del mostrador.", style = MaterialTheme.typography.titleMedium)

        if (permiso) {
            VistaCamara(Modifier.weight(1f).fillMaxWidth(), alLeer)
        } else {
            Text("La app necesita la cámara solo para leer el QR. No toma ni guarda fotos.")
            Button(onClick = { pedir.launch(Manifest.permission.CAMERA) }) { Text("Dar permiso") }
        }

        OutlinedButton(onClick = alCerrar, modifier = Modifier.fillMaxWidth()) { Text("Cancelar") }
    }
}

private fun tienePermisoCamara(contexto: Context): Boolean =
    ContextCompat.checkSelfPermission(contexto, Manifest.permission.CAMERA) ==
        PackageManager.PERMISSION_GRANTED

/** CameraX para la vista previa y ML Kit para reconocer el QR, todo en el teléfono. */
@Composable
private fun VistaCamara(modifier: Modifier, alLeer: (String) -> Unit) {
    val contexto = LocalContext.current
    val ciclo = LocalLifecycleOwner.current
    val alLeerActual by rememberUpdatedState(alLeer)
    val vista = remember { PreviewView(contexto) }
    val yaLeido = remember { AtomicBoolean(false) }

    DisposableEffect(ciclo) {
        val futuro = ProcessCameraProvider.getInstance(contexto)
        val hilo = Executors.newSingleThreadExecutor()
        val lector = BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build()
        )

        futuro.addListener({
            val proveedor = futuro.get()
            val vistaPrevia = Preview.Builder().build().also {
                it.setSurfaceProvider(vista.surfaceProvider)
            }
            val analisis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
            analisis.setAnalyzer(hilo, AnalizadorQr(lector) { valor ->
                // Se acepta solo la primera lectura para no mandar el código varias veces.
                if (yaLeido.compareAndSet(false, true)) {
                    ContextCompat.getMainExecutor(contexto).execute { alLeerActual(valor) }
                }
            })
            try {
                proveedor.unbindAll()
                proveedor.bindToLifecycle(ciclo, CameraSelector.DEFAULT_BACK_CAMERA, vistaPrevia, analisis)
            } catch (e: Exception) {
                // Sin cámara trasera (algunos emuladores): la pantalla queda en negro.
            }
        }, ContextCompat.getMainExecutor(contexto))

        onDispose {
            if (futuro.isDone) futuro.get().unbindAll()
            hilo.shutdown()
            lector.close()
        }
    }

    AndroidView(factory = { vista }, modifier = modifier)
}

private class AnalizadorQr(
    private val lector: BarcodeScanner,
    private val alLeer: (String) -> Unit,
) : ImageAnalysis.Analyzer {

    @androidx.annotation.OptIn(ExperimentalGetImage::class)
    override fun analyze(imagen: ImageProxy) {
        val media = imagen.image
        if (media == null) {
            imagen.close()
            return
        }
        val entrada = InputImage.fromMediaImage(media, imagen.imageInfo.rotationDegrees)
        lector.process(entrada)
            .addOnSuccessListener { codigos ->
                codigos.firstNotNullOfOrNull { it.rawValue }?.let(alLeer)
            }
            .addOnCompleteListener { imagen.close() }
    }
}

// =============================================================================
// Sensor 2: huella o rostro (biometría) para abrir la app.
// =============================================================================

private const val AUTENTICADORES = BiometricManager.Authenticators.BIOMETRIC_WEAK

/** true si el teléfono tiene lector y la persona registró al menos una huella o rostro. */
fun huellaDisponible(contexto: Context): Boolean =
    BiometricManager.from(contexto).canAuthenticate(AUTENTICADORES) ==
        BiometricManager.BIOMETRIC_SUCCESS

/** Muestra el diálogo del sistema. La huella nunca sale del teléfono ni la ve la app. */
fun pedirHuella(
    actividad: FragmentActivity,
    alExito: () -> Unit,
    alFallar: (String) -> Unit,
) {
    val prompt = BiometricPrompt(
        actividad,
        ContextCompat.getMainExecutor(actividad),
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                alExito()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                alFallar(errString.toString())
            }
        },
    )

    val info = BiometricPrompt.PromptInfo.Builder()
        .setTitle("Barber Agenda")
        .setSubtitle("Confirma que eres tú")
        .setAllowedAuthenticators(AUTENTICADORES)
        .setNegativeButtonText("Cancelar")
        .build()

    prompt.authenticate(info)
}

/** Pantalla de bloqueo cuando la clienta activó la huella. */
@Composable
fun PantallaBloqueo(alDesbloquear: () -> Unit, alSalir: () -> Unit) {
    val actividad = LocalContext.current as FragmentActivity
    var error by remember { mutableStateOf<String?>(null) }
    val intentar = {
        error = null
        pedirHuella(actividad, alExito = alDesbloquear, alFallar = { error = it })
    }

    LaunchedEffect(Unit) { intentar() }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, androidx.compose.ui.Alignment.CenterVertically),
    ) {
        Text("Barber Agenda está bloqueada", style = MaterialTheme.typography.headlineSmall)
        Text("Usa tu huella o rostro para ver tus citas.")
        error?.let { MensajeError(it) }
        Button(onClick = intentar, modifier = Modifier.fillMaxWidth()) { Text("Desbloquear") }
        OutlinedButton(onClick = alSalir, modifier = Modifier.fillMaxWidth()) {
            Text("Cerrar sesión y entrar con código")
        }
    }
}
