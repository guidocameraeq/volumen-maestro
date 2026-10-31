# CLAUDE.md — Reglas para Claude Code en Volumen Maestro

> Solo reglas de comportamiento y cómo llegar a la TV. Arquitectura, decisiones e historia → archivos dedicados (ver mapa de docs).
> Test por línea antes de agregar nada: **si borro esta línea, ¿Claude se equivoca? Si no, sobra.**

## Qué es esto

**Volumen Maestro**: app Android TV que hace que los botones de volumen del control de la TV manejen un equipo de audio conectado por fibra óptica (que TCL deja a volumen fijo), con carteles animados con temas, y además optimiza la TV. Stack: Kotlin + Jetpack Compose for TV, Gradle, sin librerías de terceros para lo crítico. La usa Guido en **su** TCL Google TV (es la única TV donde está probada).

Este repo también guarda **toda la historia de esa TV** (`docs/tv-guido-tcl/`): si la misión es "hacer algo en la TV", empezar por ahí.

**Comandos clave** (desde la raíz, en PowerShell):
- `.\gradlew.bat assembleRelease testDebugUnitTest` — compila el APK y corre las pruebas (necesita `JAVA_HOME` del JDK 17, ver abajo).
- `/instalar` — compila, instala en la TV y verifica que el motor arrancó.
- `adb -s 192.168.0.219:5555 logcat -s VolumenMaestro` — primer paso ante cualquier "no funciona": el registro del motor.

## Conexión a la TV (para retomar cualquier día)

| Qué | Valor |
|---|---|
| TV | TCL "Smart TV Pro" (G09), Google TV, Android TV 12, Amlogic, 2 GB RAM |
| IP / puerto ADB | `192.168.0.219:5555` (red Wi-Fi de la casa; si cambió, buscar el puerto 5555 abierto en la red) |
| adb | Platform-Tools instalado con winget: `%LOCALAPPDATA%\Microsoft\WinGet\Packages\Google.PlatformTools_Microsoft.Winget.Source_8wekyb3d8bbwe\platform-tools\adb.exe` |
| JDK | Temurin 17: `C:\Program Files\Eclipse Adoptium\jdk-17*` → `$env:JAVA_HOME` |
| Android SDK | `%LOCALAPPDATA%\Android\Sdk` (platform 34, build-tools 34.0.0) |
| Firma | `keystore.jks` en la raíz, **no versionado** (alias `vm`, clave `android`). Sin él no se puede actualizar la app instalada sin desinstalarla |

Pasos: `adb connect 192.168.0.219:5555` → `adb devices` debe decir `device`. Si dice `unauthorized`, Guido tiene que aceptar "Allow USB debugging?" en la TV. La depuración USB de la TV **tiene que quedar activada**: la app la usa desde adentro (127.0.0.1:5555).

## El ciclo de trabajo

1. **Chat nuevo por misión.** `/inicio` arranca el ritual (si el hook SessionStart está activo, ya inyectó el handoff). **No toco nada hasta el OK del usuario.**
2. Trabajar con las reglas de abajo operando solas.
3. `/cierre` al terminar → docs al día, commit, push → **el chat se descarta**. Compactar es emergencia (`cierre parcial`), no forma de vida.

Los rituales viven en `.claude/skills/`: **`/inicio` · `/cierre` · `/instalar`**. Ahí está el detalle; no lo duplico acá.

## 🚨 Restricciones duras

1. **Las elecciones de Guido en la TV son la fuente de verdad**: orden de apps (`docs/tv-guido-tcl/respaldo-inicio/orden-apps.txt`), Ambient con sus fondos, Game Bar, Alexa y las apps que bajó él **no se tocan** sin mostrar antes "esto cambio / esto preservo" y esperar su OK.
2. **Nada irreversible en la TV sin OK**: reseteo de fábrica, borrar datos de apps, desinstalar apps del sistema, `pm clear`. Desinstalar apps de fábrica se hace siempre con `pm uninstall -k --user 0` (reversible con `cmd package install-existing`) y se anota en `docs/tv-guido-tcl/apps-quitadas.txt`.
3. **No tocar `tcl_device_out_type`** (salida de audio): TCL la usa para rutear el audio; cambiarla a mano puede prender los parlantes de la TV o cortar la fibra. Se cambia solo desde el menú de la TV.
4. **Firma**: nunca cambiar `keystore.jks`, el `applicationId` (`local.volumenmaestro`) ni el nombre de la clase `local.volumenmaestro.VolumeService`: cualquiera de los tres obliga a reinstalar y re-autorizar todo en la TV.
5. **Secretos**: los registros de la TV (`logcat`) pueden traer tokens de cuentas (Alexa, Netflix). No se copian a archivos persistentes ni al repo; si se guardan para diagnosticar, se borran al terminar.

## Reglas de evidencia (contra los "fixes fantasma")

1. **Un bug se declara resuelto SOLO tras reproducirlo en la TV** y verlo funcionar (captura con `screencap`, registro del motor o valor leído con `service call media.audio_flinger 9`). Lo que depende del oído de Guido se marca **NO VERIFICADO** hasta que lo confirme.
2. Todo resultado se reporta con **evidencia real** o como **NO VERIFICADO**. Nunca "listo" a secas.
3. **En tareas aprobadas no freno a pedir permiso intermedio.** Trabajos >2 min → `run_in_background`.
4. **Post-compactación asumo que NO leí ningún archivo**: Read antes de cualquier Edit.
5. Ante errores: diagnosticar root cause, no aplicar parches encima.
6. **Toda spec se escribe en un formato del anexo** `~/.claude/skills/arquitecto/anexos/formato-spec.md`: **SPEC-0** si es nuevo, **SPEC delta** (con su NO SE TOCA) si toca algo que ya anda. Implementada: su línea de Estado pasa a `COMPLETO <fecha>` y el spec se queda donde está.
7. **Si la charla diseñó una feature no-trivial y todavía no hay spec escrito, OFREZCO cristalizarla en spec antes de construir.**

## Reglas operativas del stack

1. **Teclas desde la PC**: `input keyevent` no sirve para probar el volumen ni el atajo. Para simular volumen usar `cmd media_session volume --stream 3 --adj raise|lower`; el atajo solo se prueba con el control real.
2. **Nunca prender el filtro de teclas de accesibilidad** (`flagRequestFilterKeyEvents`): con él Android deja de repetir las teclas mantenidas y el volumen baja de a un paso. El control se escucha con `getevent` (ADR-011).
3. **Capturas**: `screencap` sale negro sobre apps con DRM (Netflix) y no muestra lo que hay debajo del protector (Ambient). Para probar carteles, usar la pantalla de inicio o la propia app.
4. **Navegar la TV con flechas desde la PC es frágil**: verificar con captura antes de apretar OK (ya se apagó la app sin querer una vez). Para elegir opciones en la app sirve `input tap x y`.
5. **Netflix toma los ajustes de audio solo al arrancar**: después de cambiar `nrdp_*` hay que `am force-stop com.netflix.ninja`.
6. **Temas sin estado**: cada `Theme` dibuja todo desde `OverlayState`, sin allocations ni `BlurMaskFilter` (ver `docs/temas.md`). La placa opaca tapa el cartel de TCL desde el primer cuadro.
7. **Mensajes multi-tema del usuario** (dicta por voz): confirmar el orden de prioridad antes de ejecutar si no dijo "primero X".

## Mapa de documentación

| Archivo | Rol | Quién lo escribe / cuándo |
|---|---|---|
| `docs/SESSION_HANDOFF.md` | **Save game** — foto única de dónde quedamos | `/cierre` lo sobreescribe entero |
| `docs/TODO.md` | ÚNICA fuente de pendientes | `/cierre`: completadas afuera, nuevas adentro |
| `docs/CHANGELOG.md` | Historia de cambios | `/cierre`: entrada por sesión |
| `docs/arquitectura.md` | Cómo está hecha la app (procesos, motor, temas, protocolo) | al cambiar la estructura |
| `docs/decisiones.md` | ADRs: qué se eligió y por qué | al tomar una decisión con alternativas |
| `docs/audio-dolby.md` | La investigación del audio: caminos, Dolby, por qué funciona el volumen maestro | al descubrir algo nuevo del audio |
| `docs/temas.md` | Cómo funciona y cómo se agrega un tema | al cambiar el contrato de temas |
| `docs/desarrollo.md` | Entorno, compilar, instalar, depurar | al cambiar herramientas |
| `docs/tv-guido-tcl/` | Historia de la TV, apps quitadas y cómo restaurarlas, respaldos | cada vez que se toca la TV |
| `docs/specs/` | Specs (la v2 está COMPLETO) | Arquitecto / brainstorming |

**Reglas del sistema**: una sola narrativa por sesión (HANDOFF + CHANGELOG). Un número vive en UN archivo. Lo derivable del código o de git NO se escribe en un doc.

## Comunicación

- Español rioplatense (vos), **en criollo: corto, sin jerga técnica** — Guido no es programador. Aplica a TODO.
- Decisiones: opciones con pros/contras + UNA recomendación justificada.
- Explicar QUÉ cambié, no narrar el proceso.
