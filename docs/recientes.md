# Panel de apps abiertas (botón TCL Home)

La segunda función de Volumen Maestro, separada del audio (ADR-014, spec `specs/2026-11-01-panel-apps.md`). El botón **TCL Home** del control hace de "Alt-Tab":

- **Un toque**: abre una franja abajo con las apps recientes (vista previa o banner, nombre, "en pantalla" en la actual). El foco arranca en la app anterior. **OK** la abre (vuelve donde estaba si seguía abierta), **mantener OK** la cierra, **Atrás** cierra el panel, **Limpiar todo** (arriba a la derecha) cierra todas menos las protegidas.
- **Dos toques** (menos de 400 ms): vuelve a la app anterior; desde el inicio, a la última usada.
- Se apaga en Ajustes → "Botón TCL Home".

## Cómo está hecho

Todo vive en `app/src/main/java/local/volumenmaestro/recientes/`:

| Pieza | Proceso | Qué hace |
|---|---|---|
| `RecentApps.kt` | los dos | Lee `dumpsys activity recents` (tareas "standard", en orden; incluye las que la TV cerró por memoria). Listas `HIDDEN` (no aparecen) y `PROTECTED` (no se cierran: Alexa, `com.tcl.ttvs`, Ambient, Game Bar, el inicio, la app) |
| `TapDetector.kt` | — | Toque simple o doble con una ventana de 400 ms (probado) |
| `AppPanel.kt` | `:motor` | Recibe TCL Home desde el lector `getevent` del motor (código `0xCC`), vuelve a la app anterior con `am start` del intent de inicio (`-f 0x10200000`, igual que el launcher), abre el panel, cierra apps (`am force-stop` + `am stack remove <tarea>`) y saca las vistas previas (cada 60 s con la pantalla prendida y al abrir el panel) |
| `Snapshots.kt` | los dos | `screencap` crudo (~0,5 s; en PNG tarda ~2 s) en `Android/data/local.volumenmaestro/files/capturas`, achicado a 384×216 JPEG. Solo si la app tiene el foco; si sale negra (Netflix, Disney+, a veces YouTube) se descarta y el panel usa el banner |
| `RecentsActivity.kt` | principal | El panel (Compose). Tarea propia (`taskAffinity`), translúcida y fuera de recientes. Abre apps con su intent de inicio; cerrar se lo pide al motor (`Protocol.CLOSE_APP` / `CLOSE_ALL`) |

El motor solo suma tres enganches: el código `0xCC` en `Engine.onRawKey`, `appPanel.tick()` en el watchdog y `CLOSE_APP`/`CLOSE_ALL` en `EngineService`.

## Cosas que se aprendieron (01/11/2026)
- Google TV no tiene pantalla de recientes, pero el sistema sigue llevando la lista. `am start` del intent de inicio con `NEW_TASK | RESET_TASK_IF_NEEDED` trae la tarea viva tal como estaba ("intent delivered to currently running top-most instance").
- No usar `am start -W`: se cuelga si hay Ambient (protector) en pantalla.
- La TV cierra por memoria las apps de atrás: quedan en la lista con `sz=0`. Por eso el panel no filtra por "viva".
- La shell no puede simular TCL Home (`sendevent` sin permiso sobre `/dev/input`): el botón se prueba con el control real.
- TCL Home abría `com.tcl.dashboard` (cuenta TCL, mensajes, Safety Guard); se quitó (reversible, en `tv-guido-tcl/apps-quitadas.txt`). Su pantalla no se puede capturar.
- Al instalar o quitar una app, Android reconecta el servicio de accesibilidad (`stop()` + `start()` en el mismo proceso): el lector `getevent` viejo quedaba vivo y cada toque llegaba repetido. Ahora cada lector lleva un número y el viejo se cierra.
