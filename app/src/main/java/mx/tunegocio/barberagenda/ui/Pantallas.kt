@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package mx.tunegocio.barberagenda.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import mx.tunegocio.barberagenda.BarberApp
import mx.tunegocio.barberagenda.BuildConfig
import mx.tunegocio.barberagenda.data.Cita
import mx.tunegocio.barberagenda.data.Servicio

// Diseño mínimo a propósito: aquí solo se resuelve que funcione.

// --- Acceso ------------------------------------------------------------------

@Composable
fun PantallaAcceso(app: BarberApp) {
    val vm: AccesoViewModel = viewModel { AccesoViewModel(app.api, app.sesion) }

    Scaffold(topBar = { TopAppBar(title = { Text("Barber Agenda") }) }) { relleno ->
        Column(
            modifier = Modifier
                .padding(relleno)
                .imePadding()
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Entra con tu WhatsApp", style = MaterialTheme.typography.headlineSmall)

            if (!vm.esperandoCodigo) {
                Text("Te mandamos un código de 6 dígitos. No necesitas contraseña.")
                OutlinedTextField(
                    value = vm.telefono,
                    onValueChange = { vm.telefono = it },
                    label = { Text("Número a 10 dígitos") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(
                    onClick = vm::pedirCodigo,
                    enabled = !vm.cargando,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(if (vm.cargando) "Enviando…" else "Enviarme el código") }
            } else {
                vm.aviso?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
                OutlinedTextField(
                    value = vm.codigo,
                    onValueChange = { vm.codigo = it.take(6) },
                    label = { Text("Código de 6 dígitos") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(
                    onClick = vm::entrar,
                    enabled = !vm.cargando,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(if (vm.cargando) "Entrando…" else "Entrar") }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = vm::cambiarNumero) { Text("Cambiar número") }
                    TextButton(onClick = vm::pedirCodigo, enabled = !vm.cargando) { Text("Reenviar código") }
                }
            }

            vm.error?.let { MensajeError(it) }

            if (BuildConfig.DEBUG) {
                Spacer(Modifier.height(24.dp))
                Text(
                    "Pruebas: usa 5550000001, 5550000002 o 5550000003.\nServidor: ${BuildConfig.API_BASE}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

// --- Contenedor con pestañas -------------------------------------------------------

private data class Pestana(val titulo: String, val icono: ImageVector)

private val PESTANAS = listOf(
    Pestana("Agendar", Icons.Filled.Add),
    Pestana("Mis citas", Icons.Filled.DateRange),
    Pestana("Tarjeta", Icons.Filled.Star),
    Pestana("Perfil", Icons.Filled.Person),
)

@Composable
fun PantallaPrincipal(app: BarberApp) {
    var pestana by rememberSaveable { mutableIntStateOf(0) }
    val contexto = LocalContext.current
    // Una sesión nueva arranca con ViewModels nuevos: no se mezclan datos de otra clienta.
    val clave = app.sesion.token.hashCode().toString()

    Scaffold(
        topBar = { TopAppBar(title = { Text(PESTANAS[pestana].titulo) }) },
        bottomBar = {
            NavigationBar {
                PESTANAS.forEachIndexed { i, p ->
                    NavigationBarItem(
                        selected = pestana == i,
                        onClick = { pestana = i },
                        icon = { Icon(p.icono, contentDescription = p.titulo) },
                        label = { Text(p.titulo) },
                    )
                }
            }
        },
    ) { relleno ->
        Box(Modifier.padding(relleno).fillMaxSize()) {
            when (pestana) {
                0 -> PantallaAgendar(
                    vm = viewModel(key = "agendar-$clave") { AgendarViewModel(app.api) },
                    alAgendar = { cita ->
                        Toast.makeText(
                            contexto,
                            "Cita confirmada: ${fechaLarga(cita.inicio)}",
                            Toast.LENGTH_LONG,
                        ).show()
                        pestana = 1
                    },
                )
                1 -> PantallaCitas(viewModel(key = "citas-$clave") { CitasViewModel(app.api) })
                2 -> PantallaSellos(viewModel(key = "sellos-$clave") { SellosViewModel(app.api) })
                else -> PantallaPerfil(viewModel(key = "perfil-$clave") { PerfilViewModel(app.api, app.sesion) })
            }
        }
    }
}

// --- Agendar ------------------------------------------------------------------

@Composable
fun PantallaAgendar(vm: AgendarViewModel, alAgendar: (Cita) -> Unit) {
    LaunchedEffect(Unit) { vm.alEntrar() }
    val servicio = vm.servicio
    val horario = vm.horario

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        vm.error?.let { item { MensajeError(it) } }

        item { Subtitulo("1. Elige el servicio") }
        if (vm.cargando && vm.secciones.isEmpty()) item { Cargando() }
        if (!vm.cargando && vm.secciones.isEmpty()) {
            item { TextButton(onClick = vm::cargarCatalogo) { Text("Reintentar") } }
        }

        vm.secciones.forEach { seccion ->
            item(key = "seccion-${seccion.id}") {
                Text(
                    seccion.nombre,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            items(seccion.servicios, key = { "servicio-${it.id}" }) { s ->
                FilaServicio(s, elegido = s.id == servicio?.id) { vm.elegirServicio(s) }
            }
        }

        if (servicio != null) {
            item { Subtitulo("2. Elige el día") }
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

            item { Subtitulo("3. Elige la hora") }
            item {
                when {
                    vm.cargandoHorarios -> Cargando()
                    vm.horarios.isEmpty() -> Text("No hay horarios libres ese día. Prueba otro día.")
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

            if (horario != null) {
                item {
                    Text(
                        "${servicio.nombre} · ${fechaLarga(horario.inicio)} · ${precioTexto(servicio.precio)}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                item {
                    Button(
                        onClick = { vm.agendar(alAgendar) },
                        enabled = !vm.cargando,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    ) {
                        Text(
                            if (vm.cargando) "Agendando…"
                            else "Confirmar cita"
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FilaServicio(s: Servicio, elegido: Boolean, alElegir: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = alElegir),
        colors = if (elegido) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        } else {
            CardDefaults.cardColors()
        },
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = elegido, onClick = alElegir)
            Column(Modifier.weight(1f)) {
                Text(s.nombre, fontWeight = FontWeight.SemiBold)
                Text(
                    "${s.duracionMinutos} min · ${precioTexto(s.precio)}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

// --- Mis citas ---------------------------------------------------------------

@Composable
fun PantallaCitas(vm: CitasViewModel) {
    LaunchedEffect(Unit) { vm.cargar() }
    var porCancelar by remember { mutableStateOf<Cita?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.weight(1f))
                TextButton(onClick = vm::cargar, enabled = !vm.cargando) { Text("Actualizar") }
            }
        }
        vm.error?.let { item { MensajeError(it) } }
        if (vm.cargando && vm.citas.isEmpty()) item { Cargando() }
        if (!vm.cargando && vm.citas.isEmpty() && vm.error == null) {
            item { Text("Todavía no tienes citas. Agenda una en la pestaña Agendar.") }
        }
        val ahora = java.time.OffsetDateTime.now()
        val (proximas, historial) = vm.citas.partition {
            it.estado == "confirmada" && it.inicio.isAfter(ahora)
        }
        if (proximas.isNotEmpty()) {
            item { Subtitulo("Próximas") }
            items(proximas.sortedBy { it.inicio }, key = { it.id }) { c ->
                TarjetaCita(c, cancelando = vm.cancelando == c.id) { porCancelar = c }
            }
        }
        if (historial.isNotEmpty()) {
            item { Subtitulo("Historial") }
            items(historial, key = { it.id }) { c ->
                TarjetaCita(c, cancelando = vm.cancelando == c.id) { porCancelar = c }
            }
        }
    }

    porCancelar?.let { c ->
        AlertDialog(
            onDismissRequest = { porCancelar = null },
            title = { Text("¿Cancelar la cita?") },
            text = {
                Text("${c.servicio?.nombre ?: "Cita"} el ${fechaLarga(c.inicio)}. El horario quedará libre para otra persona.")
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.cancelar(c.id)
                    porCancelar = null
                }) { Text("Sí, cancelar") }
            },
            dismissButton = {
                TextButton(onClick = { porCancelar = null }) { Text("No") }
            },
        )
    }
}

@Composable
private fun TarjetaCita(c: Cita, cancelando: Boolean, alCancelar: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(c.servicio?.nombre ?: "Servicio", fontWeight = FontWeight.SemiBold)
            Text(fechaLarga(c.inicio))
            c.seccion?.let { Text(it.nombre, style = MaterialTheme.typography.bodySmall) }
            Text(
                estadoTexto(c.estado),
                color = if (c.estado == "confirmada") MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outline,
                style = MaterialTheme.typography.labelLarge,
            )
            if (c.puedeCancelarse) {
                OutlinedButton(onClick = alCancelar, enabled = !cancelando) {
                    Text(if (cancelando) "Cancelando…" else "Cancelar cita")
                }
            }
        }
    }
}

// --- Tarjeta de sellos ----------------------------------------------------------

@Composable
fun PantallaSellos(vm: SellosViewModel) {
    LaunchedEffect(Unit) { vm.cargar() }
    val abrir = LocalUriHandler.current
    val s = vm.sellos

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        vm.error?.let { MensajeError(it) }
        if (s == null) {
            if (vm.cargando) Cargando()
            return@Column
        }

        Text("Un sello por cada cita completada.", style = MaterialTheme.typography.bodyMedium)

        // Cuadrícula de 5 por fila: lleno = sello ganado.
        val primario = MaterialTheme.colorScheme.primary
        (1..s.meta).chunked(5).forEach { fila ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                fila.forEach { n ->
                    val lleno = n <= s.sellos
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .background(if (lleno) primario else MaterialTheme.colorScheme.surface, CircleShape)
                            .border(2.dp, primario, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            if (lleno) "✓" else n.toString(),
                            color = if (lleno) MaterialTheme.colorScheme.onPrimary else primario,
                        )
                    }
                }
                // Relleno para que la última fila no se estire.
                repeat(5 - fila.size) { Spacer(Modifier.weight(1f)) }
            }
        }

        Text(
            "${s.sellos} de ${s.meta} sellos. Te faltan ${s.faltan} para: ${s.premio}.",
            fontWeight = FontWeight.SemiBold,
        )

        if (s.premiosDisponibles > 0) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Text(
                    "Tienes ${s.premiosDisponibles} premio(s) listo(s): ${s.premio}. Muéstralo en tu próxima visita.",
                    modifier = Modifier.padding(16.dp),
                )
            }
        }

        s.tarjetaUrl?.let { url ->
            OutlinedButton(onClick = { abrir.openUri(url) }, modifier = Modifier.fillMaxWidth()) {
                Text("Abrir mi tarjeta en el navegador")
            }
        }

        TextButton(onClick = vm::cargar, enabled = !vm.cargando) { Text("Actualizar") }
    }
}

// --- Perfil --------------------------------------------------------------------

@Composable
fun PantallaPerfil(vm: PerfilViewModel) {
    LaunchedEffect(Unit) { vm.cargar() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        vm.error?.let { MensajeError(it) }

        Text("WhatsApp: ${vm.telefono}")

        OutlinedTextField(
            value = vm.nombre,
            onValueChange = { vm.nombre = it },
            label = { Text("Nombre") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Button(onClick = vm::guardarNombre, enabled = !vm.cargando) { Text("Guardar nombre") }

        HorizontalDivider()

        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Avisos de espacios libres", fontWeight = FontWeight.SemiBold)
                Text(
                    "Te escribimos por WhatsApp cuando se libera un horario. Puedes apagarlo cuando quieras.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Switch(
                checked = vm.aceptaAvisos,
                onCheckedChange = vm::cambiarAvisos,
                enabled = !vm.cargando,
            )
        }

        vm.mensaje?.let { Text(it, color = MaterialTheme.colorScheme.primary) }

        HorizontalDivider()

        OutlinedButton(onClick = vm::salir, modifier = Modifier.fillMaxWidth()) {
            Text("Cerrar sesión")
        }
    }
}

// --- Piezas comunes -------------------------------------------------------------

@Composable
private fun Subtitulo(texto: String) {
    Text(
        texto,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 12.dp),
    )
}

@Composable
private fun Cargando() {
    Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(Modifier.size(32.dp))
    }
}

@Composable
fun MensajeError(texto: String) {
    Text(texto, color = MaterialTheme.colorScheme.error)
}
