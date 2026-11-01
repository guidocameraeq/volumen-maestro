# SESSION_HANDOFF — Volumen Maestro

> Save game del proyecto. `/cierre` lo SOBREESCRIBE ENTERO en cada sesión — acá nunca se apila historia (eso vive en CHANGELOG). El hook SessionStart lo inyecta en cada chat nuevo: **tope de 5.500 bytes** (unas 70 líneas); si se pasa, el hook lo corta y avisa "CORTADO".

**Cierre**: 2026-11-01 · **Último commit**: ver `git log -1` (cierre de sesión)

## Qué se hizo
- Arreglado el mudo con la app encendida (empezó con Disney+, era todo): al prender la TV, TCL engancha 7 efectos de sonido a la mezcla y Android redondea el volumen maestro a 0 o 1. Un ecualizador plano en la mezcla controla el volumen (ADR-013, `audio/MixVolume.kt`). Guido lo confirmó de oído en Disney+, YouTube, Netflix, Plex, Prime y Max, también después de reiniciar.
- Retraso al mantener apretado: era la transición; queda "Apagada" de fábrica.
- 3 temas de Noche de brujas (Colina espiral, Candelabro gótico, Hilo de seda) y la pestaña Temas en 4 grupos; arreglado el foco que corría la columna derecha.
- Botón TCL Home como "Alt-Tab" (Volumen Maestro 2.3, ADR-014, `docs/recientes.md`): un toque abre el panel de apps recientes, dos toques vuelven a la anterior. Guido lo probó con el control. Se quitó `com.tcl.dashboard` (reversible).
- Arreglado el lector del control duplicado al instalar/quitar apps (cada toque llegaba doble).
- Volumen Maestro 2.3 (versionCode 5) instalada en la TV.
- 8 investigadores (Sonnet) buscaron ideas para mejorar la TV: resumen en `docs/ideas-google-tv.md`.

## Estado
- TV: Volumen Maestro 2.3 ENCENDIDA, panel de apps prendido. Con la app encendida, `dumpsys media.audio_flinger | grep "effects for session"` da 8 (los 7 de TCL + nuestro Equalizer). Transición de Guido: Apagada.
- Red: la TV está en "Yulius 4" (`192.168.0.219`); la PC a veces queda en "Domotics3" (ver `CLAUDE.md`).
- Se quitó `com.tcl.dashboard` (anotado en `apps-quitadas.txt`).

## En curso
- Nada a medias.

## Próximo paso CONCRETO
Con Guido: repasar `docs/ideas-google-tv.md` y elegir cuáles hacer (las recomendadas: SmartTube desde su GitHub, DNS con AdGuard, animaciones 0,5×, temporizador para dormir en la app). Antes, que pruebe los temas de Noche de brujas en pantalla.

## Bloqueos
- Ninguno.

## Archivos tocados
- `recientes/*` (nuevo), `engine/{Protocol,EngineService}.kt`, `ui/SettingsScreen.kt`, `audio/MixVolume.kt` (nuevo), `engine/Engine.kt`, `AndroidManifest.xml` (MODIFY_AUDIO_SETTINGS), `settings/Config.kt`, `overlay/Theme.kt` (grupos), `overlay/themes/{Espiral,Candelabro,Telarana}Theme.kt` (nuevos), `ui/ThemesScreen.kt`, `ui/MainActivity.kt`, `app/build.gradle.kts`.
- Docs: decisiones (ADR-013/014), arquitectura, audio-dolby, temas, `mockups/temas-spooky.html`, `specs/2026-11-01-panel-apps.md`, `ideas-google-tv.md`, `tv-guido-tcl/sesiones/2026-11-01.md`, CLAUDE.md (red).

## Contexto que no está en otro doc
- Bajar el volumen maestro sin el ecualizador deja la TV muda con los efectos de TCL y al rato puede tumbar el HAL de audio (el servidor se reinicia solo). No probar con `service call media.audio_flinger 7` valores < 0,5 con la app apagada sin saberlo.
- La verificación del 31/10 (1,0 → 0,94) no servía: con el redondeo 0,94 suena igual que 1,0. Para probar el volumen, bajar por debajo de 0,5.
- El panel de apps solo se abre con el control real (la shell no puede simular TCL Home); para verlo, pedirle a Guido que lo deje abierto y `screencap`.
- Pruebas de pestañas desde la PC: `am start -n local.volumenmaestro/.ui.MainActivity` + flechas + `screencap` anduvo bien (no apretar OK sin captura).
- Guido: la auto-actualización desde GitHub quedó descartada por ahora (TODO).
