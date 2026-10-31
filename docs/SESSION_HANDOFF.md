# SESSION_HANDOFF — Volumen Maestro

> Save game del proyecto. `/cierre` lo SOBREESCRIBE ENTERO en cada sesión — acá nunca se apila historia (eso vive en CHANGELOG). El hook SessionStart lo inyecta en cada chat nuevo: **tope de 5.500 bytes** (unas 70 líneas); si se pasa, el hook lo corta y avisa "CORTADO".

**Cierre**: 2026-10-31 · **Último commit**: ver `git log -1` (cierre de sesión)

## Qué se hizo
- Diagnóstico y limpieza de la TV TCL de Guido: caché, apps de fábrica quitadas (reversible), Alexa arreglada con Network Standby. Historia en `docs/tv-guido-tcl/`.
- Investigación del audio por fibra: el volumen maestro de AudioFlinger funciona con todo lo PCM (también Netflix en túnel); con Dolby no hay forma. Ver `docs/audio-dolby.md`.
- Volumen Maestro 1.0 → 2.0 (Compose for TV, motor en `:motor`, 5 pestañas, 12 temas) → 2.1 (vuelve la bajada progresiva al mantener apretado; el atajo se escucha con `getevent`, ADR-011).
- Repo público guidocameraeq/volumen-maestro con README, landing (Pages desde `/docs`) y releases v2.0.0 y v2.1.0 con el APK.
- El proyecto vive en `C:\Users\Usuario\Desktop\Proyectos\volumen-maestro`.
- `keystore.jks` subido al repo público por decisión de Guido (ADR-012).
- Hook SessionStart y permisos del proyecto activos (`.claude/settings.json`, versionado).

## Estado
- TV: Volumen Maestro 2.1 instalada y ENCENDIDA. Tema Sable láser, curva Ampli (la eligió Guido), atajo Info mantenido 2 s. Audio Output Optical, Digital Audio Out PCM, Netflix sin Dolby.
- Verificado al cierre (por ADB): motor y lector `getevent` corriendo; bajar/subir el volumen mueve el volumen maestro (1.0 → 0,94 → 1.0), o sea la app sigue autorizada. El panel sobre el cartel de TCL no se pudo capturar (Netflix estaba abierto: sale negro por DRM); se había verificado antes en la sesión.
- Verificado por Guido: atajo Info y bajada progresiva al mantener apretado.
- `.local-privado/` (capturas del inicio con la foto de perfil) solo en la PC; no hace falta guardarlo.

## En curso
- Nada a medias.

## Próximo paso CONCRETO
Con Guido frente a la TV: probar de oído el volumen en Plex, Disney+, Prime y HBO Max, y un reinicio programado real (2-3 minutos adelante, TV apagada con el control). Detalle en `docs/TODO.md`.

## Bloqueos
- Ninguno.

## Archivos tocados
- Todo el repo (sesión fundacional).

## Contexto que no está en otro doc
- Guido usa Ambient con fondos propios y lo quiere así; el Game Bar y Alexa no se tocan.
- Guido prefiere no guardar nada solo en la PC: todo lo del proyecto va al repo.
- La app tiene su propia clave ADB autorizada en la TV ("volumen-maestro@tv"); la PC tiene la suya.
- La interfaz de la app se puede manejar desde la PC con `input tap x y` (más seguro que las flechas).
- El respaldo `/sdcard/VolumenMaestro/ajustes.json` queda en versión 1 hasta el próximo cambio de ajustes; la app lo migra al leerlo.
