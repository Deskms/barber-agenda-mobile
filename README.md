# Barber Agenda: app Android para clientas

App nativa (Kotlin + Jetpack Compose) para que las clientas de Joss Barba Salón
entren con su WhatsApp, agenden, vean y cancelen sus citas y controlen los
avisos de espacios libres.

La app **no tiene base de datos ni reglas propias**. Todo lo decide la API de
[barber-agenda-web](https://github.com/deskms/barber-agenda-web): disponibilidad,
choques de horario y quién puede cancelar. El contrato está en
`docs/API_MOVIL.md` de ese repositorio.

Proyecto Cuatrimestral Integral · IDGS · UTJ · 2026C · 9.° B.

## Qué hace

| Pantalla | Endpoint |
|---|---|
| Entrar con código de WhatsApp o con Google | `POST auth/codigo`, `POST auth/token`, `POST auth/google` |
| Agendar: servicio → día → hora | `GET catalogo`, `GET disponibilidad`, `POST citas` |
| Mis citas y cancelar | `GET citas`, `POST citas/{id}/cancelar` |
| Tarjeta de sellos | `GET sellos` |
| Perfil: nombre y avisos | `GET perfil`, `PUT perfil`, `POST auth/salir` |

El token se guarda cifrado (EncryptedSharedPreferences). Si el servidor
responde 401, la app regresa sola a la pantalla de acceso. Si al agendar el
horario ya se ocupó (409), muestra el aviso y vuelve a pedir los horarios.

Diseño visual (tema oscuro de Joss Barber): José Francisco Quezada Palomo.

## Abrir y correr

1. Android Studio → **Open** → esta carpeta. Espera a que termine el *Gradle sync*.
2. Si Studio ofrece actualizar el Android Gradle Plugin, puedes aceptarlo o no; el proyecto usa AGP 8.7.3, Kotlin 2.0.21 y Gradle 8.11.1.
3. Elige un emulador o tu teléfono con *depuración USB* y presiona **Run**.

Para instalar en un teléfono sin cable: **Build → Build App Bundle(s) / APK(s) → Build APK(s)**.
El APK queda en `app/build/outputs/apk/debug/app-debug.apk`.

## Servidor

La dirección de la API vive en un solo lugar, `gradle.properties`:

```
BARBER_API_URL=https://citas.tunegociomx.app/api/movil/
```

Para el emulador contra Laravel en la misma laptop, sin túnel:

```
BARBER_API_URL=http://10.0.2.2:8010/api/movil/
```

Después de cambiarla, haz *Sync* y vuelve a correr.

El servidor de pruebas solo responde cuando la laptop está encendida con
`artisan serve --port=8010` y el túnel de Cloudflare activos.

## Datos de prueba

Con el servidor en `APP_ENV=local`, la respuesta de `auth/codigo` trae el código
y la app lo llena sola. Usa los números ficticios `5550000001`, `5550000002` o
`5550000003` (vienen de `PruebasMovilSeeder`). No uses números de clientas reales.

## Estructura

```
app/src/main/java/mx/tunegocio/barberagenda/
  BarberApp.kt          dependencias compartidas (API y sesión)
  MainActivity.kt       decide entre acceso y pantallas principales
  data/ApiClient.kt     llamadas a /api/movil y manejo de errores
  data/Modelos.kt       modelos y lectura de JSON
  data/SesionStore.kt   token cifrado
  ui/ViewModels.kt      estado de cada pantalla
  ui/Pantallas.kt       pantallas en Compose
  ui/Formato.kt         fechas, precios y estados en español
  ui/Tema.kt            tema provisional
```

## Antes estaba

El andamiaje inicial con Expo/React Native quedó en `legacy-expo/`, sin uso.
Se reemplazó por el proyecto nativo de Android Studio (SCRUM-54).

## Equipo

| Integrante | Participación |
|---|---|
| Adrián Dominic Hernández Esqueda | Líder del proyecto. Desarrollo de la app y de la API que consume, pruebas en teléfono y demostración. |
| José Francisco Quezada Palomo | Co-desarrollo junto con Dominic y diseño visual de la app. |
| José Manuel Castellanos Castro | Versión inicial (V1) del QR y del backend móvil. Resultó insuficiente y se tomó como guía para la versión final. |
| Rodrigo Salas Martínez | No participó en el desarrollo. |
