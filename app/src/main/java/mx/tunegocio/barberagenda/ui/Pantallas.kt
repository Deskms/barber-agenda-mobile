@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package mx.tunegocio.barberagenda.ui

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.foundation.layout.width
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
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.fragment.app.FragmentActivity
import mx.tunegocio.barberagenda.BarberApp
import mx.tunegocio.barberagenda.BuildConfig
import mx.tunegocio.barberagenda.data.Cita
import mx.tunegocio.barberagenda.data.Servicio

// Estilo minimalista tipo iOS (ver Tema.kt): fondo gris, grupos blancos,
// títulos grandes y un solo color de acción (azul ballena).

// --- Acceso ------------------------------------------------------------------

@Composable
fun PantallaAcceso(app: BarberApp) {
    val vm: AccesoViewModel = viewModel { AccesoViewModel(app.api, app.sesion) }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { relleno ->
        Column(
            modifier = Modifier
                .padding(relleno)
                .imePadding()
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Logo()
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Barber Agenda", style = MaterialTheme.typography.headlineMedium)
                Text("Entra con tu WhatsApp", style = MaterialTheme.typography.bodyLarge, color = Paleta.Gris2)
            }

            if (!vm.esperandoCodigo) {
                Text("Te mandamos un código de 6 dígitos. No necesitas contraseña.", style = MaterialTheme.typography.bodyMedium, color = Paleta.Gris2)
                OutlinedTextField(
                    value = vm.telefono,
                    onValueChange = { vm.telefono = it },
                    label = { Text("Número a 10 dígitos") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    shape = RoundedCornerShape(12.dp),
                    colors = coloresCampo(),
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(
                    onClick = vm::pedirCodigo,
                    enabled = !vm.cargando,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) { Text(if (vm.cargando) "Enviando…" else "Enviarme el código") }
            } else {
                vm.aviso?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
                OutlinedTextField(
                    value = vm.codigo,
                    onValueChange = { vm.codigo = it.take(6) },
                    label = { Text("Código de 6 dígitos") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    shape = RoundedCornerShape(12.dp),
                    colors = coloresCampo(),
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(
                    onClick = vm::entrar,
                    enabled = !vm.cargando,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(52.dp),
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
                    color = Paleta.Gris,
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
    var escaneando by rememberSaveable { mutableStateOf(false) }
    var brujula by rememberSaveable { mutableStateOf(false) }
    var sensores by rememberSaveable { mutableStateOf(false) }
    var citaAMover by remember { mutableStateOf<Cita?>(null) }

    // Acelerómetro: agitar el teléfono abre la tarjeta de sellos desde cualquier pestaña.
    DetectorDeAgitar {
        escaneando = false
        brujula = false
        sensores = false
        citaAMover = null
        pestana = 2
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        // Título grande a la izquierda, como en las apps de iOS.
        topBar = {
            TopAppBar(
                title = { Text(PESTANAS[pestana].titulo, style = MaterialTheme.typography.headlineMedium) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        // Barra de pestañas blanca con línea fina arriba y sin "pastilla".
        bottomBar = {
            Column {
                HorizontalDivider(thickness = 0.5.dp, color = Paleta.LineaFuerte)
                NavigationBar(containerColor = Paleta.Superficie, tonalElevation = 0.dp) {
                    PESTANAS.forEachIndexed { i, p ->
                        NavigationBarItem(
                            selected = pestana == i,
                            onClick = { pestana = i },
                            icon = { Icon(p.icono, contentDescription = p.titulo) },
                            label = { Text(p.titulo, style = MaterialTheme.typography.labelSmall) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Paleta.Ballena,
                                selectedTextColor = Paleta.Ballena,
                                indicatorColor = Color.Transparent,
                                unselectedIconColor = Paleta.Gris,
                                unselectedTextColor = Paleta.Gris,
                            ),
                        )
                    }
                }
            }
        },
    ) { relleno ->
        Box(Modifier.padding(relleno).fillMaxSize()) {
            if (escaneando) {
                PantallaLlegada(
                    vm = viewModel(key = "llegada-$clave") { LlegadaViewModel(app.api) },
                    alCerrar = {
                        escaneando = false
                        pestana = 1
                    },
                )
                return@Box
            }
            if (sensores) {
                PantallaDiagnosticoSensores(alCerrar = { sensores = false })
                return@Box
            }
            citaAMover?.let { cita ->
                PantallaReprogramar(
                    vm = viewModel(key = "mover-${cita.id}-$clave") { ReprogramarViewModel(app.api, cita) },
                    alTerminar = { movida ->
                        Toast.makeText(contexto, "Nuevo horario: ${fechaLarga(movida.inicio)}", Toast.LENGTH_LONG).show()
                        citaAMover = null
                        pestana = 1
                    },
                    alCerrar = { citaAMover = null },
                )
                return@Box
            }
            if (brujula) {
                PantallaBrujula(
                    vm = viewModel(key = "brujula-$clave") { BrujulaViewModel(app.api) },
                    alCerrar = { brujula = false },
                )
                return@Box
            }
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
                1 -> PantallaCitas(
                    vm = viewModel(key = "citas-$clave") { CitasViewModel(app.api) },
                    alLlegar = { escaneando = true },
                    alComoLlegar = { brujula = true },
                    alMover = { citaAMover = it },
                )
                2 -> PantallaSellos(viewModel(key = "sellos-$clave") { SellosViewModel(app.api) })
                else -> PantallaPerfil(
                    vm = viewModel(key = "perfil-$clave") { PerfilViewModel(app.api, app.sesion) },
                    app = app,
                    alSensores = { sensores = true },
                )
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

        if (vm.ofertas.isNotEmpty()) {
            items(vm.ofertas, key = { "oferta-${it.id}" }) { o ->
                TarjetaOferta(o, ocupado = vm.cargando) { vm.aceptarOferta(o.id, alAgendar) }
            }
        }

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
                    color = Paleta.Cafe,
                    modifier = Modifier.padding(start = 4.dp, top = 8.dp),
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
                    Grupo(Modifier.padding(top = 8.dp)) {
                        Text(servicio.nombre, style = MaterialTheme.typography.titleMedium)
                        Text(fechaLarga(horario.inicio), style = MaterialTheme.typography.bodyMedium)
                        Text(precioTexto(servicio.precio), style = MaterialTheme.typography.bodySmall)
                    }
                }
                item {
                    Button(
                        onClick = { vm.agendar(alAgendar) },
                        enabled = !vm.cargando,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(52.dp),
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
        colors = CardDefaults.cardColors(containerColor = Paleta.Superficie),
        border = if (elegido) BorderStroke(1.5.dp, Paleta.Ballena) else null,
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(s.nombre, style = MaterialTheme.typography.bodyLarge)
                Text("${s.duracionMinutos} min · ${precioTexto(s.precio)}", style = MaterialTheme.typography.bodySmall)
            }
            if (elegido) {
                Icon(Icons.Filled.Check, contentDescription = "Elegido", tint = Paleta.Ballena)
            }
        }
    }
}

// --- Mis citas ---------------------------------------------------------------

@Composable
fun PantallaCitas(vm: CitasViewModel, alLlegar: () -> Unit, alComoLlegar: () -> Unit, alMover: (Cita) -> Unit) {
    LaunchedEffect(Unit) { vm.cargar() }
    var porCancelar by remember { mutableStateOf<Cita?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Button(
                onClick = alLlegar,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                Text("Ya llegué: escanear QR del mostrador")
            }
        }
        item {
            OutlinedButton(
                onClick = alComoLlegar,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = Paleta.Superficie),
                border = BorderStroke(1.dp, Paleta.Linea),
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                Text("Cómo llegar (brújula)")
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.weight(1f))
                TextButton(onClick = vm::cargar, enabled = !vm.cargando) { Text("Actualizar") }
            }
        }
        vm.error?.let { item { MensajeError(it) } }
        if (vm.cargando && vm.citas.isEmpty()) item { Cargando() }
        if (!vm.cargando && vm.citas.isEmpty() && vm.error == null) {
            item { Text("Todavía no tienes citas. Agenda una en la pestaña Agendar.", color = Paleta.Gris2) }
        }
        val ahora = java.time.OffsetDateTime.now()
        val (proximas, historial) = vm.citas.partition {
            it.estado == "confirmada" && it.inicio.isAfter(ahora)
        }
        if (proximas.isNotEmpty()) {
            item { Subtitulo("Próximas") }
            items(proximas.sortedBy { it.inicio }, key = { it.id }) { c ->
                TarjetaCita(c, cancelando = vm.cancelando == c.id, alMover = { alMover(c) }) { porCancelar = c }
            }
        }
        if (historial.isNotEmpty()) {
            item { Subtitulo("Historial") }
            items(historial, key = { it.id }) { c ->
                TarjetaCita(c, cancelando = vm.cancelando == c.id, alMover = { alMover(c) }) { porCancelar = c }
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
private fun TarjetaCita(c: Cita, cancelando: Boolean, alMover: () -> Unit, alCancelar: () -> Unit) {
    val colorEstado = when (c.estado) {
        "confirmada" -> Paleta.Ballena
        "completada" -> Paleta.Verde
        "no_asistio" -> Paleta.Oxido
        else -> Paleta.Gris
    }
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Paleta.Superficie)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(c.servicio?.nombre ?: "Servicio", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Box(Modifier.size(8.dp).background(colorEstado, CircleShape))
                Spacer(Modifier.width(6.dp))
                Text(estadoTexto(c.estado), color = colorEstado, style = MaterialTheme.typography.labelLarge)
            }
            Text(fechaLarga(c.inicio), style = MaterialTheme.typography.bodyMedium)
            c.seccion?.let { Text(it.nombre, style = MaterialTheme.typography.bodySmall) }
            c.llegadaEn?.let {
                Text("Llegaste a las ${horaCorta(it)}", color = Paleta.Cafe, style = MaterialTheme.typography.bodyMedium)
            }
            if (c.puedeCancelarse) {
                HorizontalDivider(Modifier.padding(vertical = 8.dp), thickness = 0.5.dp, color = Paleta.Linea)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = alMover, enabled = !cancelando) { Text("Cambiar horario") }
                    Spacer(Modifier.weight(1f))
                    TextButton(
                        onClick = alCancelar,
                        enabled = !cancelando,
                        colors = ButtonDefaults.textButtonColors(contentColor = Paleta.Oxido),
                    ) {
                        Text(if (cancelando) "Cancelando…" else "Cancelar")
                    }
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

        // Tarjeta de cartón café: un sello lleno por cada cita completada.
        Grupo {
            Text("${s.sellos} de ${s.meta}", style = MaterialTheme.typography.headlineSmall)
            Text("Te faltan ${s.faltan} para: ${s.premio}", style = MaterialTheme.typography.bodyMedium, color = Paleta.Gris2)
            Spacer(Modifier.height(8.dp))
            (1..s.meta).chunked(5).forEach { fila ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(bottom = 10.dp)) {
                    fila.forEach { n ->
                        val lleno = n <= s.sellos
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .border(1.5.dp, if (lleno) Paleta.Cafe else Paleta.LineaFuerte, CircleShape)
                                .background(if (lleno) Paleta.Cafe else Paleta.Superficie, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                if (lleno) "✓" else n.toString(),
                                color = if (lleno) Color.White else Paleta.Gris,
                                fontWeight = if (lleno) FontWeight.Bold else FontWeight.Normal,
                            )
                        }
                    }
                    // Relleno para que la última fila no se estire.
                    repeat(5 - fila.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }

        Text(
            "Un sello por cada cita completada. Agita el teléfono para abrir esta tarjeta.",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 4.dp),
        )

        if (s.premiosDisponibles > 0) {
            Card(colors = CardDefaults.cardColors(containerColor = Paleta.CafeClaro)) {
                Text(
                    "Tienes ${s.premiosDisponibles} premio(s) listo(s): ${s.premio}. Muéstralo en tu próxima visita.",
                    color = Paleta.Cafe,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }

        s.tarjetaUrl?.let { url ->
            TextButton(onClick = { abrir.openUri(url) }, modifier = Modifier.fillMaxWidth()) {
                Text("Abrir mi tarjeta en el navegador")
            }
        }

        TextButton(onClick = vm::cargar, enabled = !vm.cargando) { Text("Actualizar") }
    }
}

// --- Perfil --------------------------------------------------------------------

@Composable
fun PantallaPerfil(vm: PerfilViewModel, app: BarberApp, alSensores: () -> Unit) {
    LaunchedEffect(Unit) { vm.cargar() }
    val actividad = LocalContext.current as FragmentActivity
    val hayHuella = remember { huellaDisponible(actividad) }
    var errorHuella by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        vm.error?.let { MensajeError(it) }

        EncabezadoGrupo("Tus datos")
        Grupo {
            Text("WhatsApp", style = MaterialTheme.typography.bodySmall)
            Text(vm.telefono, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = vm.nombre,
                onValueChange = { vm.nombre = it },
                label = { Text("Nombre") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = coloresCampo(),
                modifier = Modifier.fillMaxWidth(),
            )
            TextButton(onClick = vm::guardarNombre, enabled = !vm.cargando) { Text("Guardar nombre") }
        }

        EncabezadoGrupo("Preferencias")
        Grupo {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f).padding(end = 12.dp)) {
                    Text("Avisos de espacios libres", style = MaterialTheme.typography.bodyLarge)
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

            vm.mensaje?.let { Text(it, color = Paleta.Ballena, style = MaterialTheme.typography.bodySmall) }

            HorizontalDivider(Modifier.padding(vertical = 12.dp), thickness = 0.5.dp, color = Paleta.Linea)

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f).padding(end = 12.dp)) {
                    Text("Abrir con huella o rostro", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        if (hayHuella) "Nadie ve tus citas aunque tome tu teléfono."
                        else "Tu teléfono no tiene huella o rostro registrados.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Switch(
                    checked = app.sesion.pedirHuella,
                    enabled = hayHuella,
                    onCheckedChange = { activar ->
                        errorHuella = null
                        if (activar) {
                            // Se confirma con la huella antes de activarla.
                            pedirHuella(
                                actividad,
                                alExito = {
                                    app.sesion.cambiarHuella(true)
                                    app.desbloqueada = true
                                },
                                alFallar = { errorHuella = it },
                            )
                        } else {
                            app.sesion.cambiarHuella(false)
                        }
                    },
                )
            }
        }
        errorHuella?.let { MensajeError(it) }

        // Solo en la versión de pruebas: evidencia de los sensores para la presentación.
        if (BuildConfig.DEBUG) {
            Grupo {
                TextButton(onClick = alSensores, modifier = Modifier.fillMaxWidth()) {
                    Text("Sensores del teléfono (pruebas)")
                }
            }
        }

        Grupo {
            TextButton(
                onClick = vm::salir,
                colors = ButtonDefaults.textButtonColors(contentColor = Paleta.Oxido),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Cerrar sesión")
            }
        }
    }
}

// --- Llegada -------------------------------------------------------------------

@Composable
fun PantallaLlegada(vm: LlegadaViewModel, alCerrar: () -> Unit) {
    val cerrar = {
        vm.reiniciar()
        alCerrar()
    }
    val resultado = vm.resultado
    val error = vm.error

    when {
        vm.cargando -> Cargando()
        resultado != null || error != null -> Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (resultado != null) {
                Text(resultado, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            } else {
                MensajeError(error.orEmpty())
            }
            Button(onClick = cerrar, modifier = Modifier.fillMaxWidth()) { Text("Listo") }
            if (resultado == null) {
                OutlinedButton(onClick = vm::reiniciar, modifier = Modifier.fillMaxWidth()) {
                    Text("Escanear de nuevo")
                }
            }
        }
        else -> PantallaEscaner(alLeer = vm::registrar, alCerrar = cerrar)
    }
}

// --- Piezas comunes -------------------------------------------------------------

@Composable
private fun Subtitulo(texto: String) {
    EncabezadoGrupo(texto, Modifier.padding(top = 12.dp))
}

/** Encabezado pequeño en mayúsculas sobre un grupo, como en Ajustes de iOS. */
@Composable
internal fun EncabezadoGrupo(texto: String, modifier: Modifier = Modifier) {
    Text(texto.uppercase(), style = EstiloGrupo, modifier = modifier.padding(start = 4.dp, top = 4.dp))
}

/** Grupo blanco con esquinas redondeadas (lista agrupada de iOS). */
@Composable
internal fun Grupo(modifier: Modifier = Modifier, contenido: @Composable () -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Paleta.Superficie),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) { contenido() }
    }
}

/** Cuadro negro con las iniciales: el logotipo provisional. */
@Composable
private fun Logo() {
    Box(
        modifier = Modifier.size(64.dp).background(Paleta.Negro, RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text("BA", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
    }
}

@Composable
internal fun coloresCampo() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = Paleta.Superficie,
    unfocusedContainerColor = Paleta.Superficie,
    unfocusedBorderColor = Paleta.Linea,
    focusedBorderColor = Paleta.Ballena,
)

@Composable
internal fun Cargando() {
    Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(Modifier.size(28.dp), color = Paleta.Ballena, strokeWidth = 2.5.dp)
    }
}

@Composable
fun MensajeError(texto: String) {
    Text(
        texto,
        color = Paleta.Oxido,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier
            .fillMaxWidth()
            .background(Paleta.OxidoClaro, RoundedCornerShape(12.dp))
            .padding(12.dp),
    )
}
