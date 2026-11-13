package mx.tunegocio.barberagenda.data

import org.json.JSONArray
import org.json.JSONObject
import java.time.OffsetDateTime

// Espejo de los JSON de docs/API_MOVIL.md del repositorio barber-agenda-web.

data class Cliente(
    val id: Int,
    val telefono: String,
    val nombre: String?,
    val aceptaAvisos: Boolean,
)

data class Servicio(
    val id: Int,
    val nombre: String,
    val duracionMinutos: Int,
    val precio: Double?,
    val seccionId: Int?,
)

data class Seccion(
    val id: Int,
    val nombre: String,
    val servicios: List<Servicio>,
)

/** Un inicio posible para el servicio elegido. */
data class Horario(
    val inicio: OffsetDateTime,
    val fin: OffsetDateTime,
    val hora: String,
)

data class Cita(
    val id: Int,
    val estado: String,
    val origen: String?,
    val inicio: OffsetDateTime,
    val fin: OffsetDateTime,
    val puedeCancelarse: Boolean,
    val servicio: Servicio?,
    val seccion: Seccion?,
)

/** Tarjeta de sellos: un sello por cada cita completada. */
data class Sellos(
    val meta: Int,
    val premio: String,
    val sellos: Int,
    val faltan: Int,
    val premiosDisponibles: Int,
    val totalGanados: Int,
    val tarjetaUrl: String?,
)

data class CodigoEnviado(
    val mensaje: String,
    val vigenciaMinutos: Int,
    /** Solo llega cuando el servidor no está en producción. */
    val codigoDePrueba: String?,
)

data class SesionIniciada(
    val token: String,
    val cliente: Cliente,
)

// --- Lectura de JSON ------------------------------------------------------

/** optString devuelve "null" cuando el valor es JSON null; esto devuelve null. */
internal fun JSONObject.texto(llave: String): String? =
    if (isNull(llave)) null else optString(llave)

internal fun JSONObject.decimal(llave: String): Double? =
    if (isNull(llave)) null else optDouble(llave)

internal fun JSONObject.entero(llave: String): Int? =
    if (isNull(llave)) null else optInt(llave)

internal fun <T> JSONArray?.lista(convertir: (JSONObject) -> T): List<T> {
    if (this == null) return emptyList()
    return (0 until length()).map { convertir(getJSONObject(it)) }
}

internal fun clienteDe(j: JSONObject) = Cliente(
    id = j.getInt("id"),
    telefono = j.texto("telefono").orEmpty(),
    nombre = j.texto("nombre"),
    aceptaAvisos = j.optBoolean("acepta_avisos", false),
)

internal fun servicioDe(j: JSONObject) = Servicio(
    id = j.getInt("id"),
    nombre = j.texto("nombre").orEmpty(),
    duracionMinutos = j.optInt("duracion_minutos", 0),
    precio = j.decimal("precio"),
    seccionId = j.entero("seccion_id"),
)

internal fun seccionDe(j: JSONObject) = Seccion(
    id = j.getInt("id"),
    nombre = j.texto("nombre").orEmpty(),
    servicios = j.optJSONArray("servicios").lista(::servicioDe),
)

internal fun horarioDe(j: JSONObject) = Horario(
    inicio = OffsetDateTime.parse(j.getString("inicio")),
    fin = OffsetDateTime.parse(j.getString("fin")),
    hora = j.texto("hora").orEmpty(),
)

internal fun sellosDe(j: JSONObject) = Sellos(
    meta = j.optInt("meta", 10),
    premio = j.texto("premio").orEmpty(),
    sellos = j.optInt("sellos", 0),
    faltan = j.optInt("faltan", 0),
    premiosDisponibles = j.optInt("premios_disponibles", 0),
    totalGanados = j.optInt("total_ganados", 0),
    tarjetaUrl = j.texto("tarjeta_url"),
)

internal fun citaDe(j: JSONObject) = Cita(
    id = j.getInt("id"),
    estado = j.texto("estado").orEmpty(),
    origen = j.texto("origen"),
    inicio = OffsetDateTime.parse(j.getString("inicio")),
    fin = OffsetDateTime.parse(j.getString("fin")),
    puedeCancelarse = j.optBoolean("puede_cancelarse", false),
    servicio = j.optJSONObject("servicio")?.let(::servicioDe),
    seccion = j.optJSONObject("seccion")?.let(::seccionDe),
)
