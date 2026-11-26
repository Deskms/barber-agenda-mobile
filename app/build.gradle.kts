plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// La URL de la API vive solo en gradle.properties (BARBER_API_URL).
val apiBase: String = (project.findProperty("BARBER_API_URL") as String?)
    ?: "https://citas.tunegociomx.app/api/movil/"

// "ID de cliente web" de Google Cloud para "Continuar con Google". Va en
// gradle.properties (GOOGLE_WEB_CLIENT_ID). Vacío = el botón no aparece.
val googleWebClientId: String = (project.findProperty("GOOGLE_WEB_CLIENT_ID") as String?).orEmpty()

android {
    namespace = "mx.tunegocio.barberagenda"
    compileSdk = 35

    defaultConfig {
        applicationId = "mx.tunegocio.barberagenda"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
        buildConfigField("String", "API_BASE", "\"$apiBase\"")
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"$googleWebClientId\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.core)
    implementation(libs.androidx.security.crypto)
    implementation(libs.okhttp)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // Sensor 1: cámara para leer el QR de llegada.
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.mlkit.barcode.scanning)

    // Sensor 2: huella o rostro para abrir la app.
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.fragment.ktx)

    // Iniciar sesión con Google (Credential Manager).
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services)
    implementation(libs.googleid)
    debugImplementation(libs.androidx.ui.tooling)
}
