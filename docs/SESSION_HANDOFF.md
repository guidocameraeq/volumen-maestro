# SESSION_HANDOFF — Volumen Maestro

> Save game del proyecto. `/cierre` lo SOBREESCRIBE ENTERO en cada sesión — acá nunca se apila historia (eso vive en CHANGELOG). El hook SessionStart lo inyecta en cada chat nuevo: **tope de 5.500 bytes** (unas 70 líneas); si se pasa, el hook lo corta y avisa "CORTADO".

**Cierre**: 2026-11-01 (noche) · **Último commit**: ver `git log -1` (cierre de sesión)

## Qué se hizo (sesión del 01/11 a la noche: CleanYourTV)
- Se investigó **CleanYourTV** (`github.com/Gathin23/cleanyourtv`, el proyecto de moda para limpiar TVs Android). Tiene una lista para este mismo modelo (G09, firmware AS24).
- Se cruzaron **todas sus listas de TCL** contra lo prendido en la TV: la mayoría de la del G09 ya estaba quitada. **Se quitaron las 5 que faltaban** (TIAS, instalador silencioso de TCL, instalaciones de socios, términos de TCL, actualizador de barras de sonido), con `pm uninstall -k --user 0`, de a una.
- Verificado después: motor, Alexa, Wi-Fi con internet, Ajustes y el inicio andando, sin errores en el registro. **No se reinició la TV.**
- Lo demás de sus listas son cosas elegidas por Guido o "nunca tocar". Ideas y datos que sirven, anotados en `docs/ideas-google-tv.md` (línea de CleanYourTV).
- Guido aclaró: **nada de protectores ni pantallas nuevas en este repo** (eso es Museum Slideshow, con otro dev) y lo que se decidió no apagar no se apaga.
- Bitácora: `docs/tv-guido-tcl/sesiones/2026-11-01.md` (punto 13).

## Estado
- TV: Volumen Maestro 2.3 ENCENDIDA (tema Colina espiral, transición apagada, panel de apps prendido). Protector = Ambient de Google. 32 apps de fábrica quitadas (`apps-quitadas.txt`).
- Red: TV en el Wi-Fi "Yulius" (`192.168.0.219`); la PC a veces queda en "Domotics3" (ver `CLAUDE.md`).
- Repo limpio, todo subido.

## En curso
- Nada a medias.

## Próximo paso CONCRETO
La próxima vez que la TV se reinicie (o reiniciarla al empezar la sesión): confirmar que arranca bien sin las 5 apps nuevas (motor con `pidof local.volumenmaestro:motor`, Alexa, Wi-Fi). Después: que Guido pruebe los temas de Noche de brujas en pantalla y mirar si la fila de Prime se fue (ver `docs/TODO.md`).

## Bloqueos
- Ninguno.

## Contexto que no está en otro doc
- Bajar el volumen maestro por debajo de 0,5 con la app apagada (sin el ecualizador) deja la TV muda y puede tumbar el HAL de audio. Para probar el volumen, bajar por debajo de 0,5 (con el redondeo, 0,94 suena igual que 1,0).
- El panel de apps solo se abre con el control real (la shell no puede simular TCL Home): pedirle a Guido que lo deje abierto y `screencap`.
- Navegar menús de la TV desde la PC anduvo bien leyendo el foco con `uiautomator dump`: bajar, leer qué tiene el foco, recién ahí OK.
- Las capturas de la pantalla de inicio muestran la foto de perfil de Guido: borrarlas al terminar.
- `am start` de Ajustes no se abre mientras corre el Ambient: primero `input keyevent HOME`.
- La TV tiene un segundo perfil (usuario 10, "new_user") sin usar; no gasta nada mientras no se abra. No se tocó.
- Guido: la auto-actualización desde GitHub quedó descartada por ahora; los avisos desde el celu, descartados.
- Guido quiere que toda decisión que pueda ser opción sea un ajuste con valor por defecto (vale también para Volumen Maestro).
