package mx.tunegocio.barberagenda.ui

import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import mx.tunegocio.barberagenda.data.ApiClient
import mx.tunegocio.barberagenda.data.ApiException
import mx.tunegocio.barberagenda.data.Cita
import mx.tunegocio.barberagenda.data.Horario
import mx.tunegocio.barberagenda.data.Negocio
import mx.tunegocio.barberagenda.data.Oferta
import mx.tunegocio.barberagenda.data.ResultadoGoogle
import mx.tunegocio.barberagenda.data.Seccion
import mx.tunegocio.barberagenda.data.Sellos
import mx.tunegocio.barberagenda.data.Servicio
import mx.tunegocio.barberagenda.data.SesionStore
import java.time.LocalDate
import java.time.ZoneId

val ZONA_NEGOCIO: ZoneId = ZoneId.of("America/Mexico_City")

/** Manejo común de "cargando" y del mensaje de error. */
abstract class BaseViewModel : ViewModel() {
    var cargando by mutableStateOf(false)
        protected set
    var error by mutableStateOf<String?>(null)
        protected set

    protected fun lanzar(alFallar: (ApiException) -> Unit = {}, bloque: suspend () -> Unit): Job {
        cargando = true
        error = null
        return viewModelScope.launch {
            try {
                bloque()
            } catch (e: CancellationException) {
                throw e
            } catch (e: ApiException) {
                error = e.message
                alFallar(e)
            } catch (e: Exception) {
                error = "Respuesta inesperada del servidor."
            } finally {
                cargando = false
            }
        }
    }
}

// --- Acceso por código de WhatsApp -------------------------------------------

class AccesoViewModel(
    private val api: ApiClient,
    private val sesion: SesionStore,
) : BaseViewModel() {

    var telefono by mutableStateOf(sesion.telefono?.takeLast(10).orEmpty())
    var codigo by mutableStateOf("")
    var esperandoCodigo by mutableStateOf(false)
        private set
    var aviso by mutableStateOf<String?>(null)
        private set

    /** Cuenta de Google elegida que falta ligar a un número (solo la primera vez). */
    var enlaceGoogle by mutableStateOf<String?>(null)
        private set
    var correoGoogle by mutableStateOf<String?>(null)
        private set

    private val soloDigitos: String get() = telefono.filter { it.isDigit() }

    private val dispositivo: String get() = "${Build.MANUFACTURER} ${Build.MODEL}"

    /** El token de Google ya se obtuvo en la pantalla (necesita la Activity). */
    fun entrarConGoogle(idToken: String) {
        if (cargando) return
        lanzar {
            when (val r = api.entrarConGoogle(idToken, dispositivo)) {
                is ResultadoGoogle.Entro -> {
                    sesion.guardar(r.sesion)
                    enlaceGoogle = null
                }
                is ResultadoGoogle.FaltaTelefono -> {
                    enlaceGoogle = r.enlace
                    correoGoogle = r.email
                    esperandoCodigo = false
                    aviso = null
                }
            }
        }
    }

    fun errorDeGoogle(mensaje: String) {
        error = mensaje
    }

    fun olvidarGoogle() {
        enlaceGoogle = null
        correoGoogle = null
    }

    fun pedirCodigo() {
        if (cargando) return
        if (soloDigitos.length < 10) {
            error = "Escribe tu número de WhatsApp a 10 dígitos."
            return
        }
        lanzar {
            val r = api.pedirCodigo(soloDigitos)
            esperandoCodigo = true
            codigo = r.codigoDePrueba.orEmpty()
            aviso = if (r.codigoDePrueba != null) {
                "Modo pruebas: el servidor devolvió el código ${r.codigoDePrueba}."
            } else {
                "Te enviamos un código por WhatsApp. Vence en ${r.vigenciaMinutos} minutos."
            }
        }
    }

    fun entrar() {
        if (cargando) return
        val limpio = codigo.filter { it.isDigit() }
        if (limpio.length != 6) {
            error = "El código tiene 6 dígitos."
            return
        }
        lanzar {
            sesion.guardar(api.canjearCodigo(soloDigitos, limpio, dispositivo, enlaceGoogle))
            enlaceGoogle = null
            correoGoogle = null
            codigo = ""
            esperandoCodigo = false
            aviso = null
        }
    }

    fun cambiarNumero() {
        esperandoCodigo = false
        codigo = ""
        aviso = null
        error = null
    }
}

// --- Agendar ---------------------------------------------------------------

class AgendarViewModel(private val api: ApiClient) : BaseViewModel() {

    var secciones by mutableStateOf<List<Seccion>>(emptyList())
        private set
    var servicio by mutableStateOf<Servicio?>(null)
        private set
    var fecha by mutableStateOf(LocalDate.now(ZONA_NEGOCIO))
        private set
    var horarios by mutableStateOf<List<Horario>>(emptyList())
        private set
    var horario by mutableStateOf<Horario?>(null)
        private set
    var cargandoHorarios by mutableStateOf(false)
        private set
    var ofertas by mutableStateOf<List<Oferta>>(emptyList())
        private set

    /** Los próximos 14 días; el servidor dice cuáles están cerrados. */
    val dias: List<LocalDate>
        get() = (0L until 14L).map { LocalDate.now(ZONA_NEGOCIO).plusDays(it) }

    private var trabajoHorarios: Job? = null

    fun alEntrar() {
        if (secciones.isEmpty()) cargarCatalogo() else cargarHorarios()
        cargarOfertas()
    }

    /** Huecos avisados por WhatsApp que esta clienta todavía puede tomar. */
    fun cargarOfertas() {
        viewModelScope.launch {
            ofertas = try {
                api.ofertas()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                emptyList() // las ofertas son un extra: si fallan no se bloquea Agendar
            }
        }
    }

    fun aceptarOferta(id: Int, alTerminar: (Cita) -> Unit) {
        if (cargando) return
        lanzar(alFallar = { cargarOfertas() }) {
            val cita = api.aceptarOferta(id)
            ofertas = ofertas.filterNot { it.id == id }
            alTerminar(cita)
        }
    }

    fun cargarCatalogo() {
        lanzar { secciones = api.catalogo() }
    }

    fun elegirServicio(s: Servicio) {
        servicio = s
        cargarHorarios()
    }

    fun elegirFecha(d: LocalDate) {
        fecha = d
        cargarHorarios()
    }

    fun elegirHorario(h: Horario) {
        horario = h
    }

    /** No se guarda en caché: alguien más puede tomar el lugar en cualquier momento. */
    fun cargarHorarios(limpiarError: Boolean = true) {
        val s = servicio ?: return
        horario = null
        trabajoHorarios?.cancel()
        cargandoHorarios = true
        if (limpiarError) error = null
        trabajoHorarios = viewModelScope.launch {
            try {
                horarios = api.disponibilidad(s.id, fecha)
            } catch (e: CancellationException) {
                throw e
            } catch (e: ApiException) {
                horarios = emptyList()
                error = e.message
            } catch (e: Exception) {
                horarios = emptyList()
                error = "Respuesta inesperada del servidor."
            } finally {
                cargandoHorarios = false
            }
        }
    }

    fun agendar(alTerminar: (Cita) -> Unit) {
        val s = servicio ?: return
        val h = horario ?: return
        if (cargando) return
        lanzar(alFallar = { e ->
            // 409: el lugar ya no está. Se vuelve a preguntar al servidor.
            if (e.codigo == 409) cargarHorarios(limpiarError = false)
        }) {
            val cita = api.agendar(s.id, h)
            servicio = null
            horario = null
            horarios = emptyList()
            alTerminar(cita)
        }
    }
}

// --- Reprogramar -----------------------------------------------------------

class ReprogramarViewModel(private val api: ApiClient, val cita: Cita) : BaseViewModel() {

    var fecha by mutableStateOf(cita.inicio.toLocalDate())
        private set
    var horarios by mutableStateOf<List<Horario>>(emptyList())
        private set
    var horario by mutableStateOf<Horario?>(null)
        private set

    val dias: List<LocalDate>
        get() = (0L until 14L).map { LocalDate.now(ZONA_NEGOCIO).plusDays(it) }

    fun elegirFecha(d: LocalDate) {
        fecha = d
        cargarHorarios()
    }

    fun elegirHorario(h: Horario) {
        horario = h
    }

    fun cargarHorarios() {
        val servicio = cita.servicio ?: return
        horario = null
        lanzar {
            // La hora actual de la cita no cuenta como cambio.
            horarios = api.disponibilidad(servicio.id, fecha, cita.id)
                .filterNot { it.inicio.isEqual(cita.inicio) }
        }
    }

    fun confirmar(alTerminar: (Cita) -> Unit) {
        val h = horario ?: return
        if (cargando) return
        lanzar(alFallar = { e -> if (e.codigo == 409) cargarHorarios() }) {
            alTerminar(api.reprogramar(cita.id, h))
        }
    }
}

// --- Mis citas -----------------------------------------------------------------

class CitasViewModel(private val api: ApiClient) : BaseViewModel() {

    var citas by mutableStateOf<List<Cita>>(emptyList())
        private set
    var cancelando by mutableStateOf<Int?>(null)
        private set

    fun cargar() {
        lanzar { citas = api.misCitas() }
    }

    fun cancelar(id: Int) {
        if (cancelando != null) return
        cancelando = id
        lanzar {
            try {
                val actualizada = api.cancelar(id)
                citas = citas.map { if (it.id == id) actualizada else it }
            } finally {
                cancelando = null
            }
        }
    }
}

// --- Llegada con el QR del mostrador ------------------------------------------

class LlegadaViewModel(private val api: ApiClient) : BaseViewModel() {

    var resultado by mutableStateOf<String?>(null)
        private set

    fun registrar(codigo: String) {
        resultado = null
        lanzar { resultado = api.registrarLlegada(codigo) }
    }

    fun reiniciar() {
        resultado = null
        error = null
    }
}

// --- Brújula -------------------------------------------------------------------

class BrujulaViewModel(private val api: ApiClient) : BaseViewModel() {

    var negocio by mutableStateOf<Negocio?>(null)
        private set

    fun cargar() {
        if (negocio == null) lanzar { negocio = api.negocio() }
    }
}

// --- Tarjeta de sellos -------------------------------------------------------

class SellosViewModel(private val api: ApiClient) : BaseViewModel() {

    var sellos by mutableStateOf<Sellos?>(null)
        private set

    fun cargar() {
        lanzar { sellos = api.sellos() }
    }
}

// --- Perfil ----------------------------------------------------------------------

class PerfilViewModel(
    private val api: ApiClient,
    private val sesion: SesionStore,
) : BaseViewModel() {

    var telefono by mutableStateOf("")
        private set
    var nombre by mutableStateOf("")
    var aceptaAvisos by mutableStateOf(false)
        private set
    var mensaje by mutableStateOf<String?>(null)
        private set

    fun cargar() {
        mensaje = null
        lanzar {
            val p = api.perfil()
            telefono = p.telefono
            nombre = p.nombre.orEmpty()
            aceptaAvisos = p.aceptaAvisos
        }
    }

    fun guardarNombre() {
        mensaje = null
        lanzar {
            val p = api.actualizarPerfil(nombre = nombre.trim(), aceptaAvisos = null)
            nombre = p.nombre.orEmpty()
            mensaje = "Nombre guardado."
        }
    }

    /** El consentimiento se guarda en cuanto se mueve el interruptor. */
    fun cambiarAvisos(valor: Boolean) {
        val anterior = aceptaAvisos
        aceptaAvisos = valor
        mensaje = null
        lanzar(alFallar = { aceptaAvisos = anterior }) {
            val p = api.actualizarPerfil(nombre = null, aceptaAvisos = valor)
            aceptaAvisos = p.aceptaAvisos
            mensaje = if (p.aceptaAvisos) "Avisos activados." else "Avisos apagados."
        }
    }

    fun salir() {
        viewModelScope.launch {
            try {
                api.salir()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Aunque el servidor no conteste, la sesión se cierra en el teléfono.
            }
            sesion.limpiar()
        }
    }
}
