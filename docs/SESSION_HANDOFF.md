# SESSION_HANDOFF — Volumen Maestro

> Save game del proyecto. `/cierre` lo SOBREESCRIBE ENTERO en cada sesión — acá nunca se apila historia (eso vive en CHANGELOG). El hook SessionStart lo inyecta en cada chat nuevo: **tope de 5.500 bytes** (unas 70 líneas); si se pasa, el hook lo corta y avisa "CORTADO".

**Cierre**: 2026-10-31 · **Último commit**: ver `git log -1` (Volumen Maestro 2.1)

## Qué se hizo
- Diagnóstico y limpieza de la TV TCL de Guido: caché, 17 apps de fábrica quitadas (reversible), Alexa arreglada con Network Standby. Historia en `docs/tv-guido-tcl/`.
- Investigación del audio por fibra: el volumen maestro de AudioFlinger funciona con todo lo PCM (también Netflix en túnel); con Dolby no hay forma. Ver `docs/audio-dolby.md`.
- Volumen Maestro 1.0 → 2.0 (Compose for TV, motor en `:motor`, 5 pestañas, 12 temas) → 2.1 (vuelve la bajada progresiva al mantener apretado; el atajo se escucha con `getevent`, ADR-011).
- Repo público guidocameraeq/volumen-maestro con README, landing (Pages desde `/docs`) y releases v2.0.0 y v2.1.0 con el APK.

## Estado
- TV: Volumen Maestro 2.1 instalada y ENCENDIDA. Tema Sable láser, curva Ampli (la eligió Guido), atajo Info mantenido 2 s. Audio Output Optical, Digital Audio Out PCM, Netflix sin Dolby.
- Verificado por Guido: atajo Info y bajada progresiva al mantener apretado.
- `keystore.jks` y `.local-privado/` solo en la PC (no versionados).

## En curso
- Nada a medias en el código.

## Próximo paso CONCRETO
Activar el hook SessionStart: correr el comando de PowerShell que crea `.claude/settings.json` desde `docs/claude-settings.json` (está en el README de esta sesión y en `docs/TODO.md`). Después, con Guido: probar el volumen en Plex/Disney+/Prime/HBO y un reinicio programado real.

## Bloqueos
- Ninguno.

## Archivos tocados
- Todo el repo (sesión fundacional).

## Contexto que no está en otro doc
- Guido usa Ambient con fondos propios y lo quiere así; el Game Bar y Alexa no se tocan.
- La app tiene su propia clave ADB autorizada en la TV ("volumen-maestro@tv"); la PC tiene la suya.
- La interfaz de la app se puede manejar desde la PC con `input tap x y` (más seguro que las flechas).
- El respaldo `/sdcard/VolumenMaestro/ajustes.json` queda en versión 1 hasta el próximo cambio de ajustes; la app lo migra al leerlo.
