# SESSION_HANDOFF — Volumen Maestro

> Save game del proyecto. `/cierre` lo SOBREESCRIBE ENTERO en cada sesión — acá nunca se apila historia (eso vive en CHANGELOG). El hook SessionStart lo inyecta en cada chat nuevo: **tope de 5.500 bytes** (unas 70 líneas); si se pasa, el hook lo corta y avisa "CORTADO".

**Cierre**: 2026-11-05 (tarde) · **Último commit**: ver `git log -1` (cierre de sesión)

## Qué se hizo (sesión del 05/11: la tira LED y el reinicio al apagar)
- La tira LED del USB titilaba con la TV apagada. Causa medida: en Network Standby el Wi-Fi despierta a la TV con el tráfico de la red. **La TV pasó a Doze**: la tira queda apagada, pero Alexa ya no prende la TV (ADR-015).
- **Volumen Maestro 2.4 publicada e instalada**: reinicio al apagar de noche en vez de alarma a hora fija (ADR-016, spec COMPLETO). Arreglos en las pestañas Optimizar e Inicio.
- Se descartó la espera de 1 minuto: TCL duerme la TV a ~1,3 s de apretar el botón. Reinicia en el momento.
- Bitácora con las pruebas: `docs/tv-guido-tcl/sesiones/2026-11-05.md`.

## Estado
- TV: Volumen Maestro 2.4 ENCENDIDA (tema Colina espiral). Reposo = Doze. Reinicio al apagar prendido: domingo, lunes y jueves desde las 22:00. La noche del jueves 05/11 quedó marcada por la prueba: el próximo es el domingo 08/11.
- Red: TV en el Wi-Fi "Yulius" (`192.168.0.219`); la PC a veces queda en "Domotics3" (ver `CLAUDE.md`).
- Repo limpio, todo subido.

## En curso
- Nada a medias.

## Próximo paso CONCRETO
El lunes 09/11 o después: confirmar el reinicio al apagar en Doze (primer punto de `docs/TODO.md`), que es lo único sin probar. Después: Guido prueba los temas de Noche de brujas en pantalla.

## Bloqueos
- Ninguno.

## Contexto que no está en otro doc
- Bajar el volumen maestro por debajo de 0,5 con la app apagada (sin el ecualizador) deja la TV muda y puede tumbar el HAL de audio. Para probar el volumen, bajar por debajo de 0,5 (con el redondeo, 0,94 suena igual que 1,0).
- El panel de apps solo se abre con el control real (la shell no puede simular TCL Home): pedirle a Guido que lo deje abierto y `screencap`.
- **En Doze, apagar la TV desde la PC la deja inalcanzable hasta que Guido la prenda con el control.** Antes de apagarla, dejar todo como tiene que quedar.
- Las capturas de la pantalla de inicio muestran la foto de perfil de Guido: borrarlas al terminar.
- Quedan en la TV `/sdcard/s0.png`–`s3.png` y `th.png`, que no son de esta sesión (¿de Museum Slideshow?). No se tocaron.
- `am start` de Ajustes no se abre mientras corre el Ambient: primero `input keyevent HOME`.
- La TV tiene un segundo perfil (usuario 10, "new_user") sin usar; no gasta nada mientras no se abra. No se tocó.
- Guido: la auto-actualización desde GitHub quedó descartada por ahora; los avisos desde el celu, descartados.
- Guido quiere que toda decisión que pueda ser opción sea un ajuste con valor por defecto (vale también para Volumen Maestro).
