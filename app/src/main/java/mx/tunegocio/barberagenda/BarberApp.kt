package mx.tunegocio.barberagenda

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import mx.tunegocio.barberagenda.data.ApiClient
import mx.tunegocio.barberagenda.data.SesionStore

/** Dependencias compartidas de toda la app. */
class BarberApp : Application() {

    lateinit var sesion: SesionStore
        private set

    lateinit var api: ApiClient
        private set

    /** Solo en memoria: al cerrar la app o mandarla al fondo se vuelve a bloquear. */
    var desbloqueada by mutableStateOf(false)

    override fun onCreate() {
        super.onCreate()
        sesion = SesionStore(this)
        api = ApiClient(BuildConfig.API_BASE, sesion)
    }
}
