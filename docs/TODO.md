# TODO — Volumen Maestro

> ÚNICA fuente de pendientes del proyecto. Completado → SE BORRA (la historia vive en CHANGELOG y git). Header de 1 línea, sin narrativa de sesión.

2026-10-31 — ver SESSION_HANDOFF.md

## 🔴 Crítico
- (nada)

## 🟠 Importante
- [ ] Guido: probar el volumen de oído en Plex, Disney+, Prime y HBO Max en modo ENCENDIDO. Si alguna no responde, revisar su camino de audio (`docs/audio-dolby.md`): puede seguir mandando Dolby por offload.
- [ ] Probar un reinicio programado real (ponerlo 2-3 minutos adelante y apagar la TV con el control); confirmar que la pantalla no queda prendida después.

## 🟡 Cuando se pueda
- [ ] Ajustar la curva por defecto si a Guido le resulta muy brusca o muy suave (hoy: Pareja, 0,5 dB por punto).
- [ ] Revisar el rendimiento de los temas más cargados (Espacial, Medieval) en la TV con `dumpsys gfxinfo local.volumenmaestro:motor`.

## 🟢 Ideas / algún día
- [ ] Emisor infrarrojo por Wi-Fi (Broadlink) para manejar el volumen del Sony también con Dolby (descartado por ahora: Guido no quiere comprar hardware).
- [ ] Agregar `direction` (subió/bajó) a `OverlayState` para que algunos temas (Retro, Espacial, Steampunk) animen hacia el lado correcto.
