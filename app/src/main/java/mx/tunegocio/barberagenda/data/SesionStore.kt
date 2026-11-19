package mx.tunegocio.barberagenda.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Guarda el token de Sanctum cifrado en el teléfono.
 * `token` es estado de Compose: cuando se borra, la app regresa sola a la
 * pantalla de acceso.
 */
class SesionStore(context: Context) {

    private val prefs: SharedPreferences = abrir(context)

    var token: String? by mutableStateOf(prefs.getString(LLAVE_TOKEN, null))
        private set

    var telefono: String? by mutableStateOf(prefs.getString(LLAVE_TELEFONO, null))
        private set

    /** La clienta pidió que la app se abra con huella o rostro. */
    var pedirHuella: Boolean by mutableStateOf(prefs.getBoolean(LLAVE_HUELLA, false))
        private set

    fun cambiarHuella(activa: Boolean) {
        prefs.edit().putBoolean(LLAVE_HUELLA, activa).apply()
        pedirHuella = activa
    }

    fun guardar(sesion: SesionIniciada) {
        prefs.edit()
            .putString(LLAVE_TOKEN, sesion.token)
            .putString(LLAVE_TELEFONO, sesion.cliente.telefono)
            .apply()
        telefono = sesion.cliente.telefono
        token = sesion.token
    }

    fun limpiar() {
        prefs.edit().clear().apply()
        pedirHuella = false
        token = null
    }

    private companion object {
        const val LLAVE_TOKEN = "token"
        const val LLAVE_TELEFONO = "telefono"
        const val LLAVE_HUELLA = "pedir_huella"

        fun abrir(context: Context): SharedPreferences = try {
            val llave = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context,
                "sesion_segura",
                llave,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        } catch (e: Exception) {
            // Algunos emuladores fallan con el Keystore; no se bloquea la app.
            context.getSharedPreferences("sesion", Context.MODE_PRIVATE)
        }
    }
}
