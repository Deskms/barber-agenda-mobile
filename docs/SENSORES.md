# Sensores físicos usados por la app Barber Agenda

Dispositivo hardware: el propio teléfono Android de la clienta. No se usa
hardware externo (ESP32, Arduino ni sensores conectados).

Todos los sensores se consultan con `SensorManager` y se registran solo
mientras la pantalla que los usa está en primer plano (`ON_RESUME`) y se
liberan en `ON_PAUSE`, para no gastar batería. Ninguna lectura se guarda ni se
envía al servidor.

## Sensor 1: acelerómetro

| Dato | Valor |
|---|---|
| Nombre técnico | Acelerómetro de 3 ejes (el nombre exacto del chip aparece en Perfil → Sensores del teléfono) |
| Tipo Android | `Sensor.TYPE_ACCELEROMETER` (sensor físico, no virtual) |
| Lecturas | `values[0..2]` = aceleración en x, y, z en m/s², incluida la gravedad |
| Cálculo | fuerza = √(x² + y² + z²) / 9.81. En reposo vale ≈ 1 g |
| Función | Si la fuerza supera **2.5 g**, la app abre la pestaña **Tarjeta** (sellos y premios) |
| Activaciones accidentales | Umbral alto (caminar o guardar el teléfono no llega a 2.5 g) y pausa de 1.5 s entre agitadas |
| Alternativa sin sensor | La pestaña **Tarjeta** de la barra inferior |
| Código | `ui/SensoresMovimiento.kt` → `DetectorDeAgitar`; se usa en `PantallaPrincipal` |

## Sensor 2: magnetómetro

| Dato | Valor |
|---|---|
| Nombre técnico | Magnetómetro de 3 ejes (brújula digital) |
| Tipo Android | `Sensor.TYPE_MAGNETIC_FIELD` (sensor físico) |
| Lecturas | `values[0..2]` = campo magnético en x, y, z en µT |
| Cálculo | `SensorManager.getRotationMatrix()` con el campo magnético y la gravedad del acelerómetro → `getOrientation()` → azimut (ángulo respecto al norte magnético). Se suaviza con un filtro paso bajo (0.15) |
| Corrección | `GeomagneticField(...).declination` pasa del norte magnético al norte real |
| Función | Pantalla **Cómo llegar (brújula)**: una flecha apunta a la barbería y muestra la distancia |
| Calibración | Si `onAccuracyChanged` reporta precisión baja, la app pide mover el teléfono en forma de 8 |
| Alternativa | Botón **Abrir en el mapa** (Google Maps con las coordenadas del local) |
| Código | `ui/SensoresMovimiento.kt` → `PantallaBrujula` y `Brujula` |

Aclaración: la brújula combina el magnetómetro con el acelerómetro (para saber
cómo está inclinado el teléfono). Se cuentan como **dos sensores distintos**
porque cada uno tiene su propia función: el acelerómetro solo detecta agitar,
y el magnetómetro es el que da la orientación.

## Tecnologías complementarias del teléfono (no cuentan como los dos sensores)

| Tecnología | Uso | Permiso |
|---|---|---|
| GPS (`LocationManager`, GPS y red) | Ubicación de la clienta para calcular rumbo y distancia | `ACCESS_FINE_LOCATION`, solo en la brújula |
| Cámara (CameraX + ML Kit, en el teléfono) | Leer el QR del mostrador para registrar la llegada | `CAMERA`, solo al escanear |
| Huella o rostro (`BiometricPrompt`) | Abrir la app; la huella la valida Android y la app nunca la recibe | `USE_BIOMETRIC` |

## Cómo comprobar que son lecturas reales

En la app: **Perfil → Sensores del teléfono**. La pantalla:

1. Lista todos los sensores que reporta el teléfono con `getSensorList(Sensor.TYPE_ALL)`.
2. Muestra en vivo los valores x, y, z del acelerómetro y del magnetómetro.
3. Si alguno falta, lo indica en lugar de inventar lecturas.

## Pruebas en teléfono físico

Llenar con los resultados reales. Para la evidencia, toma una captura de cada prueba.

| # | Prueba | Resultado esperado | Teléfono (marca/modelo) | Resultado obtenido |
|---|---|---|---|---|
| S1 | Abrir Perfil → Sensores del teléfono | Aparecen acelerómetro y magnetómetro con lecturas que cambian al mover el teléfono | | Pendiente |
| S2 | Teléfono en reposo sobre la mesa | Acelerómetro ≈ 0, 0, 9.8 m/s²; no se abre la tarjeta | | Pendiente |
| S3 | Caminar con el teléfono en la mano | No se abre la tarjeta | | Pendiente |
| S4 | Agitar con fuerza en Agendar | Se abre la pestaña Tarjeta | | Pendiente |
| S5 | Brújula con permiso de ubicación | La flecha gira al girar el teléfono y apunta al local; la distancia coincide con Maps | | Pendiente |
| S6 | Brújula cerca de un imán o metal | Aparece el aviso de calibrar | | Pendiente |
| S7 | Negar el permiso de ubicación | Aparece la explicación y el botón para dar permiso; la app no se cierra | | Pendiente |
| S8 | Mandar la app al fondo en la brújula | Los sensores dejan de escucharse (el GPS se apaga) | | Pendiente |
