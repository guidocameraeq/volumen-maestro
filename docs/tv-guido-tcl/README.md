# La TV de Guido (TCL Google TV) — historia completa

Todo lo que se le hizo a la TV, cómo y por qué, para poder retomarla cualquier día. Lo que es de la app Volumen Maestro está en el resto de `docs/`; acá está la TV.

## La TV

| Dato | Valor |
|---|---|
| Modelo | TCL "Smart TV Pro" (`G09`, producto `G09_4K_GB`). Guido cree que es una C65C |
| Sistema | Google TV sobre Android TV 12 (SDK 31), parche de seguridad 2025-12-05, build `STT2.230203.001.AS24` |
| Chip | Amlogic (`t5m`), 32 bits, **2 GB de RAM** (el límite real de todo) |
| Almacenamiento | 8 GB |
| Pantalla | 4K física, interfaz a 1920×1080 (densidad 320) |
| Control | TCL RC813 por Bluetooth (no programable, no manda infrarrojo a otros equipos) |
| Audio | Receptor **Sony STR-KG700** por **fibra óptica**. El Sony no tiene red ni HDMI ARC: solo entiende su control infrarrojo |

## Cómo conectarse

1. En la TV: Settings → System → About → apretar 7 veces "Android TV OS build" → aparece "Developer options".
2. Settings → System → Developer options → **USB debugging** activado. En esta TV no existe "Wireless debugging": con USB debugging activado, igual escucha ADB por la red en el puerto 5555.
3. La TV y la PC tienen que estar en **la misma red Wi-Fi** (al principio la PC estaba en una red y la TV en otra y no se veían).
4. En la PC: `adb connect 192.168.0.219:5555`. La primera vez la TV pregunta "Allow USB debugging?" → "Always allow from this computer" → Allow.
5. Si la IP cambió: buscar en la red un equipo con el puerto 5555 abierto (así se encontró la primera vez), o mirarla en Settings → Network & Internet.

`adb` se instaló con `winget install Google.PlatformTools` (ruta exacta en `CLAUDE.md`).

## Lo que se encontró el primer día (31/10/2026)

- RAM libre ~450 MB de 2 GB, ~690 MB en memoria comprimida, **6 días sin reiniciar**.
- Apps de fondo comiendo memoria: Plex (286 MB), el inicio de Google TV (100 MB), Ambient con el panel de casa inteligente (52 MB), Spotify, el Asistente, Netflix y servicios de TCL.
- ~25 apps de fábrica de relleno (demo de tienda, menú de hotel, TCL Channel, registros de uso…).
- Alexa (la integración "Works with Alexa" de TCL) se cerraba por falta de memoria y se reabría.

## Qué se hizo, en orden

### 1. Limpieza básica
- `am force-stop com.plexapp.android` y `pm trim-caches 999G` (borra solo caché): RAM libre de 458 MB a 747 MB, espacio de 3,4 GB a 4,2 GB.

### 2. Apps de fábrica desinstaladas (27)
Guido eligió cuáles. Se quitaron con `pm uninstall -k --user 0 <paquete>`: no se borran del sistema, solo de su usuario, y **se pueden restaurar**. La lista exacta está en [`apps-quitadas.txt`](apps-quitadas.txt):

| Paquete | Qué era |
|---|---|
| `com.tcl.exhibit`, `com.tcl.esticker` | Modo demo y etiquetas de tienda |
| `com.tcl.hotelmenu` | Menú para hoteles |
| `com.tcl.ocean.instructions`, `com.tcl.repairguide` | Manual y guía de reparación |
| `com.tcl.channelplus` | TCL Channel |
| `com.tcl.messagebox`, `com.tcl.logkit`, `com.tcl.waterfall.overseas` | Mensajes, registros de uso y contenido promocional de TCL |
| `com.mirakulo.astrotv.service` | Servicio de contenido de un socio |
| `com.google.android.play.games`, `com.google.android.feedback` | Play Juegos, comentarios a Google |
| `com.android.printspooler`, `com.google.android.marvin.talkback` | Impresión, lector de pantalla |
| `com.tcl.magiconnectfree`, `com.tcl.hearaid` | Magiconnect y ayuda auditiva (Guido no los usa) |
| `com.tcl.tv.tclhome_passive` | Contenido de la pestaña TCL (se quitó intentando sacar esa pestaña) |
| `com.tcl.dashboard` | Panel de cuenta TCL del botón TCL Home (01/11; ese botón ahora es de Volumen Maestro) |
| `com.tcl.usercenter`, `com.tcl.guard` | Cuenta TCL y Safety Guard, el "limpiador" de TCL (01/11) |
| `com.android.providers.calendar`, `com.google.android.syncadapters.calendar` | Calendario y su sincronización (01/11) |
| `com.android.dynsystem`, `com.android.managedprovisioning`, `com.google.android.onetimeinitializer` | Actualización dinámica, aprovisionamiento de empresa, inicializador (01/11) |
| `com.google.android.videos`, `com.google.android.youtube.tvmusic` | Tienda de películas de Google y YouTube Music (01/11; Guido no los usa) |

El 01/11 Guido además **borró** (desinstalación común, sin vuelta atrás) dos apps que había instalado a mano: Saki Play (`play.saki.app`) y MX IPTV (`tv.mxliptv.app`). Después de quitar todo, la TV reinició sin errores.

**Se dejaron a propósito**: Game Bar, Alexa (`com.tcl.smartalexa` y su servicio `com.tcl.ttvs`, que **no hay que tocar**), Netflix, Prime, YouTube Music, AirPlay, Miracast, Media Player, `com.tcl.partnercustomizer` (maneja el ícono "TCL Home" y probablemente los botones del control) y todo lo del sistema.

**Para restaurar**: [`restaurar-apps.ps1`](restaurar-apps.ps1) (todas, o una: `.\restaurar-apps.ps1 com.tcl.hearaid`). A mano: `adb shell cmd package install-existing --user 0 <paquete>`. Un reseteo de fábrica también las trae (y borra todo lo demás).

Resultado tras reiniciar: **894 MB de RAM libre**.

### 3. Recomendaciones de Google TV
- Guido quería quedarse con sus apps, "Continue watching" y las filas de sus apps, y sacar la publicidad.
- El "modo solo apps" de Google TV saca "Continue watching" y el Asistente: descartado.
- **La pestaña "TCL"** del inicio: se probó quitar `tclhome_passive`, quitar `partnercustomizer`, reiniciar y borrar los datos del inicio (`pm clear com.google.android.apps.tv.launcherx`), todo junto. **No se va: la manda el servidor de Google por ser una TCL.** Se restauró `partnercustomizer`.
- **El banner grande de Prime Video** arriba de todo: se apagaron las notificaciones de Prime (Settings → Apps → Prime Video → Notifications) y no cambió. También lo arma el servidor; Prime no tiene canales propios guardados en la TV.
- **01/11**: en Ajustes → Cuentas y perfiles → la cuenta → Preferencias de contenido → **Apps y servicios** se apagó Prime Video (Prime sigue andando). La fila de Prime no se fue al momento (mirar en unos días). Se probó otra vez el **modo solo apps**: saca la fila de Prime y la pestaña TCL, pero también "Continue watching" y la búsqueda/voz de Google. Guido prefiere "Continue watching": se volvió atrás y el inicio quedó igual que antes (mismo orden de apps).
- **01/11**: en la misma pantalla de la cuenta se apagó **"Autoplay video"** (avances, canales Freeplay y algunas publicidades que arrancaban solos en el inicio). Es un ajuste del inicio de Google TV; los avances dentro de cada app tienen su propio ajuste.
- Borrar los datos del inicio reseteó el orden de las apps; se volvió a ordenar a mano con el modo "Reorder" según el respaldo [`respaldo-inicio/orden-apps.txt`](respaldo-inicio/orden-apps.txt).

### 4. Alexa
- La TV ya estaba vinculada a la cuenta de Amazon de Guido (en "Works with Alexa" aparece "Sign Out"). Apagaba bien la TV pero **no la podía prender**: "device doesn't support requested value".
- Causa: en reposo la TV entraba en modo **Hibernated** y cortaba el Wi-Fi, así que la orden de prender no llegaba.
- Arreglo: Settings → System → Power & Energy → TV Standby → **Standby Mode: Network Standby** (`tcl_sys_network_standby_enabled=1`). Ahora Alexa la prende, la apaga, abre Netflix, etc. (comprobado en los registros: llega un "MagicPacket" y la TV despierta).
- Efecto secundario: mandar algo por Chromecast prende la TV sola. Gasta ~1-2 W más en reposo.

### 5. El problema del volumen (lo que dio origen a Volumen Maestro)
- El Sony va por fibra óptica y TCL deja la salida óptica a **volumen fijo**: los botones de volumen muestran "Audio output through optical fibers does not support volume adjustment".
- Guido antes usaba un Chromecast, que bajaba su propia señal antes de mandarla: el Sony quedaba fijo y el volumen se manejaba desde el Chromecast.
- La investigación completa (efectos de audio, volumen maestro, Dolby, offload, túnel) está en [`../audio-dolby.md`](../audio-dolby.md). La solución es la app de este repo.

### 6. Ajustes de audio que cambió la investigación (y cómo quedaron)

| Ajuste | De fábrica | Con Volumen Maestro ENCENDIDO | APAGADO |
|---|---|---|---|
| Audio Output (`global tcl_device_out_type`) | Auto (Internal Speaker) | **Optical** (32) — se cambió a mano en el menú | Optical |
| Digital Audio Out (`system tcl_sys_audio_spdif_outmode`) | PCM (1) | PCM (1) | Auto (3) |
| `global encoded_surround_output` | (sin valor) | 1 (nunca surround) | (sin valor) |
| Netflix `global nrdp_audio_platform_capabilities` | Dolby (ddplus/atmos `enabled:true`) | Dolby desactivado | Dolby |
| Netflix `global nrdp_external_surround_sound_enabled` | 1 | 0 | 1 |
| Volumen maestro (AudioFlinger) | 1.0 | según el control | 1.0 |

El valor original exacto de Netflix está en [`respaldo-inicio/audio-netflix-original.txt`](respaldo-inicio/audio-netflix-original.txt) (la app también lo guarda). "Digital Audio Out" estaba en PCM de fábrica; Guido lo pasó a Auto durante las pruebas, y Auto es lo que deja el modo APAGADO.

### 7. Volumen Maestro instalado
- Versión 1 (31/10, Java sin Gradle) → versión 2 (31/10, Compose, 5 pestañas, 11 temas) → 2.3 (01/11: ecualizador de la mezcla, 15 temas, panel de apps con TCL Home). Se actualiza siempre "encima" con la misma firma.
- Servicio de accesibilidad `local.volumenmaestro/.VolumeService` habilitado. La app tiene su propia clave ADB autorizada en la TV ("volumen-maestro@tv").
- Tema elegido: Colina espiral (01/11). Curva: pareja, 0,5 dB por punto. Transición: apagada.
- El 01/11 se quitó `com.tcl.dashboard` (el panel de cuenta TCL que abría el botón TCL Home): ahora ese botón lo usa Volumen Maestro.

### 8. DNS privado con AdGuard (01/11/2026)
- `settings put global private_dns_specifier dns.adguard-dns.com` + `settings put global private_dns_mode hostname`. Antes estaba sin configurar (`null`, o sea automático).
- Bloquea dominios de publicidad y rastreo en toda la TV (comprobado: `doubleclick.net` no resuelve). No saca la publicidad de YouTube.
- Guido probó Netflix, Disney+, Prime, Max, Plex y YouTube: andan.
- **Para deshacerlo**: `adb shell settings put global private_dns_mode off` (o Ajustes → Red → DNS privado).
- Ya estaban: animaciones del sistema en 0,5×. No existen en Android 12 los servicios de publicidad de Google que sugieren algunas guías (`adservices`, `ondevicepersonalization`, `federatedcompute`).
- Se probó y desinstaló **Dialogue Mode** (`local.dialoguemode`, de GitHub), usado solo para comprobar que los efectos de audio no alcanzaban a Netflix.

## Cosas que NO funcionan (para no volver a probarlas)
- Programar el control de la TV para que maneje el Sony: el RC813 no se puede programar y la TV no tiene emisor infrarrojo.
- Ver el volumen del Sony con Dolby: el Dolby pasa comprimido (ver `audio-dolby.md`). Haría falta un emisor infrarrojo por Wi-Fi (Broadlink); Guido no lo quiere.
- El parámetro oculto `spdif_volume` del chip de audio: acepta valores pero no cambia nada.
- Sacar la pestaña TCL o el banner de Prime (los manda Google).
- Teclas simuladas desde la PC (`input keyevent`) no llegan al servicio de accesibilidad.

## Mantenimiento recomendado
- Reiniciar seguido (con 2 GB es lo que más se nota): la app tiene reinicio programado (pestaña Optimizar).
- "Optimizar ahora" en la app cuando la TV se ponga lenta (Mubi junta mucha caché).
- Si la TV se actualiza, abrir la app: la pestaña Inicio dice si algo se desacomodó y "Reparar todo" lo vuelve a poner.

## Sesiones
- [`sesiones/2026-10-31.md`](sesiones/2026-10-31.md) — el primer día: diagnóstico, limpieza, Alexa, audio y Volumen Maestro v1 y v2.
- [`sesiones/2026-11-01.md`](sesiones/2026-11-01.md) — el mudo de Disney+ (efectos de TCL), temas de Noche de brujas y el botón TCL Home.
