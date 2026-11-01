# TODO — Volumen Maestro

> ÚNICA fuente de pendientes del proyecto. Completado → SE BORRA (la historia vive en CHANGELOG y git). Header de 1 línea, sin narrativa de sesión.

2026-11-01 — ver SESSION_HANDOFF.md

## 🔴 Crítico
- (nada)

## 🟠 Importante
- [ ] Guido: probar los 3 temas de Noche de brujas en pantalla de verdad ("Probar en pantalla"), con silencio incluido; que no se asome el cartel de TCL.
- [ ] Revisar las ideas de la investigación de Google TV (`docs/ideas-google-tv.md`) y elegir cuáles hacer.
- [ ] Probar un reinicio programado real (ponerlo 2-3 minutos adelante y apagar la TV con el control); confirmar que la pantalla no queda prendida después.

## 🟡 Cuando se pueda
- [ ] Ajustar la curva por defecto si a Guido le resulta muy brusca o muy suave (hoy: Pareja, 0,5 dB por punto).
- [ ] Auto-actualización desde los Releases de GitHub: descartada por ahora (cambios esporádicos; la firma está en el repo público). Revisar si se empieza a cambiar seguido.
- [ ] Revisar el rendimiento de los temas más cargados (Espacial, Medieval, Candelabro) en la TV con `dumpsys gfxinfo local.volumenmaestro:motor`.
- [ ] Comprobar que el ecualizador de la mezcla se recrea solo si el servidor de audio se reinicia (no se pudo provocar a propósito; el watchdog lo revisa cada 10 s).
- [ ] Panel de apps: medir en la TV el costo de la vista previa cada 60 s (`top` mientras se ve algo); si molesta, dejar solo la de al abrir el panel.
- [ ] Pestaña Temas: al entrar, el título del grupo del tema elegido queda apenas fuera de pantalla.

## 🟢 Ideas / algún día
- [ ] Emisor infrarrojo por Wi-Fi (Broadlink) para manejar el volumen del Sony también con Dolby (descartado por ahora: Guido no quiere comprar hardware).
- [ ] Agregar `direction` (subió/bajó) a `OverlayState` para que algunos temas (Retro, Espacial, Steampunk) animen hacia el lado correcto.
