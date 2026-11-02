# SESSION_HANDOFF — Volumen Maestro

> Save game del proyecto. `/cierre` lo SOBREESCRIBE ENTERO en cada sesión — acá nunca se apila historia (eso vive en CHANGELOG). El hook SessionStart lo inyecta en cada chat nuevo: **tope de 5.500 bytes** (unas 70 líneas); si se pasa, el hook lo corta y avisa "CORTADO".

**Cierre**: 2026-11-01 · **Último commit**: ver `git log -1` (cierre de sesión)

## Qué se hizo (sesión del 01/11, bitácora en `docs/tv-guido-tcl/sesiones/2026-11-01.md`)
- **2.2 — el mudo**: al prender la TV, TCL engancha 7 efectos de sonido a la mezcla y Android redondea el volumen maestro a 0 o 1. Un ecualizador plano en la mezcla se hace cargo del volumen (ADR-013, `audio/MixVolume.kt`). Probado de oído en Disney+, YouTube, Netflix, Plex, Prime y Max. Transición apagada de fábrica (sacaba retraso).
- **Temas**: 3 de Noche de brujas (Colina espiral, Candelabro gótico, Hilo de seda), la pestaña Temas en 4 grupos y el foco arreglado.
- **2.3 — TCL Home como "Alt-Tab"** (ADR-014, `docs/recientes.md`, spec COMPLETO): un toque abre el panel de apps recientes, dos vuelven a la anterior. Arreglado el lector del control duplicado al instalar/quitar apps. Publicada en Releases (v2.3.0).
- **La TV**: DNS privado con AdGuard; 10 apps de fábrica más quitadas (27 en total, `apps-quitadas.txt`); Saki Play y MX IPTV borradas por Guido; Prime apagado en "Apps y servicios"; "Autoplay video" del inicio apagado. Modo solo apps probado y descartado (saca "Continue watching", que Guido quiere).
- 8 investigadores buscaron ideas: `docs/ideas-google-tv.md` (con lo hecho y lo descartado).

## Estado
- TV: Volumen Maestro 2.3 ENCENDIDA, tema Colina espiral, transición apagada, panel de apps prendido. Con la app encendida, `dumpsys media.audio_flinger | grep "effects for session"` da 8 (7 de TCL + nuestro Equalizer).
- Verificado al cierre: motor y un solo `getevent` corriendo; el cartel de volumen tapa el de TCL en el inicio.
- Red: TV en el Wi-Fi "Yulius" (`192.168.0.219`); la PC a veces queda en "Domotics3" (ver `CLAUDE.md`).
- Repo limpio, todo subido.

## En curso
- Nada a medias.

## Próximo paso CONCRETO
Guido va a usar este chat para otra cosa. Cuando se vuelva a Volumen Maestro: que Guido pruebe los temas de Noche de brujas en pantalla (con silencio) y mirar si la fila de Prime del inicio se fue (ver `docs/TODO.md`).

## Bloqueos
- Ninguno.

## Contexto que no está en otro doc
- Bajar el volumen maestro por debajo de 0,5 con la app apagada (sin el ecualizador) deja la TV muda y puede tumbar el HAL de audio. Para probar el volumen, bajar por debajo de 0,5 (con el redondeo, 0,94 suena igual que 1,0).
- El panel de apps solo se abre con el control real (la shell no puede simular TCL Home): pedirle a Guido que lo deje abierto y `screencap`.
- Navegar menús de la TV desde la PC anduvo bien leyendo el foco con `uiautomator dump` (más seguro que las capturas): bajar, leer qué tiene el foco, recién ahí OK.
- Las capturas de la pantalla de inicio muestran la foto de perfil de Guido: borrarlas al terminar.
- Guido: la auto-actualización desde GitHub quedó descartada por ahora; los avisos desde el celu, descartados.
