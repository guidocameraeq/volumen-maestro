# CHANGELOG — Volumen Maestro

> Historia permanente. `/cierre` agrega una entrada AL TOPE en cada sesión. Orden descendente estricto, sin excepciones. Nada de versiones duplicadas en otros docs.

## 2026-11-01 — Volumen Maestro 2.3: el botón TCL Home hace de "Alt-Tab"

### Added
- Panel de apps abiertas: un toque en TCL Home abre una franja con las apps recientes (vista previa o banner), OK abre, mantener OK cierra, "Limpiar todo" cierra todas menos Alexa, Ambient, Game Bar y la app; dos toques vuelven a la app anterior. Paquete `recientes/`, doc `docs/recientes.md`, spec COMPLETO. Interruptor en Ajustes.

### Fixed
- Al instalar o quitar una app, el lector del control quedaba duplicado y cada toque llegaba dos veces (también afectaba al atajo de Info).

### Removed
- En la TV: `com.tcl.dashboard` (el panel de cuenta TCL que abría TCL Home), reversible y anotado en `apps-quitadas.txt`.

## 2026-11-01 — Volumen Maestro 2.2: el mudo de Disney+ y los temas de Noche de brujas

### Fixed
- Con la app encendida, todo quedaba mudo después de prender la TV (se notó en Disney+): los efectos de sonido de TCL hacían que Android redondeara el volumen maestro a 0 o 1. Un ecualizador plano en la mezcla se hace cargo del volumen (`audio/MixVolume.kt`, ADR-013).
- Retraso del volumen al mantener apretado: la transición pasa a "Apagada" de fábrica.
- Pestaña Temas: al bajar desde las pestañas, el foco va al tema elegido en vez de correr la columna de la derecha.

### Added
- 3 temas de "Noche de brujas": Colina espiral, Candelabro gótico, Hilo de seda (mockups en `docs/mockups/temas-spooky.html`).
- La pestaña Temas en 4 grupos: Sobrios, Fantasía, Gamer y tecno, Noche de brujas (`ThemeRegistry.groups`).
- Spec del panel de apps abiertas con el botón TCL Home (`docs/specs/2026-11-01-panel-apps.md`, en BORRADOR sellado) y ADR-014.
- Bitácora de la TV del día (`docs/tv-guido-tcl/sesiones/2026-11-01.md`).

## 2026-10-31 — Cierre: repo autosuficiente

### Changed
- `keystore.jks` versionado en el repo público, para compilar e instalar como actualización desde cualquier PC (ADR-012).
- El proyecto pasó a `C:\Users\Usuario\Desktop\Proyectos\volumen-maestro`.

### Added
- `.claude/settings.json`: hook SessionStart (inyecta el handoff en cada chat nuevo) y permisos del proyecto.

## 2026-10-31 — Volumen Maestro 2.1: vuelve la bajada progresiva

### Fixed
- Mantener apretado el volumen volvió a acelerar: el servicio de accesibilidad ya no filtra teclas (con el filtro, Android no repetía las teclas mantenidas).

### Changed
- El atajo del control se escucha con `getevent` (señales crudas del control) y se guarda como código del kernel; la config pasa a versión 2 y migra Info solo (ADR-011).
- Pestaña Volumen: sección "Mantener apretado"; Ajustes: aviso de que el botón del atajo igual hace su función.

## 2026-10-31 — De la TV lenta a Volumen Maestro 2.0

### Added
- Volumen Maestro 2.0: Compose for TV con 5 pestañas (Inicio, Volumen, Temas, Optimizar, Ajustes), motor en proceso `:motor`, interruptor ENCENDIDO/APAGADO, curva configurable (Pareja/Suave/Ampli), transiciones suaves, semáforo de chequeos con "Reparar todo", Optimizar ahora, reinicio programado por días y hora, atajo del control, copia de seguridad automática y desinstalación limpia.
- 12 temas para los carteles (Sable láser rojo, Neón, Medieval, Retro, Vidrio, Arcade, Espacial, Ondas, Steampunk, Acuático, Terminal, Clásico), con vista previa animada.
- 16 pruebas automáticas (curvas, próximo reinicio, JSON de la config).
- Documentación: arquitectura, decisiones (ADR 001-010), investigación del audio y Dolby, guía de temas, desarrollo, historia completa de la TV y CLAUDE.md con las skills `/inicio`, `/cierre` e `/instalar`.
- Landing en `docs/index.html` y mockups interactivos en `docs/mockups/temas.html`.

### Changed
- La TV: 17 apps de fábrica quitadas (reversible), reposo en Network Standby (Alexa la prende), salida de audio en Optical, ajustes de audio de Netflix según el modo.

### Removed
- Volumen Maestro 1.0 (Java sin Gradle): reemplazada por la 2.0 con la misma firma.
