# Decisiones (ADR)

Cada decisión con alternativas: qué se eligió, qué se descartó y por qué. Las nuevas van abajo con el número siguiente.

| # | Decisión | Fecha |
|---|---|---|
| 001 | Volumen maestro de AudioFlinger en vez de efectos de audio | 31/10/2026 |
| 002 | Permisos de shell con una conexión ADB local propia | 31/10/2026 |
| 003 | Seguir el número de volumen de TCL en vez de capturar las teclas | 31/10/2026 |
| 004 | Tapar el cartel de TCL con un panel opaco | 31/10/2026 |
| 005 | Modo ENCENDIDO sin Dolby, con interruptor para volver | 31/10/2026 |
| 006 | Compose for TV y un proceso aparte para el motor | 31/10/2026 |
| 007 | Temas sin estado, el mismo código en vista previa y en la TV | 31/10/2026 |
| 008 | Actualizar siempre "encima": misma firma, mismo paquete y misma clase | 31/10/2026 |
| 009 | Reinicio programado con `reboot:` de adbd y volver a apagar la pantalla | 31/10/2026 |
| 010 | Copia de seguridad en `/sdcard` escrita por la shell | 31/10/2026 |
| 011 | Atajo del control con `getevent` en vez del filtro de teclas | 31/10/2026 |
| 012 | La clave de firma va en el repo público | 31/10/2026 |

## ADR-001 — Volumen maestro en vez de efectos de audio
- **Contexto**: la salida óptica es de volumen fijo; hacía falta una capa de volumen por software.
- **Opciones**: efecto global DynamicsProcessing (sesión 0) · volumen maestro de AudioFlinger.
- **Elegido**: volumen maestro. Se aplica también al camino directo "túnel" de Netflix; los efectos no (Android rechaza efectos en hilos DIRECT). Detalle en `audio-dolby.md`.
- **Costo**: necesita permisos de shell (ver ADR-002).

## ADR-002 — Conexión ADB local propia
- **Contexto**: `setMasterVolume` y cambiar ajustes globales requieren permisos que una app común no tiene.
- **Opciones**: root (no hay) · Shizuku · conectarse al adbd de la propia TV (127.0.0.1:5555) con una clave RSA propia.
- **Elegido**: ADB local con un cliente mínimo propio (`AdbClient.java`, sin librerías). Se autoriza una vez en la TV y sobrevive a reinicios; Shizuku hay que relanzarlo a mano.
- **Costo**: la depuración USB tiene que quedar activada.

## ADR-003 — Seguir el índice de volumen de TCL
- **Contexto**: la v1 interceptaba las teclas con el filtro de accesibilidad, pero TCL procesa las de volumen antes y nunca llegan.
- **Elegido**: escuchar `VOLUME_CHANGED_ACTION` / `STREAM_MUTE_CHANGED_ACTION`: TCL guarda el índice 0..100 aunque diga "no soportado", y la app lo traduce a volumen maestro. Bonus: Alexa también mueve ese índice.
- **Costo**: el paso por toque lo decide TCL; el usuario ajusta "cuánto suena cada punto" con la curva.

## ADR-004 — Tapar el cartel de TCL
- **Contexto**: TCL muestra su cartel siempre que la salida es óptica (`TclVolumeFunction.showDeviceUnconnectToast`, leído de SystemUI). Lo único que lo controla es `tcl_device_out_type`, que también rutea el audio.
- **Elegido**: dos ventanas de accesibilidad opacas, del tamaño justo, encima del cartel inferior y de la barra lateral, que duran más que el cartel (3,8 s contra 3,5 s).
- **Descartado**: cambiar `tcl_device_out_type` (riesgo de prender los parlantes o cortar la fibra).

## ADR-005 — Modo sin Dolby, con interruptor
- **Contexto**: con Dolby (offload o passthrough) ningún volumen por software llega al Sony.
- **Opciones**: emisor infrarrojo por Wi-Fi (funciona con Dolby, requiere comprar un Broadlink) · resignar el Dolby.
- **Elegido**: Guido prefirió resignar el Dolby. ENCENDIDO = todo PCM; APAGADO = la TV como venía. Al prender la TV siempre vuelve a ENCENDIDO.

## ADR-006 — Compose for TV y proceso aparte
- **Opciones**: vistas Java hechas a mano (v1, liviana) · Compose for TV.
- **Elegido**: Compose for TV para una interfaz "de verdad". El motor (servicio de accesibilidad, ventanas, ADB) corre en el proceso `:motor` sin Compose (~15 MB), y la interfaz solo ocupa memoria mientras está abierta. Se hablan por Messenger (`EngineService`).

## ADR-007 — Temas sin estado
- **Elegido**: cada tema es un `object` que dibuja todo desde `OverlayState` (nivel, índice, silencio, tiempos). Las partículas se calculan con fórmulas cerradas o arrays sembrados. Así la vista previa de la app y la TV muestran exactamente lo mismo, y no hay estado que se desincronice.
- **Costo**: efectos con "memoria" (resortes, chispas acumuladas) se aproximan con fórmulas.

## ADR-008 — Actualizar siempre encima
- **Elegido**: misma clave (`keystore.jks`, alias `vm`), mismo `applicationId` (`local.volumenmaestro`) y el servicio con el mismo nombre de clase que la v1 (`local.volumenmaestro.VolumeService`). Así la app se actualiza sin perder la clave ADB autorizada, los ajustes ni la habilitación del servicio.

## ADR-009 — Reinicio programado
- **Elegido**: alarma exacta (`setExactAndAllowWhileIdle`; el permiso se concede con `appops` por ADB). Solo reinicia con la pantalla apagada (espera hasta 3 h). Reinicia con el servicio `reboot:` de adbd (como `adb reboot`) y deja una marca: si al arrancar la pantalla se prendió sola, manda `KEYCODE_SLEEP`.
- **Requisito**: reposo en Network Standby (en Hibernated la TV no despierta para la alarma).

## ADR-010 — Copia de seguridad por la shell
- **Contexto**: una app reinstalada no puede leer archivos que creó la instalación anterior.
- **Elegido**: la copia (`/sdcard/VolumenMaestro/ajustes.json`) se escribe y se lee con la shell (base64 por ADB), así la versión nueva la encuentra y ofrece restaurarla.

## ADR-011 — Atajo con `getevent`
- **Contexto**: la 2.0 escuchaba el botón del atajo con el filtro de teclas de accesibilidad (`FLAG_REQUEST_FILTER_KEY_EVENTS`). Con ese filtro activo, Android **deja de repetir las teclas mantenidas**: mantener apretado el volumen bajaba de a un paso (medido: 3,2 s apretado = 1 solo cambio, `repeatCount` siempre 0). El control solo manda "apretado" y "soltado"; la repetición la genera Android, y se pierde cuando los eventos pasan por el filtro.
- **Opciones**: prender el filtro solo con atajo configurado (sigue rompiendo la repetición en ese caso) · leer las señales crudas del control con `getevent` por ADB.
- **Elegido**: `getevent` en una conexión ADB propia. Atajo y repetición funcionan juntos (verificado por Guido el 31/10). El atajo se guarda como código del kernel (Info = `0xC3`); la config pasó a versión 2 y migra el KEYCODE_INFO viejo.
- **Costo**: la señal no se puede consumir: el botón del atajo igual hace su función normal en la TV.

## ADR-012 — La clave de firma va en el repo público
- **Contexto**: `keystore.jks` firma la app. Android solo acepta una actualización firmada con la misma clave; sin ella hay que desinstalar, y se pierden la clave ADB autorizada y los ajustes. Guido no quiere guardar nada en la PC.
- **Opciones**: dejarla solo en la PC · guardarla en un lugar privado · subirla al repo público.
- **Elegido**: subirla al repo público, por decisión de Guido. Cualquier PC puede compilar e instalar la app como actualización con un `git clone`.
- **Riesgo aceptado**: cualquiera puede firmar una app que la TV acepte como "actualización" de Volumen Maestro. Esa app heredaría los datos de la actual, incluida su clave ADB autorizada (acceso de shell a la TV). Para que pase, alguien tendría que lograr instalarla en esta TV (con acceso a la red de la casa y a la depuración, o convenciendo a Guido de instalarla). Mitigación: instalar solo APKs compilados desde este repo o descargados de sus Releases.
