# Volumen Maestro — Especificación de diseño

Estado: COMPLETO 2026-10-31 (verificaciones con el control pendientes en docs/TODO.md)

Fecha: 31/10/2026
Dispositivo objetivo: TCL "Smart TV Pro" (G09, Amlogic, Android TV 12 / Google TV, 2 GB RAM), salida óptica a receptor Sony STR-KG700.

## 1. Problema y objetivo

El receptor Sony está conectado por fibra óptica. TCL fija el volumen de la salida óptica: los botones de volumen del control no cambian nada y muestran el cartel *"Audio output through optical fibers does not support volume adjustment"*. El Sony no tiene red ni HDMI ARC.

Ya está probado que el **volumen maestro de AudioFlinger** se aplica a todo el audio PCM que sale por la fibra (mezclador y camino directo/túnel de Netflix), pero **no** al audio Dolby (offload E-AC3 o passthrough AC3). El usuario aceptó perder Dolby a cambio de manejar el volumen con el control de la TV.

**Objetivo:** una app Android TV autónoma que, con un interruptor, convierta los botones de volumen del control de la TV en un volumen real por software para todas las apps, con carteles animados con temas, y que además ofrezca optimización básica de la TV. Todo manejable desde la TV, sin PC.

## 2. Alcance

Entra:

- Interruptor general ENCENDIDO / APAGADO del ecosistema de audio.
- Curva de volumen configurable y transiciones suaves.
- 11 temas para los dos carteles de volumen, con vista previa (los 10 de los mockups más "Sable láser rojo").
- Opciones del panel (duración, cartel lateral).
- Estado con chequeos y "Reparar todo".
- Optimizar TV y reinicio programado con días y hora.
- Atajo de un botón del control para prender/apagar la app.
- Copia de seguridad automática de ajustes, registro de eventos y desinstalación limpia.

No entra (decidido):

- Volumen con Dolby (no hay forma sin hardware adicional; el usuario no quiere un emisor IR).
- Tope de volumen, volumen fijo al prender, volumen por app, modo noche por horario.
- Gestión de apps de fábrica.
- Distribución por Play Store o auto-actualización.

## 3. Comportamiento de los modos

**ENCENDIDO** (estado por defecto):

| Ajuste | Valor |
|---|---|
| `global encoded_surround_output` | `1` (nunca surround: las apps mandan PCM) |
| `global nrdp_external_surround_sound_enabled` | `0` |
| `global nrdp_audio_platform_capabilities` | original con `ddplus.enabled=false` y `atmos.enabled=false` |
| `system tcl_sys_audio_spdif_outmode` (Digital Audio Out) | `1` (PCM) |
| Volumen maestro | según la curva y el índice de volumen de la TV |
| Carteles de volumen | visibles con el tema elegido, tapando los de TCL |

**APAGADO** ("como venía la TV"):

| Ajuste | Valor |
|---|---|
| `global encoded_surround_output` | borrado (Auto) |
| `global nrdp_external_surround_sound_enabled` | `1` |
| `global nrdp_audio_platform_capabilities` | el original guardado (Dolby activado) |
| `system tcl_sys_audio_spdif_outmode` | `3` (Auto: Dolby pasa al equipo) |
| Volumen maestro | `1.0` |
| Carteles | ninguno (se ve el de TCL) |

Reglas:

- Al cambiar de modo se cierra Netflix (`am force-stop com.netflix.ninja`) para que tome la configuración; la UI lo avisa.
- El valor original de `nrdp_audio_platform_capabilities` se guarda la primera vez que se lee con Dolby activado; si no existe se usa el valor de fábrica conocido.
- **Al prender la TV siempre queda ENCENDIDO** (arranque completo, reinicio programado o salida de standby), aunque se haya apagado antes. Mientras la TV está prendida, APAGADO persiste hasta el próximo encendido.
- La salida de audio (`global tcl_device_out_type`) debe ser `32` (Optical) en ambos modos; la app no la cambia por su cuenta salvo desde "Arreglar" y avisa si no lo está.

## 4. Arquitectura

Proyecto Gradle, Kotlin, `minSdk 28`, `targetSdk 34`, Jetpack Compose para TV (`androidx.tv:tv-material`) para la interfaz. Paquete `local.volumenmaestro` y **el mismo `keystore.jks`** de la versión actual: así la nueva se instala como actualización, conserva sus datos (incluida la clave ADB ya autorizada en la TV) y el servicio de accesibilidad sigue habilitado. Con otra firma habría que desinstalar, volver a aceptar el permiso de depuración y reactivar el servicio.

Dos procesos:

1. **Motor** (proceso `:motor`, sin Compose). Contiene:
   - `VolumeService` (AccessibilityService): arranca con la TV, escucha `VOLUME_CHANGED_ACTION` / `STREAM_MUTE_CHANGED_ACTION` de `STREAM_MUSIC`, filtra teclas para el atajo, dibuja los carteles y mantiene el watchdog.
   - `EngineService`: servicio enlazable (Messenger/AIDL) que la UI usa para mandar comandos y recibir estado.
   - Receptores de alarma (reinicio programado) y de arranque.
2. **Interfaz** (proceso principal): la Activity Compose con 5 pestañas. No guarda estado propio: lee y escribe a través de `EngineService`.

Bloques de código (cada uno con una responsabilidad):

| Bloque | Contenido |
|---|---|
| `adb/` | `AdbClient`, `AdbKeys` (ya existen y funcionan) |
| `audio/` | `MasterVolume` (set/get por `service call media.audio_flinger 7/9`), `VolumeCurve`, `Ramp` (transiciones), `AudioSetup` (modos, ya existe) |
| `overlay/` | `OverlayController` (las dos ventanas `TYPE_ACCESSIBILITY_OVERLAY`), interfaz `Theme` y los 10 temas |
| `system/` | `Optimizer`, `RebootScheduler`, `HealthChecks` |
| `settings/` | `Config` (modelo), `ConfigStore` (archivo JSON en el proceso motor), `Backup` |
| `ui/` | una pantalla Compose por pestaña y componentes compartidos |

Todos los comandos privilegiados pasan por la conexión ADB local (127.0.0.1:5555) con la clave propia de la app, ya autorizada.

## 5. Volumen

**Entrada:** índice de `STREAM_MUSIC` en el dispositivo óptico (0–100). Lo cambian los botones del control (TCL decide el paso por toque) y Alexa. La app no modifica el índice.

**Curva** (`VolumeCurve`), salida = ganancia lineal 0..1 (100 → 1.0 siempre; nunca se amplifica):

- *Pareja en dB*: `dB = (índice − 100) × dBPorPunto`, `dBPorPunto` configurable de 0,2 a 1,0 en pasos de 0,1 (por defecto 0,5).
- *Suave*: `ganancia = (índice/100)²` (comportamiento actual).
- *Tipo amplificador*: curva en S en dB: fina entre 30 y 80, más gruesa en los extremos; rango total fijo de −60 dB.
- *Volumen 0*: opción "silencio total" (por defecto) o "mínimo de la curva".
- Silencio del sistema (`isStreamMute`) → ganancia 0.

**Transiciones** (`Ramp`): Apagado / Rápida (150 ms) / Suave (400 ms). La rampa interpola en dB en pasos de ~25 ms; si llega un cambio nuevo a mitad de rampa, continúa desde el valor actual. Salir de silencio usa siempre un fundido de 150 ms.

**Watchdog:** cada 10 s compara el volumen maestro real con el esperado y lo reaplica si difiere; también al encender la pantalla.

**Prueba en la pestaña Volumen:** mientras la pantalla está abierta, los cambios de curva se aplican al instante (vista previa), y el gráfico muestra la curva y un texto tipo "en 50 vas a escuchar −25 dB".

## 6. Carteles y temas

Dos ventanas fijas, opacas, que tapan los carteles de TCL (coordenadas en 1920×1080):

- Panel inferior: x 480–1440, y 882–1032 (960×150). Tapa el toast de TCL en x 524–1396 / y 901–1016.
- Panel lateral: x 1740–1848, y 326–756 (108×430). Tapa la barra vertical de TCL.

Interfaz `Theme`:

```kotlin
interface Theme {
    val id: String
    val name: String
    fun drawBottom(canvas: Canvas, w: Int, h: Int, s: OverlayState)
    fun drawSide(canvas: Canvas, w: Int, h: Int, s: OverlayState)
}
data class OverlayState(
    val level: Float,        // 0..1 animado
    val index: Int,          // 0..100 para mostrar
    val muted: Boolean,
    val timeSec: Float,      // reloj de animación
    val appear: Float,       // 0..1 entrada/salida
    val message: String?     // p. ej. "Volumen Maestro: APAGADO", "Sin conexión"
)
```

- Los temas salen de los mockups aprobados (`mockups-volumen.html`): los 10 originales más el 11, "Sable láser rojo" (hoja roja que se enciende desde la empuñadura, largo = volumen, titileo, chispas al cambiar y retracción al silenciar). Diseño genérico, sin nombres ni logos de franquicias.
- La animación de entrada nunca deja ver el cartel de TCL: el rectángulo de TCL queda cubierto en todo momento (escala mínima 0,96 o sin escala; nada de fundidos desde transparente).
- Las vistas previas de la UI usan las mismas clases `Theme` dibujando en un Canvas de Compose.
- Opciones: duración Normal 3,8 s / Larga 5 s / Muy larga 7 s (mínimo 3,8 s porque el toast de TCL dura 3,5 s); cartel lateral sí/no.
- La animación corre solo mientras el cartel está visible (`postInvalidateOnAnimation`), para no gastar CPU con los carteles escondidos.

## 7. Atajo del control

- Modo "Elegir botón": la app escucha teclas por el filtro de accesibilidad y muestra la que llega. Si el sistema no la entrega (Home, Encendido, Asistente, volumen), avisa que no se puede usar.
- Candidatos indicados por el usuario: **CH** y **Info**.
- Modos de activación: *toque simple* (el botón queda dedicado) o *mantener 2 s* (el toque normal sigue funcionando; la app reinyecta la pulsación corta con `input keyevent` por ADB).
- Al usarlo se muestra un mensaje con el tema activo y se cambia de modo (con el cierre de Netflix del punto 3).

## 8. Optimizar y reinicio programado

**Tarjetas:** memoria libre (`/proc/meminfo` MemAvailable), espacio libre (`/data`), tiempo sin reiniciar (`uptime`; amarillo si > 7 días).

**Optimizar ahora:**
1. `pm trim-caches 999G`.
2. `am kill-all` (solo procesos en segundo plano).
3. Mostrar MB liberados de espacio y memoria.

"Reiniciar ahora" con confirmación (`svc power reboot` por ADB).

**Reinicio programado:**
- Interruptor, atajos (todos los días / 1 / 2 veces por semana / personalizado), casillas L–D, hora y minutos. Texto "Próximo reinicio: …".
- `AlarmManager.setExactAndAllowWhileIdle` (el permiso de alarmas exactas se concede con `appops` por ADB).
- A la hora: si la pantalla está prendida, espera a que se apague hasta 3 h; si no se apaga, pasa al próximo día programado.
- Antes de reiniciar se guarda la marca "reinicio programado". Al arrancar, si existe la marca y la pantalla se prendió sola, se manda `KEYCODE_SLEEP`.
- Requiere modo de reposo "Network Standby"; si no lo está, Inicio avisa.

## 9. Interfaz (Compose para TV)

Barra de pestañas arriba: **Inicio · Volumen · Temas · Optimizar · Ajustes**. Fondo oscuro, tarjetas con foco animado, navegación completa con el control.

- **Inicio:** estado grande con interruptor, volumen actual, tema; semáforo de chequeos con "Arreglar" individual y "Reparar todo".
- **Volumen:** tipo de curva, dB por punto, volumen 0, transición, gráfico en vivo.
- **Temas:** grilla de 11 con mini vistas previas animadas, TV de muestra grande del tema enfocado, "Probar en pantalla", duración, cartel lateral.
- **Optimizar:** tarjetas, "Optimizar ahora", "Reiniciar ahora", reinicio programado.
- **Ajustes:** atajo del control, copia de seguridad (exportar/importar), registro, desinstalar limpio, acerca de.

Chequeos del semáforo:

| Chequeo | Cómo se mide | Arreglo |
|---|---|---|
| Conexión interna | `AdbClient` conectado | reintentar; instrucciones para aceptar el permiso |
| Depuración USB | `global adb_enabled = 1` | solo aviso |
| Salida Optical | `global tcl_device_out_type = 32` | poner `32`; si no aplica, abrir el menú de audio |
| Salida digital | `system tcl_sys_audio_spdif_outmode` según el modo | escribir el valor |
| Netflix sin Dolby (encendido) | `ddplus.enabled=false` | reaplicar el modo |
| Reposo Network Standby | `system tcl_sys_network_standby_enabled = 1` | solo aviso con la ruta del menú |
| Volumen maestro responde | `service call media.audio_flinger 9` devuelve un float | si falla: "incompatible" en rojo |

## 10. Ajustes, copia y desinstalación

- `Config` se guarda como JSON en el proceso motor. Cada cambio se copia a `Download/VolumenMaestro/ajustes.json` vía MediaStore.
- Primer arranque sin ajustes y con copia encontrada → "Encontré tus ajustes, ¿los restauro?".
- Registro: últimos 100 eventos en memoria y archivo rotativo.
- Desinstalar limpio: confirmación → modo APAGADO → cancelar alarmas → quitar el servicio de `enabled_accessibility_services` → abrir `ACTION_DELETE` de la app.

## 11. Errores

- Conexión ADB caída: reintento con espera creciente (1, 2, 5, 10 s…); el panel muestra "Sin conexión" e Inicio queda en rojo.
- Comandos que fallan: se registran; el modo queda marcado "incompleto" y se ofrece "Reparar todo".
- Códigos de AudioFlinger incompatibles (actualización de firmware): detectado al iniciar → estado "incompatible", la app no intenta aplicar volumen.

## 12. Pruebas

En la PC (JUnit):
- `VolumeCurve`: valores exactos por tipo (por ejemplo 0,5 dB/punto → índice 50 = −25 dB) y extremos 0/100.
- `RebootScheduler`: cálculo del próximo reinicio (cambio de día y de semana, días vacíos, hora ya pasada hoy).
- Serialización de `Config` y compatibilidad con copias viejas.

En la TV (lista de verificación):
- Encender/apagar desde la app y desde el atajo; revisar cada ajuste de la tabla del punto 3.
- Volumen en YouTube, Netflix (túnel PCM) y Plex en modo ENCENDIDO.
- Cada tema: vista previa igual al cartel real; el cartel de TCL nunca se asoma.
- Reinicio programado de prueba a 2 minutos con la pantalla apagada y con la pantalla prendida.
- Exportar, borrar datos, importar; desinstalar limpio y comprobar el estado APAGADO.
- Prender la TV con la app en APAGADO y comprobar que vuelve a ENCENDIDO.

## 13. Riesgos conocidos

- Algunas apps (por ejemplo Plex con pistas AC3/DTS) podrían seguir usando offload aun con `encoded_surround_output=1`; se verifica en la TV y, si pasa, se documenta qué ajuste de la app de Plex usar (decodificar en el dispositivo).
- Una actualización de firmware de TCL puede cambiar los códigos de AudioFlinger o el comportamiento de los ajustes TCL; lo cubre el chequeo "volumen maestro responde".
- Compose aumenta el tamaño del APK (6–8 MB) y la memoria mientras la app está abierta; el motor queda aislado en su proceso.
