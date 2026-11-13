package mx.tunegocio.barberagenda.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

/** Error con el código HTTP (0 = sin conexión) y un mensaje listo para mostrar. */
class ApiException(val codigo: Int, mensaje: String) : Exception(mensaje)

/**
 * Cliente de /api/movil. La app no guarda reglas propias: todo lo decide el
 * servidor (disponibilidad, choques, quién puede cancelar).
 */
class ApiClient(
    baseUrl: String,
    private val sesion: SesionStore,
) {
    private val base = baseUrl.trimEnd('/') + "/"

    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    // --- Acceso ---------------------------------------------------------

    suspend fun pedirCodigo(telefono: String): CodigoEnviado {
        val j = llamar("POST", "auth/codigo", JSONObject().put("telefono", telefono))
        return CodigoEnviado(
            mensaje = j.texto("mensaje").orEmpty(),
            vigenciaMinutos = j.optInt("vigencia_minutos", 10),
            codigoDePrueba = j.texto("codigo_de_prueba"),
        )
    }

    suspend fun canjearCodigo(telefono: String, codigo: String, dispositivo: String): SesionIniciada {
        val cuerpo = JSONObject()
            .put("telefono", telefono)
            .put("codigo", codigo)
            .put("dispositivo", dispositivo.take(60))
        val j = llamar("POST", "auth/token", cuerpo)
        return SesionIniciada(
            token = j.getString("token"),
            cliente = clienteDe(j.getJSONObject("cliente")),
        )
    }

    suspend fun salir() {
        llamar("POST", "auth/salir")
    }

    // --- Catálogo y disponibilidad ---------------------------------------

    suspend fun catalogo(): List<Seccion> =
        llamar("GET", "catalogo").optJSONArray("data").lista(::seccionDe)

    suspend fun disponibilidad(servicioId: Int, fecha: LocalDate): List<Horario> {
        val dia = fecha.format(DateTimeFormatter.ISO_LOCAL_DATE)
        return llamar("GET", "disponibilidad?servicio_id=$servicioId&fecha=$dia")
            .optJSONArray("data").lista(::horarioDe)
    }

    // --- Citas -----------------------------------------------------------

    suspend fun misCitas(): List<Cita> =
        llamar("GET", "citas").optJSONArray("data").lista(::citaDe)

    /** El servidor espera la hora local sin zona ni segundos: 2026-11-14T10:00. */
    suspend fun agendar(servicioId: Int, horario: Horario): Cita {
        val inicio = horario.inicio.toLocalDateTime()
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"))
        val cuerpo = JSONObject().put("servicio_id", servicioId).put("inicio", inicio)
        return citaDe(llamar("POST", "citas", cuerpo).getJSONObject("data"))
    }

    suspend fun cancelar(citaId: Int): Cita =
        citaDe(llamar("POST", "citas/$citaId/cancelar").getJSONObject("data"))

    // --- Tarjeta de sellos ------------------------------------------------

    suspend fun sellos(): Sellos =
        sellosDe(llamar("GET", "sellos").getJSONObject("data"))

    // --- Perfil ----------------------------------------------------------

    suspend fun perfil(): Cliente =
        clienteDe(llamar("GET", "perfil").getJSONObject("data"))

    suspend fun actualizarPerfil(nombre: String?, aceptaAvisos: Boolean?): Cliente {
        val cuerpo = JSONObject()
        if (nombre != null) cuerpo.put("nombre", nombre)
        if (aceptaAvisos != null) cuerpo.put("acepta_avisos", aceptaAvisos)
        return clienteDe(llamar("PUT", "perfil", cuerpo).getJSONObject("data"))
    }

    // --- Transporte ------------------------------------------------------

    private suspend fun llamar(metodo: String, ruta: String, cuerpo: JSONObject? = null): JSONObject {
        val resultado = withContext(Dispatchers.IO) { ejecutar(metodo, ruta, cuerpo) }

        if (resultado.codigo == 401 && sesion.token != null) {
            // El token venció o se revocó: se vuelve a la pantalla de acceso.
            withContext(Dispatchers.Main) { sesion.limpiar() }
        }
        if (resultado.codigo !in 200..299) {
            throw ApiException(resultado.codigo, mensajeDeError(resultado.codigo, resultado.json))
        }
        return resultado.json
    }

    private class Respuesta(val codigo: Int, val json: JSONObject)

    private fun ejecutar(metodo: String, ruta: String, cuerpo: JSONObject?): Respuesta {
        val peticion = Request.Builder()
            .url(base + ruta)
            .header("Accept", "application/json")

        sesion.token?.let { peticion.header("Authorization", "Bearer $it") }

        val body = (cuerpo ?: JSONObject()).toString().toRequestBody(TIPO_JSON)
        when (metodo) {
            "GET" -> peticion.get()
            "POST" -> peticion.post(body)
            "PUT" -> peticion.put(body)
            else -> error("Método no soportado: $metodo")
        }

        return try {
            http.newCall(peticion.build()).execute().use { r ->
                val texto = r.body?.string().orEmpty()
                val json = try {
                    if (texto.isBlank()) JSONObject() else JSONObject(texto)
                } catch (e: JSONException) {
                    JSONObject() // p. ej. la página de error de Cloudflare
                }
                Respuesta(r.code, json)
            }
        } catch (e: IOException) {
            throw ApiException(0, "No hay conexión con el servidor. Revisa tu internet.")
        }
    }

    private fun mensajeDeError(codigo: Int, j: JSONObject): String {
        // Nuestros errores usan "mensaje"; los de Laravel, "message" y "errors".
        val propio = j.texto("mensaje")
        if (!propio.isNullOrBlank()) return propio

        val errores = j.optJSONObject("errors")
        if (errores != null) {
            val primero = errores.keys().asSequence().firstOrNull()
            val texto = primero?.let { errores.optJSONArray(it)?.optString(0) }
            if (!texto.isNullOrBlank()) return texto
        }

        return when (codigo) {
            401 -> "Tu sesión terminó. Vuelve a entrar."
            404 -> "No se encontró."
            422 -> "Revisa los datos."
            429 -> "Demasiados intentos. Espera unos minutos."
            502, 503, 504, 530 -> "El servidor no está disponible en este momento."
            else -> j.texto("message") ?: "Ocurrió un error ($codigo)."
        }
    }

    private companion object {
        val TIPO_JSON = "application/json; charset=utf-8".toMediaType()
    }
}
