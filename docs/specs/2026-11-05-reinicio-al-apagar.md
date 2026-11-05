# SPEC: Reinicio al apagar — Volumen Maestro
El reinicio programado deja de ser una alarma a hora fija: en los días elegidos, la primera vez que Guido apaga la TV después de cierta hora de la noche, se reinicia y queda apagada.
- Estado: COMPLETO 2026-11-05 (sin la espera de 1 minuto ni su criterio 4: TCL duerme la TV a ~1,3 s, ver ADR-016)
- Fecha: 2026-11-05
- Sello del Arquitecto: `arquitecto: 2026-11-05 · kit 2.4.3 · chequeos: tamaño ✅ · entorno — (sin ficha del kit) · tope ✅ · hash 1f0a66d59548`

## Pedido original, textual
> "¿Qué podríamos hacer con eso? ¿Hacer reinicios programados? ¿Cómo que la app te pregunte cada tanto? ¿Una vez por semana si nota que está prendida, que te pregunte?"
> "lo de reiniciar o apagar, está bien, pero no cada vez que apago. Porque a mí me gusta tener la tele así. Entonces, que sea reiniciar o apagar, pero una o dos veces por semana como mucho. Y que yo pueda elegir los días. [...] O, digamos, sábado, domingo y lunes. Si la apago después de la X hora de la noche, que se reinicie. [...] yo durante el día uso mucho apagarla y prenderla y que ya esté funcionando. Que tenga las cosas abiertas y demás."
> "dejemos ya por default que sean los días domingo, lunes y jueves, ponele. Tres días de la semana reset [...] Después de las 22 horas. Todo esto me imagino que va a ser opciones en la app, que va a reemplazar lo anterior."

## Por qué (el dolor)
El 05/11 la TV pasó a reposo Doze (ADR-015) para que la tira LED del USB no titile. En Doze nada despierta a la TV apagada, así que la alarma del reinicio programado (ADR-009) suena recién cuando Guido la prende, con la TV en uso, y el reinicio no pasa. Además, la pestaña Inicio muestra un aviso amarillo pidiendo Network Standby.

## Contexto del código (de la exploración — nombres REALES, con archivo:línea)
- Config: `rebootEnabled`, `rebootDays` (lunes), `rebootHour` 5, `rebootMinute` 0 (`settings/Config.kt:26-29`); versión del JSON 2 (`Config.kt:53`).
- Alarma exacta y reintentos cada 10 min hasta 3 h: `system/RebootScheduler.kt`, `system/RebootReceiver.kt`, `Engine.onRebootAlarm` (`engine/Engine.kt:425`); permiso `SCHEDULE_EXACT_ALARM` (`AndroidManifest.xml:7`, `Engine.kt:125`).
- Marca de reinicio y "si la pantalla se prendió sola, `KEYCODE_SLEEP`": `Engine.handleRebootMarker` (`Engine.kt:454`).
- El motor ya escucha `SCREEN_ON` (`Engine.kt:110`); no escucha `SCREEN_OFF`.
- Chequeo "Reposo con red" en amarillo si `tcl_sys_network_standby_enabled` ≠ 1 (`Engine.kt:370`).
- Pantalla: sección "Reinicio programado" de la pestaña Optimizar (`ui/OptimizeScreen.kt:135`), con frecuencia, días, hora y minutos.
- En la TV, al apagar, TCL deja unos 3 min de margen antes de dormir (`TclPowerManagerServiceEx: Forced release wakelock after 180000 ms`, registro del 05/11).

## AGREGA (lo nuevo)
- El motor escucha `SCREEN_OFF`. Si corresponde reiniciar, espera 1 minuto con la pantalla apagada (wakelock propio) y reinicia; si la pantalla se prende antes, cancela.
- "La noche" de un día elegido va desde la hora elegida hasta las 06:00 siguientes (apagar el domingo a la 1 cuenta como domingo).
- Ajuste nuevo **"Cuántas veces"**: "Una por cada día elegido" (por defecto) o "Una por semana" (la primera noche elegida en que se apague tarde; las otras de esa semana no).
- Se recuerda la última noche reiniciada, para no reiniciar dos veces la misma noche.
- Al arrancar la versión nueva se cancelan las alarmas que dejó la versión anterior.

## MODIFICA (lo existente que se toca)
- Valores por defecto: **domingo, lunes y jueves, desde las 22:00**. La config vieja (versión 2) pasa a versión 3 con esos días y hora; se conserva si el reinicio estaba prendido o no. Efecto a cuidar: el resto de la config (tema, curva, atajo) no cambia.
- Se quitan la alarma, los reintentos, `RebootReceiver` y el permiso `SCHEDULE_EXACT_ALARM`. Efecto a cuidar: "Reiniciar ahora" sigue andando.
- Sección de la pestaña Optimizar: interruptor, días, "Desde las" (hora y minutos), "Cuántas veces", y un texto con la regla y la próxima noche. Se quitan los botones de frecuencia. Tiene que verse bien en la TV (1920×1080) sin cortarse.
- Chequeo de reposo: pasa a informativo (siempre verde), dice si la TV duerme con red o sin red y qué implica. Ya no pide Network Standby.
- Texto de "Desinstalar limpio": se saca "cancela el reinicio programado".

## NO SE TOCA (obligatoria — el seguro de no romper)
- El volumen maestro, la curva, el ecualizador de la mezcla y los modos ENCENDIDO/APAGADO.
- Los carteles y temas, el atajo del control y el panel de apps (TCL Home).
- "Reiniciar ahora" y "Optimizar ahora".
- La marca de reinicio y el `KEYCODE_SLEEP` al arrancar si la pantalla se prendió sola.
- La firma, el `applicationId` y la clase `VolumeService` (ADR-008).
- El modo de reposo de la TV: la app no lo cambia.

## Criterios de aceptación (3-8, verificables)
1. Todo lo de NO SE TOCA sigue funcionando: motor andando, volumen y cartel de volumen en la TV.
2. CUANDO Guido apaga la TV en un día elegido después de la hora elegida y no hubo reinicio esa noche, la TV DEBE reiniciarse en ~1 min y quedar apagada (probado en la TV en Doze).
3. CUANDO la apaga antes de la hora elegida, o en un día no elegido, la TV NO DEBE reiniciarse.
4. SI la prende de nuevo dentro del minuto, ENTONCES NO DEBE reiniciarse.
5. CUANDO ya se reinició esa noche, apagarla otra vez esa noche NO DEBE reiniciarla (con "Una por semana", tampoco en otra noche elegida de los 6 días siguientes).
6. La config vieja (lunes 05:00) DEBE pasar a domingo, lunes y jueves desde las 22:00 al instalar.
7. La sección de la pestaña Optimizar DEBE verse completa y legible en la TV, y el chequeo de reposo NO DEBE quedar en amarillo en Doze.
8. Las pruebas de la regla de la noche (cruce de medianoche, días, una por semana) DEBEN pasar en `testDebugUnitTest`.

## Ejecución
Tamaño: módulo solo
- Sesiones: una (esta misma, con el OK de Guido del 05/11).
- Para abrir: inicio — ejecutá docs/specs/2026-11-05-reinicio-al-apagar.md (está READY).
- Cómo se prueba: ver "Entorno de prueba".

## Entorno de prueba
- Cómo se levanta: `/instalar` (compila, instala en la TV `192.168.0.219:5555` y verifica el motor).
- Datos semilla: la config de la TV. Para probar, poner "Desde las" un rato antes de la hora actual y el día de hoy elegido; al terminar, volver a domingo, lunes y jueves 22:00.
- Chequeo base: `.\gradlew.bat assembleRelease testDebugUnitTest` en verde antes de tocar código.
- Herramientas: adb (`adb devices` dice `device`); apagar con `input keyevent KEYCODE_POWER`; ver el reinicio con `uptime` y el registro `logcat -s VolumenMaestro`; capturas con `screencap` (borrarlas al terminar).

## Supuestos
- [ALTO] Con la pantalla recién apagada, el wakelock propio del motor alcanza para esperar 1 minuto antes de que TCL duerma la TV (TCL da ~3 min). Si no alcanzara, el reinicio pasaría en la próxima despertada. Se verifica en la prueba del criterio 2.
- [BAJO] El salvapantallas (Ambient) no cuenta como apagar: no manda `SCREEN_OFF`.
- [BAJO] Corte de "la noche" fijo a las 06:00, sin ajuste.

## Riesgos y decisiones ⚠️
- ⚠️ Se elimina el reinicio a hora fija. Consecuencia: si algún día se vuelve a Network Standby y se lo quiere a hora fija, hay que volver a programarlo (queda en git, v2.3).
- ⚠️ Migración de la config a versión 3 que pisa días y hora. Consecuencia: se pierde el horario viejo (lunes 05:00); es lo que Guido pidió.
- ⚠️ Si Guido nunca apaga la TV tarde en los días elegidos, esa semana no hay reinicio. Consecuencia aceptada: no se despierta la TV ni se pregunta nada.

### Alternativas evaluadas y descartadas
- Alarma a hora fija (lo de hoy) — descartada: en Doze nada despierta la TV para que suene.
- Que la app pregunte una vez por semana con un cartel — descartada: aparece mientras se mira algo y la respuesta es siempre "sí".
- Reiniciar cada vez que se apaga — descartada por Guido: durante el día la apaga y la prende seguido y quiere sus apps abiertas.
