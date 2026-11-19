package mx.tunegocio.barberagenda.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Looper
import android.os.SystemClock
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlin.math.roundToInt
import kotlin.math.sqrt

// Sensores de movimiento y posición de Android (SensorManager) más el GPS.

// =============================================================================
// Acelerómetro: agitar el teléfono abre la tarjeta de sellos.
// =============================================================================

/** Fuerza, en g, a partir de la cual un movimiento cuenta como "agitar". */
private const val UMBRAL_AGITAR = 2.5f
private const val PAUSA_ENTRE_AGITADAS_MS = 1500L

/**
 * Escucha el acelerómetro solo mientras la app está en primer plano,
 * para no gastar batería en segundo plano.
 */
@Composable
fun DetectorDeAgitar(alAgitar: () -> Unit) {
    val contexto = LocalContext.current
    val ciclo = LocalLifecycleOwner.current
    val alAgitarActual by rememberUpdatedState(alAgitar)

    DisposableEffect(ciclo) {
        val gestor = contexto.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val acelerometro = gestor.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        var ultima = 0L

        val oyente = object : SensorEventListener {
            override fun onSensorChanged(evento: SensorEvent) {
                val x = evento.values[0]
                val y = evento.values[1]
                val z = evento.values[2]
                // En reposo la magnitud es 1 g (la gravedad); al agitar sube mucho.
                val fuerza = sqrt(x * x + y * y + z * z) / SensorManager.GRAVITY_EARTH
                val ahora = SystemClock.elapsedRealtime()
                if (fuerza > UMBRAL_AGITAR && ahora - ultima > PAUSA_ENTRE_AGITADAS_MS) {
                    ultima = ahora
                    alAgitarActual()
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, precision: Int) = Unit
        }

        val observador = LifecycleEventObserver { _, evento ->
            when (evento) {
                Lifecycle.Event.ON_RESUME -> acelerometro?.let {
                    gestor.registerListener(oyente, it, SensorManager.SENSOR_DELAY_UI)
                }
                Lifecycle.Event.ON_PAUSE -> gestor.unregisterListener(oyente)
                else -> Unit
            }
        }
        ciclo.lifecycle.addObserver(observador)

        onDispose {
            ciclo.lifecycle.removeObserver(observador)
            gestor.unregisterListener(oyente)
        }
    }
}

// =============================================================================
// Magnetómetro + GPS: brújula que apunta a la barbería.
// =============================================================================

private val PERMISOS_UBICACION = arrayOf(
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION,
)

private fun tieneUbicacion(contexto: Context): Boolean = PERMISOS_UBICACION.any {
    ContextCompat.checkSelfPermission(contexto, it) == PackageManager.PERMISSION_GRANTED
}

@Composable
fun PantallaBrujula(vm: BrujulaViewModel, alCerrar: () -> Unit) {
    val contexto = LocalContext.current
    val abrir = LocalUriHandler.current
    var permiso by remember { mutableStateOf(tieneUbicacion(contexto)) }
    val pedir = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        permiso = it.values.any { concedido -> concedido }
    }

    LaunchedEffect(Unit) {
        vm.cargar()
        if (!permiso) pedir.launch(PERMISOS_UBICACION)
    }

    val negocio = vm.negocio
    val lat = negocio?.lat
    val lng = negocio?.lng

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(negocio?.nombre ?: "Cómo llegar", style = MaterialTheme.typography.titleLarge)
        negocio?.direccion?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        vm.error?.let { MensajeError(it) }

        when {
            vm.cargando -> Cargando()
            negocio != null && (lat == null || lng == null) ->
                Text("El negocio todavía no configuró su ubicación (NEGOCIO_LAT y NEGOCIO_LNG en el servidor).")
            lat != null && lng != null && !permiso -> {
                Text("Para apuntar hacia la barbería la app necesita tu ubicación. Solo se usa en esta pantalla y no se envía al servidor.")
                Button(onClick = { pedir.launch(PERMISOS_UBICACION) }) { Text("Dar permiso") }
            }
            lat != null && lng != null -> Brujula(lat, lng)
        }

        if (lat != null && lng != null) {
            val etiqueta = Uri.encode(negocio?.nombre ?: "Barbería")
            OutlinedButton(
                onClick = { abrir.openUri("geo:$lat,$lng?q=$lat,$lng($etiqueta)") },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Abrir en el mapa") }
        }

        OutlinedButton(onClick = alCerrar, modifier = Modifier.fillMaxWidth()) { Text("Cerrar") }
    }
}

/** Requiere el permiso de ubicación ya concedido. */
@SuppressLint("MissingPermission")
@Composable
private fun Brujula(latDestino: Double, lngDestino: Double) {
    val contexto = LocalContext.current
    val ciclo = LocalLifecycleOwner.current

    var azimut by remember { mutableFloatStateOf(0f) }
    var hayMagnetometro by remember { mutableStateOf(true) }
    var calibrar by remember { mutableStateOf(false) }
    var ubicacion by remember { mutableStateOf<Location?>(null) }

    // --- Acelerómetro + magnetómetro → hacia dónde apunta el teléfono ---------
    DisposableEffect(ciclo) {
        val gestor = contexto.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val acelerometro = gestor.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magnetometro = gestor.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        hayMagnetometro = magnetometro != null && acelerometro != null

        val gravedad = FloatArray(3)
        val campo = FloatArray(3)
        var tieneGravedad = false
        var tieneCampo = false
        val rotacion = FloatArray(9)
        val orientacion = FloatArray(3)
        val suavizado = 0.15f // filtro paso bajo: evita que la flecha tiemble

        val oyente = object : SensorEventListener {
            override fun onSensorChanged(evento: SensorEvent) {
                val destino = if (evento.sensor.type == Sensor.TYPE_ACCELEROMETER) gravedad else campo
                for (i in 0..2) destino[i] += suavizado * (evento.values[i] - destino[i])
                if (evento.sensor.type == Sensor.TYPE_ACCELEROMETER) tieneGravedad = true else tieneCampo = true

                if (tieneGravedad && tieneCampo &&
                    SensorManager.getRotationMatrix(rotacion, null, gravedad, campo)
                ) {
                    SensorManager.getOrientation(rotacion, orientacion)
                    val grados = Math.toDegrees(orientacion[0].toDouble()).toFloat()
                    azimut = (grados + 360f) % 360f
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, precision: Int) {
                if (sensor?.type == Sensor.TYPE_MAGNETIC_FIELD) {
                    calibrar = precision <= SensorManager.SENSOR_STATUS_ACCURACY_LOW
                }
            }
        }

        // --- GPS (y red como respaldo) → dónde está la clienta ----------------
        val gps = contexto.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        // Objeto completo y no lambda: en Android 8 a 10 estos métodos no tienen
        // implementación por defecto y una lambda haría fallar la app.
        val oyenteGps = object : LocationListener {
            override fun onLocationChanged(nueva: Location) {
                ubicacion = nueva
            }

            override fun onProviderEnabled(proveedor: String) = Unit
            override fun onProviderDisabled(proveedor: String) = Unit

            @Deprecated("Requerido en Android 8 a 10")
            override fun onStatusChanged(proveedor: String?, estado: Int, extras: android.os.Bundle?) = Unit
        }
        val proveedores = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            .filter { runCatching { gps.isProviderEnabled(it) }.getOrDefault(false) }

        val observador = LifecycleEventObserver { _, evento ->
            when (evento) {
                Lifecycle.Event.ON_RESUME -> {
                    acelerometro?.let { gestor.registerListener(oyente, it, SensorManager.SENSOR_DELAY_UI) }
                    magnetometro?.let { gestor.registerListener(oyente, it, SensorManager.SENSOR_DELAY_UI) }
                    proveedores.forEach { proveedor ->
                        runCatching {
                            gps.getLastKnownLocation(proveedor)?.let { if (ubicacion == null) ubicacion = it }
                            gps.requestLocationUpdates(proveedor, 2000L, 2f, oyenteGps, Looper.getMainLooper())
                        }
                    }
                }
                Lifecycle.Event.ON_PAUSE -> {
                    gestor.unregisterListener(oyente)
                    gps.removeUpdates(oyenteGps)
                }
                else -> Unit
            }
        }
        ciclo.lifecycle.addObserver(observador)

        onDispose {
            ciclo.lifecycle.removeObserver(observador)
            gestor.unregisterListener(oyente)
            gps.removeUpdates(oyenteGps)
        }
    }

    val aqui = ubicacion
    if (!hayMagnetometro) {
        Text("Tu teléfono no tiene magnetómetro; usa el botón del mapa.")
        return
    }
    if (aqui == null) {
        Text("Buscando tu ubicación… Si tarda, sal a un lugar abierto o activa la ubicación del teléfono.")
        Cargando()
        return
    }

    val destino = Location("barberia").apply {
        latitude = latDestino
        longitude = lngDestino
    }
    // El magnetómetro apunta al norte magnético; el GPS calcula contra el norte real.
    val declinacion = GeomagneticField(
        aqui.latitude.toFloat(), aqui.longitude.toFloat(), aqui.altitude.toFloat(), System.currentTimeMillis(),
    ).declination
    val rumbo = (aqui.bearingTo(destino) + 360f) % 360f
    val angulo = (rumbo - (azimut + declinacion) + 720f) % 360f
    val metros = aqui.distanceTo(destino)

    val color = MaterialTheme.colorScheme.primary
    val borde = MaterialTheme.colorScheme.outline
    Canvas(Modifier.size(240.dp)) {
        drawCircle(color = borde, style = Stroke(width = 4f))
        rotate(angulo) {
            val w = size.width
            val h = size.height
            val flecha = Path().apply {
                moveTo(w / 2f, h * 0.10f)
                lineTo(w * 0.68f, h * 0.55f)
                lineTo(w / 2f, h * 0.45f)
                lineTo(w * 0.32f, h * 0.55f)
                close()
            }
            drawPath(flecha, color)
        }
    }

    Text(distanciaTexto(metros), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
    Text("Sostén el teléfono plano. La flecha apunta hacia la barbería.", style = MaterialTheme.typography.bodySmall)
    if (calibrar) {
        Text(
            "La brújula está imprecisa: mueve el teléfono dibujando un 8 en el aire.",
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

private fun distanciaTexto(metros: Float): String = when {
    metros < 30f -> "¡Ya llegaste!"
    metros < 1000f -> "A ${metros.roundToInt()} m"
    else -> "A ${"%.1f".format(metros / 1000f)} km"
}
