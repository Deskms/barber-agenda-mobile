package mx.tunegocio.barberagenda

import android.app.Application
import mx.tunegocio.barberagenda.data.ApiClient
import mx.tunegocio.barberagenda.data.SesionStore

/** Dependencias compartidas de toda la app. */
class BarberApp : Application() {

    lateinit var sesion: SesionStore
        private set

    lateinit var api: ApiClient
        private set

    override fun onCreate() {
        super.onCreate()
        sesion = SesionStore(this)
        api = ApiClient(BuildConfig.API_BASE, sesion)
    }
}
