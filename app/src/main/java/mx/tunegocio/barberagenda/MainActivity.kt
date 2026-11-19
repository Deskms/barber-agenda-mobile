package mx.tunegocio.barberagenda

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import mx.tunegocio.barberagenda.ui.BarberTema
import mx.tunegocio.barberagenda.ui.PantallaAcceso
import mx.tunegocio.barberagenda.ui.PantallaBloqueo
import mx.tunegocio.barberagenda.ui.PantallaPrincipal

// FragmentActivity (y no ComponentActivity) porque BiometricPrompt lo necesita.
class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as BarberApp

        setContent {
            BarberTema {
                Surface(Modifier.fillMaxSize()) {
                    // Sin token se pide el código; al borrarse (salir o 401) se regresa aquí solo.
                    when {
                        app.sesion.token == null -> PantallaAcceso(app)
                        app.sesion.pedirHuella && !app.desbloqueada -> PantallaBloqueo(
                            alDesbloquear = { app.desbloqueada = true },
                            alSalir = { app.sesion.limpiar() },
                        )
                        else -> PantallaPrincipal(app)
                    }
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        // Al salir de la app se vuelve a pedir la huella; al girar la pantalla, no.
        if (!isChangingConfigurations) {
            (application as BarberApp).desbloqueada = false
        }
    }
}
