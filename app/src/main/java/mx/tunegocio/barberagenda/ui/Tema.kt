package mx.tunegocio.barberagenda.ui

import android.app.Activity
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

/** Grafito, plata y azul acero de la identidad de Joss Barber. */
object Paleta {
    val Fondo = Color(0xFF141416)
    val Barra = Color(0xFF1C2024)
    val Superficie = Color(0xFF23272D)
    val Elevada = Color(0xFF2B3037)
    val Plata = Color(0xFFE7E9ED)
    val TextoSecundario = Color(0xFFAFB6C1)
    val Acero = Color(0xFF8FA8BE)
    val AceroClaro = Color(0xFFBCD2E2)
    val AceroFondo = Color(0xFF283B49)
    val SobreAcero = Color(0xFF101820)
    val Linea = Color(0xFF39414A)
    val LineaFuerte = Color(0xFF748392)
    val Error = Color(0xFFFFB4AA)
    val ErrorFondo = Color(0xFF472A2A)
    val Verde = Color(0xFFA4D6B5)
}

private val Esquema = darkColorScheme(
    primary = Paleta.Acero,
    onPrimary = Paleta.SobreAcero,
    primaryContainer = Paleta.AceroFondo,
    onPrimaryContainer = Paleta.AceroClaro,
    inversePrimary = Paleta.AceroFondo,
    secondary = Paleta.AceroClaro,
    onSecondary = Paleta.SobreAcero,
    secondaryContainer = Paleta.AceroFondo,
    onSecondaryContainer = Paleta.AceroClaro,
    tertiary = Paleta.Plata,
    onTertiary = Paleta.SobreAcero,
    tertiaryContainer = Paleta.Elevada,
    onTertiaryContainer = Paleta.Plata,
    background = Paleta.Fondo,
    onBackground = Paleta.Plata,
    surface = Paleta.Fondo,
    onSurface = Paleta.Plata,
    surfaceVariant = Paleta.Elevada,
    onSurfaceVariant = Paleta.TextoSecundario,
    inverseSurface = Paleta.Plata,
    inverseOnSurface = Paleta.Fondo,
    surfaceTint = Color.Transparent,
    surfaceDim = Paleta.Fondo,
    surfaceBright = Paleta.Elevada,
    surfaceContainerLowest = Paleta.Fondo,
    surfaceContainerLow = Paleta.Barra,
    surfaceContainer = Paleta.Superficie,
    surfaceContainerHigh = Paleta.Elevada,
    surfaceContainerHighest = Color(0xFF333B44),
    outline = Paleta.LineaFuerte,
    outlineVariant = Paleta.Linea,
    error = Paleta.Error,
    onError = Paleta.ErrorFondo,
    errorContainer = Paleta.ErrorFondo,
    onErrorContainer = Paleta.Error,
)

private val base = Typography()
private val Tipografia = Typography(
    headlineMedium = base.headlineMedium.copy(fontFamily = FontFamily.Serif, fontSize = 32.sp, lineHeight = 38.sp, letterSpacing = (-0.6).sp),
    headlineSmall = base.headlineSmall.copy(fontFamily = FontFamily.Serif, fontSize = 27.sp, lineHeight = 33.sp),
    titleLarge = base.titleLarge.copy(fontFamily = FontFamily.Serif, fontSize = 23.sp, lineHeight = 29.sp),
    titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 17.sp),
    titleSmall = base.titleSmall.copy(fontWeight = FontWeight.SemiBold),
    bodyLarge = base.bodyLarge.copy(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = base.bodyMedium.copy(fontSize = 15.sp, lineHeight = 22.sp),
    bodySmall = base.bodySmall.copy(fontSize = 13.sp, lineHeight = 19.sp, color = Paleta.TextoSecundario),
    labelLarge = base.labelLarge.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
    labelSmall = base.labelSmall.copy(fontSize = 12.sp, fontWeight = FontWeight.Medium),
)

private val Formas = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(20.dp),
)

val EstiloGrupo = TextStyle(
    fontSize = 13.sp, lineHeight = 19.sp, fontWeight = FontWeight.SemiBold,
    letterSpacing = 1.sp, color = Paleta.AceroClaro,
)

@Composable
fun BarberTema(contenido: @Composable () -> Unit) {
    val vista = LocalView.current
    if (!vista.isInEditMode) {
        SideEffect {
            val ventana = (vista.context as? Activity)?.window ?: return@SideEffect
            @Suppress("DEPRECATION")
            ventana.statusBarColor = Paleta.Fondo.toArgb()
            @Suppress("DEPRECATION")
            ventana.navigationBarColor = Paleta.Barra.toArgb()
            WindowCompat.getInsetsController(ventana, vista).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }
    MaterialTheme(colorScheme = Esquema, typography = Tipografia, shapes = Formas, content = contenido)
}
