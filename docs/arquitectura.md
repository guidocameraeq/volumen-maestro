# Arquitectura de Volumen Maestro

## En una imagen

```mermaid
flowchart LR
    subgraph TV["TV TCL (Android TV 12)"]
        CTRL["Control remoto<br/>volumen ±"] -->|TCL guarda el índice 0..100| AS[AudioService]
        AS -->|VOLUME_CHANGED_ACTION| M
        subgraph motor["proceso :motor"]
            M["Engine<br/>(curva, rampa, modos, chequeos)"]
            VS["VolumeService<br/>(accesibilidad)"] --> M
            M --> OV["OverlayController<br/>2 ventanas + tema"]
            M --> SH["Shell<br/>2 canales ADB"]
            ES["EngineService<br/>(Messenger)"] --> M
        end
        subgraph ui["proceso principal (solo con la app abierta)"]
            UI["MainActivity<br/>Compose for TV, 5 pestañas"] --> EC[EngineClient]
        end
        EC <-->|mensajes| ES
        SH -->|127.0.0.1:5555| ADBD[adbd de la TV]
        ADBD -->|service call media.audio_flinger 7| AF[AudioFlinger<br/>volumen maestro]
        ADBD -->|settings put …| SET[Ajustes de TCL / Netflix]
        AF --> FIBRA[Salida óptica PCM]
    end
    FIBRA --> SONY[Sony STR-KG700<br/>volumen fijo alto]
```

## Las piezas

| Pieza | Archivo | Qué hace |
|---|---|---|
| Servicio de accesibilidad | `VolumeService.kt` | Android lo arranca con la TV. Le da al motor ventanas encima de todo (`TYPE_ACCESSIBILITY_OVERLAY`). No filtra teclas. Mantiene el nombre de la v1 |
| Motor | `engine/Engine.kt` | Singleton del proceso `:motor`. Escucha el volumen, aplica la curva, maneja los modos, los chequeos, el atajo, el reinicio y la copia |
| Shell | `engine/Shell.kt` + `AdbClient.java` + `AdbKeys.java` | Cliente ADB mínimo propio contra el adbd de la TV, con su clave RSA. Canales: `volume` (rápido), `commands` (tareas largas) y una conexión de streaming para `getevent` (atajo) |
| Volumen maestro | `audio/MasterVolume.kt` | Hilo propio que manda `setMasterVolume` con rampa en dB (pasos de 25 ms) y lo lee para el watchdog |
| Volumen en la mezcla | `audio/MixVolume.kt` | Ecualizador plano en la sesión 0 que recibe el volumen maestro, así los efectos de TCL no lo redondean a 0 o 1 (ADR-013) |
| Curva | `audio/VolumeCurve.kt` | Índice → ganancia: Pareja (dB por punto), Suave (cuadrática), Ampli (curva en S de −60 dB). En 100 siempre 1.0 |
| Modos | `audio/AudioSetup.kt` | Los ajustes de ENCENDIDO y APAGADO (tabla en `tv-guido-tcl/README.md`) |
| Carteles | `overlay/OverlayController.kt` | Las 2 ventanas (inferior 960×150 en x480/y882; lateral 108×430 en x1740/y326), animación de entrada/salida, solo anima mientras se ven |
| Temas | `overlay/Theme.kt`, `overlay/themes/*` | Interfaz `Theme` + registro. Ver `temas.md` |
| Ajustes | `settings/Config.kt`, `ConfigStore.kt` | `Config` (JSON), guardado en el proceso motor + copia en `/sdcard/VolumenMaestro/ajustes.json` |
| Optimizar | `system/Optimizer.kt` | Estado (memoria, espacio, tiempo encendida), `pm trim-caches`, `am kill-all` |
| Reinicio | `system/RebootScheduler.kt`, `Engine.onScreenOff` | Regla de la noche y próxima noche (probada); al apagar la TV, el motor reinicia en el momento (ADR-016) |
| Panel de apps | `recientes/*` | Botón TCL Home: panel de apps abiertas y volver a la anterior. Ver `recientes.md` |
| Puente | `engine/EngineService.kt`, `engine/Protocol.kt` | Messenger: la interfaz pide, el motor responde con un `Bundle` de estado |
| Interfaz | `ui/*` | Compose for TV: Inicio, Volumen, Temas, Optimizar, Ajustes. `EngineClient` pregunta el estado cada 1 s |

## Flujo de un toque de volumen

1. El control manda volumen −. TCL lo procesa (y muestra su cartel), y baja el índice de la salida óptica.
2. Android emite `VOLUME_CHANGED_ACTION` (STREAM_MUSIC). `Engine.onTvVolumeChanged` lo recibe.
3. `OverlayController.showVolume` muestra los dos carteles del tema elegido, que tapan los de TCL durante 3,8 s.
4. `VolumeCurve.gain` calcula la ganancia y `MasterVolume.setTarget` la manda con la transición elegida.
5. Cada 10 s el watchdog lee el volumen maestro real y lo reaplica si algo lo cambió. Al prender la pantalla también.

## Arranque

`VolumeService.onServiceConnected` → `Engine.start`: carga la config, crea las ventanas, registra los receptores y en el hilo de comandos: carga o crea la clave ADB, busca la copia de seguridad si es la primera vez, se concede el permiso de alarmas, comprueba que el volumen maestro responde, atiende la marca del reinicio programado, reafirma los ajustes del modo ENCENDIDO y programa el próximo reinicio. **Al prender la TV siempre queda ENCENDIDO.**

## Protocolo interfaz ↔ motor

Mensajes en `Protocol.kt`: `GET_STATE`, `SET_ENABLED`, `SET_CONFIG`, `REPAIR_ALL`, `FIX`, `OPTIMIZE`, `GET_STATS`, `REBOOT_NOW`, `PREVIEW_THEME`, `START_LEARN`/`STOP_LEARN` (atajo), `RESTORE_BACKUP`/`DISMISS_BACKUP`, `EXPORT_BACKUP`/`IMPORT_BACKUP`, `PREPARE_UNINSTALL`, `GET_LOG`. Los lentos corren en `Engine.commands` y responden al terminar.

## Atajo del control

El motor abre una tercera conexión ADB y deja corriendo `getevent`: lee las señales crudas del control (`tipo código valor`, por ejemplo `0001 00c3 00000001` = Info apretado) y reconoce el botón del atajo por su **código del kernel** (Info del TCL RC813 = `0xC3`). Con "Elegir botón" toma el próximo botón apretado que no sea volumen, encendido, flechas, OK, Atrás ni Inicio. "Mantener 2 s" mide entre apretado y soltado; "Toque simple" actúa al apretar.

**No** se usa el filtro de teclas de accesibilidad: con ese filtro activo Android deja de repetir las teclas mantenidas y el volumen baja de a un paso (ver ADR-011). La contra es que la señal no se puede "consumir": el botón igual hace lo suyo en la TV.

## Pruebas

`app/src/test`: curvas (`VolumeCurveTest`), próximo reinicio (`RebootSchedulerTest`), JSON de la config y lectura del float de AudioFlinger (`ConfigTest`). Lo demás se verifica en la TV (ver `/instalar` y `desarrollo.md`).
