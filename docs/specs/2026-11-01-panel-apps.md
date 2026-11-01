# SPEC: Panel de apps abiertas (TCL Home) — Volumen Maestro
El botón TCL Home del control pasa a ser "Alt-Tab": un toque abre un panel chico con las últimas apps, dos toques vuelven a la app anterior.
- Estado: BORRADOR
- Fecha: 2026-11-01
- Sello del Arquitecto: `arquitecto: 2026-11-01 · kit 2.4.3 · chequeos: tamaño ✅ · entorno — (sin ficha del kit) · tope ✅ · hash 06fd6e28170a`

## Pedido original, textual
> "a veces me pasa que yo salgo por error de la app ... ¿Hay algún botón o algún atajo que haga como Alt-Tab y vuelva a donde estabas?"
> "Puede ser un doble tap en el botón que se llama TCL Home ... que no sé ni para qué funciona ese botón. Hasta te diría que lo podríamos sacar a la mierda porque lo odio."
> "en Android hay como el panel de apps abiertas. Me gustaría hacer doble toque para la app anterior y un toque para abrir ese panel."
> "hace un plan completo que sea minimalista, lindo, que no ocupe mucho. Si puede tener un preview de qué se estaba viendo, estaría buenísimo. Y que tenga un botoncito de limpiar todo."
> "metela adentro, pero que tenga un buen separation of concerns ... que son dos cosas ahí adentro, bien documentado."

## Por qué (el dolor)
En la TV es común salir de una app sin querer (Inicio, Atrás de más) y volver obliga a buscarla de nuevo en la pantalla de inicio. Google TV sacó la pantalla de "apps recientes" de Android TV y el control no tiene un botón para eso. El botón TCL Home no le sirve de nada a Guido.

## Contexto del código (verificado en la TV y en el repo el 2026-11-01)
- TCL Home manda `KEY_DASHBOARD` (código del kernel `0xCC`, escaneo `000c003a`, en `/dev/input/event9`); la TV lo mapea a `TCL_DASHBOARD` y abre `com.tcl.dashboard/.MainActivity`. Cada toque abre o cierra ese panel.
- El motor ya lee el control crudo con `getevent` (`engine/Engine.kt:486` `startEventReader`, `:514` `parseEvent`, `:523` `onRawKey`); las teclas no se pueden consumir (ADR-011).
- El motor corre comandos de shell por la ADB local (`engine/Shell.kt`, `Engine.shell.commands.run`).
- La lista de apps abiertas existe: `dumpsys activity recents` da las tareas en orden (paquete, `type=home` / `standard`).
- No hay pantalla de recientes en la TV: ninguna actividad del sistema la ofrece.
- La interfaz (Compose) corre en el proceso principal (`ui/MainActivity.kt`); el motor en `:motor` (`AndroidManifest.xml:39`) y le habla a la interfaz por `engine/Protocol.kt`.
- `com.tcl.partnercustomizer` maneja el ícono "TCL Home" del inicio y se dejó a propósito (`docs/tv-guido-tcl/README.md:55`).

## AGREGA (lo nuevo)
- Paquete `local.volumenmaestro.recientes`, la segunda función de la app, separada del audio:
  - `RecentApps` (motor): lee y filtra la lista de apps abiertas (sin inicio, sin Volumen Maestro, sin ajustes ni pantallas del sistema).
  - `AppSwitcher` (motor): trae al frente una app (vuelve a su tarea si sigue abierta) y cierra apps (`am force-stop`), nunca las protegidas.
  - `Snapshots` (motor): guarda una captura achicada de la app de adelante para la vista previa (ver Riesgos).
  - `TapDetector`: decide un toque o dos toques de TCL Home (ventana de 400 ms).
  - `RecentsActivity` (interfaz): el panel.
- El panel: una franja baja y translúcida en la parte inferior, con hasta 6 tarjetas (vista previa 16:9 + nombre), foco inicial en la app anterior, OK abre, Atrás cierra sin cambiar nada, y una píldora "Limpiar todo" al final. Mantener OK sobre una tarjeta cierra solo esa app.
- Ajustes: interruptor "TCL Home: panel de apps", prendido de fábrica.
- Apps protegidas (nunca se cierran): `com.tcl.smartalexa`, `com.tcl.ttvs`, `com.google.android.apps.tv.dreamx` (Ambient), `com.tcl.gamebar`, el inicio de Google TV y Volumen Maestro.
- Docs: `docs/recientes.md` (cómo funciona), ADR-014 (por qué va adentro de la app), una línea en `docs/arquitectura.md`.

## MODIFICA (lo existente que se toca)
- `Engine.onRawKey`: además del atajo de Info, le pasa `0xCC` al `TapDetector`. Efecto a cuidar: el atajo de Info, el modo "aprender botón" y la lista de teclas prohibidas siguen igual.
- `AndroidManifest.xml`: se agrega `RecentsActivity` (proceso principal, tema translúcido). Efecto a cuidar: la pantalla de la app y el servicio de accesibilidad no cambian de nombre ni de proceso.
- `Protocol`: un mensaje para pedirle al motor la lista, abrir, cerrar y limpiar. Efecto a cuidar: los mensajes actuales siguen iguales.
- En la TV, con OK de Guido: se quita `com.tcl.dashboard` con `pm uninstall -k --user 0` y se anota en `docs/tv-guido-tcl/apps-quitadas.txt`. Efecto: TCL Home ya no abre el panel de TCL (sus opciones siguen en Ajustes de la TV).

## NO SE TOCA
- El volumen maestro, la curva, el ecualizador de la mezcla (ADR-013) y los carteles de volumen.
- El atajo de Info (mantener 2 s) y el lector `getevent` sin filtro de teclas (ADR-011).
- `applicationId`, `VolumeService`, `keystore.jks`, el nombre de los procesos.
- El orden de apps de Guido, Ambient, Game Bar, Alexa y `com.tcl.partnercustomizer`.
- El reinicio programado y la pestaña Optimizar.

## Criterios de aceptación
- Todo lo listado en NO SE TOCA sigue funcionando igual (volumen con una app abierta, atajo de Info, un tema en pantalla).
- CUANDO Guido toca dos veces TCL Home (menos de 400 ms entre toques) dentro de una app B, habiendo usado antes una app A, la TV DEBE mostrar A en menos de 2 s, en el punto donde estaba si A seguía abierta.
- CUANDO toca dos veces TCL Home en la pantalla de inicio, la TV DEBE abrir la última app que se usó.
- CUANDO toca una vez TCL Home, el sistema DEBE mostrar el panel en menos de 1,5 s, con la app anterior enfocada; Atrás DEBE cerrarlo y dejar la pantalla como estaba.
- Cada tarjeta DEBE mostrar una captura de la app si hay una que no salió negra; si no hay, DEBE mostrar el banner y el nombre de la app.
- CUANDO elige "Limpiar todo", el sistema DEBE cerrar todas las apps de la lista menos las protegidas (comprobado con `pidof`: sin proceso las cerradas, con proceso Alexa y Ambient) y el panel DEBE quedar vacío.
- SI no hay apps abiertas, ENTONCES el panel DEBE decir "No hay apps abiertas" y dos toques no DEBEN hacer nada.

## Ejecución
Tamaño: módulo solo
- Sesiones: una.
- Para abrir: `inicio — ejecutá el spec docs/specs/2026-11-01-panel-apps.md (está READY)` en un chat nuevo en la raíz del proyecto.
- Orden: 1) `RecentApps` + `AppSwitcher` + `TapDetector` con pruebas unitarias (lectura de `dumpsys`, ventana de toques); 2) el doble toque andando en la TV; 3) el panel y "Limpiar todo"; 4) las capturas; 5) quitar `com.tcl.dashboard` (con OK de Guido, mostrándole antes el panel de TCL desde la pantalla de inicio); 6) docs.
- Cómo se prueba: ver "Entorno de prueba".

## Entorno de prueba
- Cómo se levanta: `/instalar` (compila, instala en la TV `192.168.0.219:5555` y verifica el motor).
- Datos semilla: no hay; las apps abiertas son las de Guido. Para armar una lista: abrir YouTube, Plex y Ajustes con `am start` desde la PC.
- Chequeo base: `.\gradlew.bat assembleRelease testDebugUnitTest` en verde y `adb -s 192.168.0.219:5555 shell pidof local.volumenmaestro:motor` devuelve un número.
- Herramientas: adb (Platform-Tools, ruta en `CLAUDE.md`), JDK 17. El botón TCL Home solo se prueba con el control real: **necesita a Guido frente a la TV**. Las capturas se miran con `screencap` desde la pantalla de inicio (sobre Netflix o Disney+ salen negras).

## Supuestos
- [ALTO] [SIN CONTRASTAR] Volver a abrir una app con su intent de inicio (`am start` con `NEW_TASK`) la trae al frente donde estaba si su tarea sigue viva. (Si no, hay que usar la tarea por número; si tampoco, el doble toque abre la app de cero.)
- [ALTO] [SIN CONTRASTAR] La shell puede guardar una captura en la carpeta de la app en `/sdcard/Android/data/local.volumenmaestro/` y la app la puede leer.
- [BAJO] 6 tarjetas alcanzan; 400 ms es buena ventana para el doble toque (se ajusta con Guido).
- [BAJO] Quitar `com.tcl.dashboard` no rompe otra cosa de TCL (se revisa en la TV antes y después).

## Riesgos y decisiones ⚠️
- ⚠️ Va adentro de Volumen Maestro, en su propio paquete y documento (ADR-014). Consecuencia: la app deja de ser solo de audio; separarla después obligaría a una segunda autorización ADB y a duplicar el lector del control.
- ⚠️ Vista previa: una captura cada 60 s de la app de adelante, solo con la pantalla prendida, achicada y guardada en la carpeta privada de la app, más una al apretar TCL Home. Consecuencia: un poco de trabajo extra para la TV (se mide; si molesta, se queda solo con el banner); con Netflix, Disney+ y otras apps con protección la captura sale negra y se muestra el banner.
- ⚠️ Las capturas pueden mostrar lo que Guido estaba viendo: no salen de la TV, no van al repo y "Limpiar todo" las borra. Consecuencia: ninguna, mientras se cumpla.
- ⚠️ Un toque tarda unos 400 ms en abrir el panel (hay que esperar para saber si viene un segundo toque). Consecuencia: el panel no es instantáneo; el doble toque sí.
- ⚠️ Sin quitar `com.tcl.dashboard`, un toque abre los dos paneles. Consecuencia: si Guido no da el OK, el toque simple queda sin uso y solo vale el doble toque.

### Alternativas evaluadas y descartadas
- App aparte en el mismo repo — descartada: otra autorización ADB, otro servicio de accesibilidad y otro lector del control, que pueden fallar por separado.
- Repo aparte — descartado: el mismo costo y además otro proyecto que mantener.
- Doble toque solo, sin panel — descartado por Guido: quiere el panel de apps abiertas.
- Capturas del sistema (`/data/system_ce/.../snapshots`) — descartadas: la shell no puede leerlas.
