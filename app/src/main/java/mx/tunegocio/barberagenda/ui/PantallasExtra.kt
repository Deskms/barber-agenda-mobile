@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package mx.tunegocio.barberagenda.ui

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import mx.tunegocio.barberagenda.data.Cita
import mx.tunegocio.barberagenda.data.Oferta

// --- Ofertas de huecos (en la pestaña Agendar) --------------------------------

@Composable
fun TarjetaOferta(oferta: Oferta, ocupado: Boolean, alAceptar: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Se liberó un espacio para ti", fontWeight = FontWeight.SemiBold)
            Text("${oferta.servicio.nombre} · ${fechaLarga(oferta.inicio)}")
            Text(precioTexto(oferta.servicio.precio), style = MaterialTheme.typography.bodySmall)
            Button(onClick = alAceptar, enabled = !ocupado) {
                Text(if (ocupado) "Apartando…" else "Lo quiero")
            }
        }
    }
}

// --- Reprogramar -------------------------------------------------------------

@Composable
fun PantallaReprogramar(vm: ReprogramarViewModel, alTerminar: (Cita) -> Unit, alCerrar: () -> Unit) {
    LaunchedEffect(Unit) { vm.cargarHorarios() }
    val horario = vm.horario

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Column {
                Text("Cambiar horario", style = MaterialTheme.typography.titleLarge)
                Text("${vm.cita.servicio?.nombre ?: "Cita"} · ahora: ${fechaLarga(vm.cita.inicio)}")
            }
        }
        vm.error?.let { item { MensajeError(it) } }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(vm.dias) { d ->
                    FilterChip(
                        selected = d == vm.fecha,
                        onClick = { vm.elegirFecha(d) },
                        label = { Text(diaCorto(d)) },
                    )
                }
            }
        }

        item {
            when {
                vm.cargando && vm.horarios.isEmpty() -> Cargando()
                vm.horarios.isEmpty() -> Text("No hay otros horarios libres ese día. Prueba otro día.")
                else -> FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    vm.horarios.forEach { h ->
                        FilterChip(
                            selected = h == horario,
                            onClick = { vm.elegirHorario(h) },
                            label = { Text(h.hora) },
                        )
                    }
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { vm.confirmar(alTerminar) },
                    enabled = horario != null && !vm.cargando,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (horario == null) "Elige un horario" else "Cambiar a ${diaCorto(vm.fecha)} · ${horario.hora}")
                }
                OutlinedButton(onClick = alCerrar, modifier = Modifier.fillMaxWidth()) { Text("Volver") }
            }
        }
    }
}

// --- Diagnóstico de sensores ---------------------------------------------------

/**
 * Lista los sensores que el teléfono reporta en SensorManager y muestra
 * lecturas en vivo del acelerómetro y del magnetómetro. Sirve como evidencia
 * de que se usan sensores físicos reales y no lecturas inventadas.
 */
@Composable
fun PantallaDiagnosticoSensores(alCerrar: () -> Unit) {
    val contexto = LocalContext.current
    val ciclo = LocalLifecycleOwner.current
    val gestor = remember { contexto.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val lista = remember { gestor.getSensorList(Sensor.TYPE_ALL) }

    var acel by remember { mutableStateOf<FloatArray?>(null) }
    var mag by remember { mutableStateOf<FloatArray?>(null) }

    DisposableEffect(ciclo) {
        val oyente = object : SensorEventListener {
            override fun onSensorChanged(evento: SensorEvent) {
                when (evento.sensor.type) {
                    Sensor.TYPE_ACCELEROMETER -> acel = evento.values.copyOf(3)
                    Sensor.TYPE_MAGNETIC_FIELD -> mag = evento.values.copyOf(3)
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, precision: Int) = Unit
        }
        val observador = LifecycleEventObserver { _, evento ->
            when (evento) {
                Lifecycle.Event.ON_RESUME -> listOf(Sensor.TYPE_ACCELEROMETER, Sensor.TYPE_MAGNETIC_FIELD)
                    .mapNotNull { gestor.getDefaultSensor(it) }
                    .forEach { gestor.registerListener(oyente, it, SensorManager.SENSOR_DELAY_UI) }
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

    val acelerometro = gestor.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    val magnetometro = gestor.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item { Text("Sensores del teléfono", style = MaterialTheme.typography.titleLarge) }

        item {
            FichaSensor(
                titulo = "Acelerómetro (TYPE_ACCELEROMETER)",
                sensor = acelerometro,
                lectura = acel?.let { "x=%.2f  y=%.2f  z=%.2f m/s²".format(it[0], it[1], it[2]) },
                uso = "Agitar el teléfono abre la tarjeta de sellos.",
            )
        }
        item {
            FichaSensor(
                titulo = "Magnetómetro (TYPE_MAGNETIC_FIELD)",
                sensor = magnetometro,
                lectura = mag?.let { "x=%.1f  y=%.1f  z=%.1f µT".format(it[0], it[1], it[2]) },
                uso = "Brújula de \"Cómo llegar\" (con el acelerómetro y el GPS).",
            )
        }

        item { Text("Todos los sensores que reporta este teléfono (${lista.size})", fontWeight = FontWeight.SemiBold) }
        items(lista) { s ->
            Text("• ${s.name} — ${s.vendor} (tipo ${s.type})", style = MaterialTheme.typography.bodySmall)
        }

        item { OutlinedButton(onClick = alCerrar, modifier = Modifier.fillMaxWidth()) { Text("Volver") } }
    }
}

@Composable
private fun FichaSensor(titulo: String, sensor: Sensor?, lectura: String?, uso: String) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(titulo, fontWeight = FontWeight.SemiBold)
            if (sensor == null) {
                MensajeError("Este teléfono no tiene este sensor.")
            } else {
                Text("${sensor.name} · ${sensor.vendor}", style = MaterialTheme.typography.bodySmall)
                Text(lectura ?: "Esperando lectura…", style = MaterialTheme.typography.bodyMedium)
            }
            Text(uso, style = MaterialTheme.typography.bodySmall)
        }
    }
}
