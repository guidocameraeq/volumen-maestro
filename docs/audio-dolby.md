# El audio por fibra óptica, el Dolby y por qué funciona el volumen maestro

La investigación que llevó a Volumen Maestro, con lo que se probó y lo que salió. Sirve para no repetir pruebas y para entender por qué la app hace lo que hace.

## El problema

- La TV (TCL, Android TV 12, chip Amlogic) manda el sonido por **fibra óptica (S/PDIF)** a un receptor Sony STR-KG700.
- La fibra solo lleva sonido, no órdenes. El Sony no tiene red ni HDMI ARC (que es lo que permite que el control de la TV maneje un equipo).
- Con **Audio Output = Optical**, TCL trata la salida como **volumen fijo**: los botones de volumen no cambian nada y muestran *"Audio output through optical fibers does not support volume adjustment"*.
- Antes Guido usaba un Chromecast, que bajaba su propia señal antes de mandarla: el Sony quedaba fijo y el volumen se manejaba con el control del Chromecast. Volumen Maestro recrea eso dentro de la TV.

## Los caminos del audio dentro de Android (AudioFlinger)

Cuando una app suena, su audio entra por uno de estos caminos ("hilos de salida"). Se ven con `adb shell dumpsys media.audio_flinger`:

| Camino | Quién lo usa | ¿Efectos de audio? | ¿Volumen maestro? |
|---|---|---|---|
| **MIXER** (mezclador) | YouTube, Spotify, los menús, casi todo | ✅ | ✅ |
| **DIRECT + HW_AV_SYNC** ("túnel") | Netflix con audio PCM (sincroniza audio y video en el chip) | ❌ Android no permite efectos en hilos DIRECT | ✅ |
| **OFFLOAD** (E-AC3 / Dolby Digital Plus) | Netflix y otras apps con Dolby, decodificado por el chip | ❌ | ❌ el chip lo ignora |
| **Passthrough** (AC3/DTS tal cual a la fibra) | Con "Digital Audio Out = Auto", el Dolby sale comprimido al Sony | ❌ | ❌ imposible: va comprimido |

## Lo que se probó (31/10/2026)

| Prueba | Cómo | Resultado |
|---|---|---|
| Volumen de la salida óptica | `cmd media_session volume --stream 3 --set N` con salida Optical | El índice cambia, el sonido no (TCL lo ignora) |
| Parámetro oculto del chip `spdif_volume` | `AudioManager.setParameters("spdif_volume=N")` (llega a `audio_hw_tcl`) | No cambia nada |
| Efecto global de audio (DynamicsProcessing en la sesión 0) | App **Dialogue Mode** (github.com/colsonj/dialogue-mode) con ganancia −30/−60 dB | ✅ YouTube; ❌ Netflix (túnel/offload) |
| Netflix sin Dolby | `nrdp_audio_platform_capabilities` con `ddplus/atmos enabled:false` + `nrdp_external_surround_sound_enabled=0` | Netflix pasa de OFFLOAD E-AC3 a DIRECT PCM, pero los efectos siguen sin llegar |
| **Volumen maestro** | `service call media.audio_flinger 7 f <0..1>` (setMasterVolume) | ✅ **Netflix PCM, YouTube y todo lo PCM** |
| Volumen maestro con Dolby | Netflix con Dolby (OFFLOAD E-AC3) | ❌ no cambia |
| Volumen maestro + "Digital Audio Out = Auto" | Dolby en passthrough al Sony | ❌ no cambia |

## La solución que quedó

**Modo ENCENDIDO** de la app: todo sale como **PCM estéreo** y pasa por el volumen maestro.
- `global encoded_surround_output=1` (las apps creen que no hay sonido envolvente y mandan PCM).
- Netflix sin Dolby (`nrdp_*` como arriba) y se reinicia Netflix para que lo tome.
- "Digital Audio Out" en **PCM** (`system tcl_sys_audio_spdif_outmode=1`): la TV decodifica todo y no pasa nada comprimido.
- El volumen maestro sigue al número de volumen que TCL guarda aunque lo muestre como "no soportado".

**Lo que se pierde**: el sonido envolvente 5.1 en el Sony. Con la fibra en PCM llega estéreo (que es lo que el Sony recibía de todos modos en PCM).

**Modo APAGADO**: todo como venía. Las apps mandan Dolby, "Digital Audio Out" en Auto lo pasa tal cual al Sony (5.1) y el volumen se maneja con el control del Sony.

## Por qué el Dolby no se puede controlar desde la TV

Con passthrough el Dolby viaja **comprimido** hasta el Sony: para bajarle el volumen habría que decodificarlo, atenuarlo y volver a codificarlo, y la TV no ofrece ese modo (Digital Audio Out solo tiene PCM y Auto). Con offload el chip lo decodifica, pero ignora el volumen maestro en la salida óptica. Ningún aparato puede bajarle el volumen a un Dolby comprimido sin decodificarlo; el Chromecast tampoco lo hacía.

## Alternativas descartadas

| Alternativa | Por qué no |
|---|---|
| HDMI ARC / CEC | El STR-KG700 no tiene HDMI ARC |
| Programar el control de la TV por infrarrojo | El control TCL RC813 es Bluetooth y no se puede programar; la TV no tiene emisor IR |
| Emisor infrarrojo por Wi-Fi (Broadlink RM4 mini) + la app | Funcionaría incluso con Dolby (cambia el volumen del propio Sony). Guido no quiere comprar hardware |
| Volver al Chromecast | Funciona, pero Guido quiere todo en la TV |
| Shizuku para los permisos | Hay que relanzarlo a mano después de cada reinicio; la conexión ADB local de la app sobrevive sola |

## Comandos útiles

```text
adb shell dumpsys media.audio_flinger | grep -E "Output thread|HAL format|AudioStreamOut"   # qué camino usa cada app
adb shell service call media.audio_flinger 9            # leer el volumen maestro (float en hex)
adb shell service call media.audio_flinger 7 f 0.5      # fijarlo (Android 12; los códigos cambian entre versiones)
adb shell dumpsys audio | grep -A6 "^- STREAM_MUSIC"     # índice de volumen por salida (spdif = 0x80000)
adb shell settings get global nrdp_audio_platform_capabilities
```
