# SESSION_HANDOFF — Volumen Maestro

> Save game del proyecto. `/cierre` lo SOBREESCRIBE ENTERO en cada sesión — acá nunca se apila historia (eso vive en CHANGELOG). El hook SessionStart lo inyecta en cada chat nuevo: **tope de 5.500 bytes** (unas 70 líneas); si se pasa, el hook lo corta y avisa "CORTADO".

**Cierre**: 2026-10-31 · **Último commit**: ver `git log -1` (primer commit del repo)

## Qué se hizo
- Diagnóstico y limpieza de la TV TCL de Guido: caché, 17 apps de fábrica quitadas (reversible), Alexa arreglada con Network Standby. Historia en `docs/tv-guido-tcl/`.
- Investigación del audio por fibra: el volumen maestro de AudioFlinger funciona con todo lo PCM (también Netflix en túnel); con Dolby no hay forma. Ver `docs/audio-dolby.md`.
- Volumen Maestro 1.0 (Java) y 2.0 (Compose for TV, motor en `:motor`, 5 pestañas, 12 temas, curva, transiciones, reinicio programado, optimizar, atajo, copia, desinstalar limpio). Spec en `docs/specs/` (COMPLETO).
- Los 11 temas de los mockups pasados a Kotlin con 3 subagentes y verificados en la TV con capturas.
- Repo: CLAUDE.md, `/inicio`, `/cierre`, `/instalar`, docs (arquitectura, decisiones, audio, temas, desarrollo), README con banner y landing en `docs/index.html`.

## Estado
- TV: Volumen Maestro 2.0 instalada y ENCENDIDA, tema Sable láser, curva Pareja 0,5 dB. Audio Output Optical, Digital Audio Out PCM, Netflix sin Dolby.
- Repo: `main`, publicado en GitHub (guidocameraeq/volumen-maestro), Pages desde `/docs`.
- `keystore.jks` y `.local-privado/` solo en la PC (no versionados).

## En curso
- Nada a medias en el código.

## Próximo paso CONCRETO
Activar el hook: copiar `docs/claude-settings.json` a `.claude/settings.json` (Claude no pudo crearlo). Después, con Guido frente a la TV: probar el atajo (Ajustes → Elegir botón → CH o Info) y el volumen en Plex/Disney+/Prime/HBO.

## Bloqueos
- Ninguno.

## Archivos tocados
- Todo el repo es nuevo (primer commit).

## Contexto que no está en otro doc
- Guido usa Ambient con fondos propios y lo quiere así; el Game Bar y Alexa no se tocan.
- La app tiene su propia clave ADB autorizada en la TV ("volumen-maestro@tv"); la PC tiene la suya. Si se reinstala con otra firma, hay que volver a aceptar ambas cosas en la TV.
- La interfaz de la app se puede manejar desde la PC con `input tap x y` (más seguro que las flechas).
