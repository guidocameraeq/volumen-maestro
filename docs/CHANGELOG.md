# CHANGELOG — Volumen Maestro

> Historia permanente. `/cierre` agrega una entrada AL TOPE en cada sesión. Orden descendente estricto, sin excepciones. Nada de versiones duplicadas en otros docs.

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
