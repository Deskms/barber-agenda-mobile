package mx.tunegocio.barberagenda

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import mx.tunegocio.barberagenda.ui.BarberTema
import mx.tunegocio.barberagenda.ui.PantallaAcceso
import mx.tunegocio.barberagenda.ui.PantallaPrincipal

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as BarberApp

        setContent {
            BarberTema {
                Surface(Modifier.fillMaxSize()) {
                    // Sin token se pide el código; al borrarse (salir o 401) se regresa aquí solo.
                    if (app.sesion.token == null) {
                        PantallaAcceso(app)
                    } else {
                        PantallaPrincipal(app)
                    }
                }
            }
        }
    }
}
