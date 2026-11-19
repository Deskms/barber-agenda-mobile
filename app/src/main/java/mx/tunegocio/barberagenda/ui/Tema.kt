package mx.tunegocio.barberagenda.ui

import android.app.Activity
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

/**
 * Estilo minimalista inspirado en iOS: fondo gris claro, tarjetas blancas sin
 * sombra, un solo color de acción y títulos grandes.
 * Paleta: negro, café y azul ballena (igual que el panel web).
 */
object Paleta {
    val Negro = Color(0xFF111113)
    val Grafito = Color(0xFF3A3A3C)
    val Cafe = Color(0xFF6F4E37)
    val CafeClaro = Color(0xFFF4EDE6)
    val Ballena = Color(0xFF1E3A5F)
    val BallenaOscuro = Color(0xFF142A46)
    val BallenaClaro = Color(0xFFE8EEF5)
    val Oxido = Color(0xFFA4462F)
    val OxidoClaro = Color(0xFFF8E9E4)
    val Verde = Color(0xFF2F6B45)

    val Fondo = Color(0xFFF2F2F7)
    val Superficie = Color(0xFFFFFFFF)
    val Tinta = Color(0xFF1C1C1E)
    val Gris = Color(0xFF8E8E93)
    val Gris2 = Color(0xFF636366)
    val Linea = Color(0xFFE5E5EA)
    val LineaFuerte = Color(0xFFD1D1D6)
    val Relleno = Color(0xFFE9E9EB)
}

private val Esquema = lightColorScheme(
    primary = Paleta.Ballena,
    onPrimary = Color.White,
    primaryContainer = Paleta.BallenaClaro,
    onPrimaryContainer = Paleta.Ballena,
    inversePrimary = Paleta.BallenaClaro,

    secondary = Paleta.Cafe,
    onSecondary = Color.White,
    // Chips elegidos y otros rellenos suaves: azul ballena muy claro.
    secondaryContainer = Paleta.BallenaClaro,
    onSecondaryContainer = Paleta.Ballena,

    tertiary = Paleta.Negro,
    onTertiary = Color.White,
    tertiaryContainer = Paleta.CafeClaro,
    onTertiaryContainer = Paleta.Cafe,

    background = Paleta.Fondo,
    onBackground = Paleta.Tinta,
    surface = Paleta.Fondo,
    onSurface = Paleta.Tinta,
    surfaceVariant = Paleta.Relleno,
    onSurfaceVariant = Paleta.Gris2,
    surfaceTint = Color.Transparent,
    // Tarjetas y barras: blanco puro, como las listas agrupadas de iOS.
    surfaceBright = Paleta.Superficie,
    surfaceContainerLowest = Paleta.Superficie,
    surfaceContainerLow = Paleta.Superficie,
    surfaceContainer = Paleta.Superficie,
    surfaceContainerHigh = Paleta.Superficie,
    surfaceContainerHighest = Paleta.Superficie,

    outline = Paleta.LineaFuerte,
    outlineVariant = Paleta.Linea,
    error = Paleta.Oxido,
    onError = Color.White,
    errorContainer = Paleta.OxidoClaro,
    onErrorContainer = Paleta.Oxido,
)

// Tipografía del sistema con pesos y espaciado al estilo iOS: títulos gruesos
// y apretados, texto normal un poco más grande.
private val base = Typography()
private val Tipografia = Typography(
    headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.Bold, fontSize = 32.sp, letterSpacing = (-0.5).sp),
    headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.Bold, fontSize = 26.sp, letterSpacing = (-0.4).sp),
    titleLarge = base.titleLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp),
    titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 17.sp),
    titleSmall = base.titleSmall.copy(fontWeight = FontWeight.SemiBold),
    bodyLarge = base.bodyLarge.copy(fontSize = 17.sp, letterSpacing = (-0.2).sp),
    bodyMedium = base.bodyMedium.copy(fontSize = 15.sp, letterSpacing = (-0.1).sp),
    bodySmall = base.bodySmall.copy(fontSize = 13.sp, color = Paleta.Gris2),
    labelLarge = base.labelLarge.copy(fontWeight = FontWeight.SemiBold, fontSize = 15.sp),
    labelSmall = base.labelSmall.copy(fontWeight = FontWeight.Medium),
)

private val Formas = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

/** Encabezado de grupo al estilo iOS: texto pequeño en mayúsculas y gris. */
val EstiloGrupo = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.4.sp, color = Paleta.Gris2)

@Composable
fun BarberTema(contenido: @Composable () -> Unit) {
    val vista = LocalView.current
    if (!vista.isInEditMode) {
        SideEffect {
            // Barra de estado del mismo gris que el fondo, con íconos oscuros.
            val ventana = (vista.context as? Activity)?.window ?: return@SideEffect
            @Suppress("DEPRECATION")
            ventana.statusBarColor = Paleta.Fondo.toArgb()
            @Suppress("DEPRECATION")
            ventana.navigationBarColor = Paleta.Superficie.toArgb()
            WindowCompat.getInsetsController(ventana, vista).apply {
                isAppearanceLightStatusBars = true
                isAppearanceLightNavigationBars = true
            }
        }
    }
    MaterialTheme(colorScheme = Esquema, typography = Tipografia, shapes = Formas, content = contenido)
}
