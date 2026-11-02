# SESSION_HANDOFF — Volumen Maestro

> Save game del proyecto. `/cierre` lo SOBREESCRIBE ENTERO en cada sesión — acá nunca se apila historia (eso vive en CHANGELOG). El hook SessionStart lo inyecta en cada chat nuevo: **tope de 5.500 bytes** (unas 70 líneas); si se pasa, el hook lo corta y avisa "CORTADO".

**Cierre**: 2026-11-01 (noche) · **Último commit**: ver `git log -1` (cierre de sesión)

## Qué se hizo (sesión del 01/11 a la noche: el protector de pantalla)
- Guido quiere ver sus obras de arte mejor que con el Ambient de Google. Se investigó: los menús de Google TV solo ofrecen el Ambient, pero Android acepta otro protector.
- **Probado en la TV**: se puso el protector de colores de Android, arrancó (`dumpsys dreams` + captura) y se volvió al Ambient. Detalle y comandos en `docs/tv-guido-tcl/README.md` (punto 9).
- Se diseñó con Guido **Museum Slideshow**, una galería de arte que también es protector, y se montó su **repo privado aparte** (`../museum-slideshow`, GitHub `guidocameraeq/museum-slideshow`, SPEC-0 READY). Este repo queda como "la casa de la tele".
- Bitácora: `docs/tv-guido-tcl/sesiones/2026-11-01.md` (punto 12).

## Estado
- TV: Volumen Maestro 2.3 ENCENDIDA (tema Colina espiral, transición apagada, panel de apps prendido). Protector = Ambient de Google, sin cambios (verificado al terminar la prueba).
- Red: TV en el Wi-Fi "Yulius" (`192.168.0.219`); la PC a veces queda en "Domotics3" (ver `CLAUDE.md`).
- Repo limpio, todo subido.

## En curso
- Nada a medias acá. Museum Slideshow sigue en su repo.

## Próximo paso CONCRETO
Cuando se vuelva a Volumen Maestro: que Guido pruebe los temas de Noche de brujas en pantalla (con silencio) y mirar si la fila de Prime del inicio se fue (ver `docs/TODO.md`). Lo de la galería se trabaja en un chat abierto en `../museum-slideshow`.

## Bloqueos
- Ninguno.

## Contexto que no está en otro doc
- Bajar el volumen maestro por debajo de 0,5 con la app apagada (sin el ecualizador) deja la TV muda y puede tumbar el HAL de audio. Para probar el volumen, bajar por debajo de 0,5 (con el redondeo, 0,94 suena igual que 1,0).
- El panel de apps solo se abre con el control real (la shell no puede simular TCL Home): pedirle a Guido que lo deje abierto y `screencap`.
- Navegar menús de la TV desde la PC anduvo bien leyendo el foco con `uiautomator dump` (más seguro que las capturas): bajar, leer qué tiene el foco, recién ahí OK.
- Las capturas de la pantalla de inicio muestran la foto de perfil de Guido: borrarlas al terminar.
- Guido: la auto-actualización desde GitHub quedó descartada por ahora; los avisos desde el celu, descartados.
- Guido quiere que toda decisión que pueda ser opción sea un ajuste con valor por defecto (vale también para Volumen Maestro).
