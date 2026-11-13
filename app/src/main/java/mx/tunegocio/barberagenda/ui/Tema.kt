package mx.tunegocio.barberagenda.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Tema provisional. El diseño final lo define el equipo.
private val Esquema = lightColorScheme(
    primary = Color(0xFF1F4E79),
    primaryContainer = Color(0xFFDCE6F1),
    secondary = Color(0xFF5A6472),
)

@Composable
fun BarberTema(contenido: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Esquema, content = contenido)
}
