# TODO — Volumen Maestro

> ÚNICA fuente de pendientes del proyecto. Completado → SE BORRA (la historia vive en CHANGELOG y git). Header de 1 línea, sin narrativa de sesión.

2026-11-05 — ver SESSION_HANDOFF.md

## 🔴 Crítico
- (nada)

## 🟠 Importante
- [ ] Confirmar el reinicio al apagar en Doze: la prueba completa se hizo con reposo con red (Doze mostró el mismo tiempo antes de dormirse). El lunes 09/11 a la mañana, si Guido apagó la TV el domingo después de las 22:00, "Sin reiniciar" en Optimizar tiene que marcar solo las horas desde esa noche, y Guido cuenta si la pantalla quedó apagada.
- [ ] Guido: probar los 3 temas de Noche de brujas en pantalla de verdad ("Probar en pantalla"), con silencio incluido; que no se asome el cartel de TCL.
- [ ] Ideas que quedan de `docs/ideas-google-tv.md` (opcionales): SmartTube desde su GitHub oficial, Wholphin, LocalSend, temporizador para dormir en la app, probar el ajuste de dibujo `skiavk` (dato de CleanYourTV).

## 🟡 Cuando se pueda
- [ ] Reinicio al apagar: acortar el rato con la pantalla prendida al arrancar (hoy la app la apaga a los ~15 s de arrancar el motor), si a Guido le molesta de noche.
- [ ] Ajustar la curva por defecto si a Guido le resulta muy brusca o muy suave (hoy: Pareja, 0,5 dB por punto).
- [ ] Auto-actualización desde los Releases de GitHub: descartada por ahora (cambios esporádicos; la firma está en el repo público). Revisar si se empieza a cambiar seguido.
- [ ] Revisar el rendimiento de los temas más cargados (Espacial, Medieval, Candelabro) en la TV con `dumpsys gfxinfo local.volumenmaestro:motor`.
- [ ] Comprobar que el ecualizador de la mezcla se recrea solo si el servidor de audio se reinicia (no se pudo provocar a propósito; el watchdog lo revisa cada 10 s).
- [ ] Panel de apps: medir en la TV el costo de la vista previa cada 60 s (`top` mientras se ve algo); si molesta, dejar solo la de al abrir el panel.
- [ ] Pestaña Temas: al entrar, el título del grupo del tema elegido queda apenas fuera de pantalla.

## 🟢 Ideas / algún día
- [ ] Emisor infrarrojo por Wi-Fi (Broadlink) para manejar el volumen del Sony también con Dolby (descartado por ahora: Guido no quiere comprar hardware).
- [ ] Agregar `direction` (subió/bajó) a `OverlayState` para que algunos temas (Retro, Espacial, Steampunk) animen hacia el lado correcto.
