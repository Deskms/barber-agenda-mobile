package mx.tunegocio.barberagenda.ui

import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val ES_MX: Locale = Locale.forLanguageTag("es-MX")
private val FORMATO_LARGO = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM, HH:mm", ES_MX)
private val FORMATO_DIA = DateTimeFormatter.ofPattern("EEE d MMM", ES_MX)

/** Las fechas ya vienen con el offset del negocio; se muestran tal cual. */
fun fechaLarga(f: OffsetDateTime): String =
    f.format(FORMATO_LARGO).replaceFirstChar { it.titlecase(ES_MX) }

fun diaCorto(d: LocalDate): String =
    if (d == LocalDate.now(ZONA_NEGOCIO)) "Hoy" else d.format(FORMATO_DIA)

fun precioTexto(precio: Double?): String =
    precio?.let { "$" + String.format(ES_MX, "%,.0f", it) } ?: "precio en el local"

fun estadoTexto(estado: String): String = when (estado) {
    "confirmada" -> "Confirmada"
    "cancelada" -> "Cancelada"
    "completada" -> "Completada"
    "no_asistio" -> "No asistió"
    else -> estado
}
