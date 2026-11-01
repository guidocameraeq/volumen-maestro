# SESSION_HANDOFF — Volumen Maestro

> Save game del proyecto. `/cierre` lo SOBREESCRIBE ENTERO en cada sesión — acá nunca se apila historia (eso vive en CHANGELOG). El hook SessionStart lo inyecta en cada chat nuevo: **tope de 5.500 bytes** (unas 70 líneas); si se pasa, el hook lo corta y avisa "CORTADO".

**Cierre**: 2026-11-01 · **Último commit**: ver `git log -1` (cierre de sesión)

## Qué se hizo
- Arreglado el mudo con la app encendida (empezó con Disney+, era todo): al prender la TV, TCL engancha 7 efectos de sonido a la mezcla y Android redondea el volumen maestro a 0 o 1. Un ecualizador plano en la mezcla controla el volumen (ADR-013, `audio/MixVolume.kt`). Guido lo confirmó de oído en Disney+, YouTube, Netflix, Plex, Prime y Max, también después de reiniciar.
- Retraso al mantener apretado: era la transición; queda "Apagada" de fábrica.
- 3 temas de Noche de brujas (Colina espiral, Candelabro gótico, Hilo de seda) y la pestaña Temas en 4 grupos; arreglado el foco que corría la columna derecha.
- Botón TCL Home identificado (`KEY_DASHBOARD`, `0xCC`, abre `com.tcl.dashboard`). Spec del panel de apps abiertas escrita y sellada, en BORRADOR (ADR-014).
- Volumen Maestro 2.2 (versionCode 4) instalada en la TV.
- 8 investigadores (Sonnet) buscaron ideas para mejorar la TV: resumen en `docs/ideas-google-tv.md`.

## Estado
- TV: Volumen Maestro 2.2 ENCENDIDA. Con la app encendida, `dumpsys media.audio_flinger | grep "effects for session"` da 8 (los 7 de TCL + nuestro Equalizer). Transición de Guido: Apagada.
- Red: la TV está en "Yulius 4" (`192.168.0.219`); la PC a veces queda en "Domotics3" (ver `CLAUDE.md`).
- Nada se quitó de la TV hoy. `com.tcl.dashboard` sigue instalado.

## En curso
- Panel de apps abiertas: solo spec (`docs/specs/2026-11-01-panel-apps.md`, BORRADOR sellado). Falta el OK de Guido → `node ~/.claude/skills/arquitecto/scripts/arquitecto.cjs ready <spec>`.

## Próximo paso CONCRETO
Chat nuevo: `inicio — ejecutá el spec docs/specs/2026-11-01-panel-apps.md`. Primero mostrarle a Guido la spec final y pasarla a READY; antes de quitar `com.tcl.dashboard`, mostrarle el panel de TCL desde la pantalla de inicio (sobre Netflix/Disney+ la captura sale vacía).

## Bloqueos
- Ninguno.

## Archivos tocados
- `audio/MixVolume.kt` (nuevo), `engine/Engine.kt`, `AndroidManifest.xml` (MODIFY_AUDIO_SETTINGS), `settings/Config.kt`, `overlay/Theme.kt` (grupos), `overlay/themes/{Espiral,Candelabro,Telarana}Theme.kt` (nuevos), `ui/ThemesScreen.kt`, `ui/MainActivity.kt`, `app/build.gradle.kts`.
- Docs: decisiones (ADR-013/014), arquitectura, audio-dolby, temas, `mockups/temas-spooky.html`, `specs/2026-11-01-panel-apps.md`, `ideas-google-tv.md`, `tv-guido-tcl/sesiones/2026-11-01.md`, CLAUDE.md (red).

## Contexto que no está en otro doc
- Bajar el volumen maestro sin el ecualizador deja la TV muda con los efectos de TCL y al rato puede tumbar el HAL de audio (el servidor se reinicia solo). No probar con `service call media.audio_flinger 7` valores < 0,5 con la app apagada sin saberlo.
- La verificación del 31/10 (1,0 → 0,94) no servía: con el redondeo 0,94 suena igual que 1,0. Para probar el volumen, bajar por debajo de 0,5.
- Pruebas de pestañas desde la PC: `am start -n local.volumenmaestro/.ui.MainActivity` + flechas + `screencap` anduvo bien (no apretar OK sin captura).
- Guido: la auto-actualización desde GitHub quedó descartada por ahora (TODO).
