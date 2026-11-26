package mx.tunegocio.barberagenda.ui

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import mx.tunegocio.barberagenda.BuildConfig

/** Resultado de pedirle a Android la cuenta de Google. */
sealed interface CuentaGoogle {
    data class Token(val idToken: String) : CuentaGoogle
    data object Cancelado : CuentaGoogle
    data class Fallo(val mensaje: String) : CuentaGoogle
}

/** El botón solo aparece si la app se compiló con el ID de cliente web de Google. */
val googleDisponible: Boolean get() = BuildConfig.GOOGLE_WEB_CLIENT_ID.isNotBlank()

/**
 * Abre la hoja de Google para elegir cuenta y regresa el ID token.
 *
 * El token va dirigido al "cliente web" (serverClientId): así el servidor
 * puede comprobar con Google que es para esta app. La app nunca ve ni
 * guarda la contraseña de Google.
 *
 * contexto debe ser la Activity (LocalContext en Compose).
 */
suspend fun pedirCuentaGoogle(contexto: Context): CuentaGoogle {
    val opcion = GetSignInWithGoogleOption.Builder(BuildConfig.GOOGLE_WEB_CLIENT_ID).build()
    val solicitud = GetCredentialRequest.Builder().addCredentialOption(opcion).build()

    return try {
        val credencial = CredentialManager.create(contexto).getCredential(contexto, solicitud).credential
        if (credencial is CustomCredential && credencial.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            CuentaGoogle.Token(GoogleIdTokenCredential.createFrom(credencial.data).idToken)
        } else {
            CuentaGoogle.Fallo("Google respondió algo inesperado. Entra con tu número.")
        }
    } catch (e: GetCredentialCancellationException) {
        CuentaGoogle.Cancelado
    } catch (e: NoCredentialException) {
        CuentaGoogle.Fallo("No hay una cuenta de Google en este teléfono. Agrégala en Ajustes o entra con tu número.")
    } catch (e: GetCredentialException) {
        CuentaGoogle.Fallo("No se pudo abrir Google (${e.type.substringAfterLast('.')}). Entra con tu número.")
    }
}
