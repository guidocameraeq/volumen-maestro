# Ideas para la TV (investigación del 01/11/2026)

Lo que trajeron 8 investigadores (Sonnet) sobre qué le hace la gente a sus Google TV. **Nada de esto se probó en la TV de Guido**: son ideas para elegir. Fuentes: blogs, guías de TCL y Google, GitHub; casi no pudieron abrir Reddit/XDA. Lo que dice "sin confirmar" no tenía buena fuente.

**Hecho el 01/11/2026**: DNS privado con AdGuard (aplicado, las apps andan). Ya estaban: animaciones a 0,5×. No aplica en esta TV: apagar los servicios de publicidad de Google (no existen en Android 12). Descartado por Guido: avisos desde el celu.

Recordatorio de las reglas (`CLAUDE.md`): Ambient, Game Bar, Alexa y el orden de apps no se tocan sin "esto cambio / esto preservo"; todo lo que se quite es reversible y se anota en `tv-guido-tcl/apps-quitadas.txt`.

## ⭐ Las mejores ideas (la recomendación)

| Idea | Qué gana Guido | Riesgo |
|---|---|---|
| **SmartTube** (YouTube sin publicidad, salta patrocinios) | YouTube limpio | Bajo, **pero** a fines de 2025 le metieron malware a dos versiones (30.43 y 30.47). Bajar solo desde su GitHub oficial (versión 30.56 o más nueva) |
| **DNS privado con AdGuard** (`private_dns_mode hostname` + `dns.adguard-dns.com`) | Menos publicidad y rastreo en toda la TV | Bajo, se deshace con `private_dns_mode off`. No saca la publicidad de YouTube. Probar Netflix/Disney+ después |
| **Modo "Solo apps"** de Google TV (Ajustes → Cuentas → perfil → Modo solo apps) | Inicio más limpio y liviano | Bajo, reversible. Ya se había estudiado el 31/10 y se descartó porque saca "Seguir viendo" y el Asistente |
| **Animaciones a 0,5×** (`window/transition/animator_duration_scale`) | Se siente más rápida | Ninguno, se vuelve a 1 |
| **Wholphin** (cliente de Jellyfin con cara de Plex) | Mejor Jellyfin | Bajo (Play Store) |
| **LocalSend** (pasar archivos del celu/PC a la TV) | Mandar fotos, videos, APKs sin cable | Ninguno |
| **Temporizador para dormir + atenuador de pantalla** como función de Volumen Maestro | Apagar la TV sola después de X minutos | Ninguno: la app ya tiene carteles y shell |

## Apps para instalar
- **Projectivy Launcher** (Play Store): inicio sin publicidad ni recomendaciones, muy personalizable. Para que sea el inicio "de verdad" hay que deshabilitar el inicio de Google por ADB (`launcherx` y `setupwraith`): **riesgoso** (pantalla negra si se hace mal, puede romper Ambient y el Asistente, una actualización lo puede revertir). Choca con la regla de Ambient: solo con OK explícito. **FLauncher** es más liviano pero está parado desde 2023.
- **Sideload Launcher**: muestra las apps instaladas a mano que Google TV esconde.
- **Downloader** (AFTVnews) y **Send Files to TV**: para bajar APKs a la TV.
- **Kodi 21 "Omega"**: biblioteca local y de red. Con la fibra en PCM, el passthrough no aplica.
- **Just Player** / **VLC** / **Nova Video Player**: para archivos de un disco o la red.
- **S0undTV**: Twitch sin publicidad, con chat (probar en 2 GB).
- **Stremio**: la app es legal; muchos complementos de la comunidad no lo son.
- **AirScreen**: recibe AirPlay y Cast desde iPhone/Mac (resultados variables; lo protegido sale negro). La TV ya tiene la app de AirPlay de Amlogic.
- **Aerial Views**: protector con videos aéreos tipo Apple. En Google TV se pone por ADB. Choca con Ambient con las fotos de Guido. (01/11: en vez de esto, Guido hace su propio protector, Museum Slideshow, en `../museum-slideshow`.)
- **Plexamp**: no tiene app para TV.

## Velocidad y limpieza
- **Seguras**: animaciones a 0,5×, `pm trim-caches`, medir con `dumpsys meminfo` antes y después.
- **Deshabilitar el sistema de publicidad de Google** (reversible con `pm enable`): `com.android.adservices.api`, `com.android.ondevicepersonalization.services`, `com.android.federatedcompute.services`, `com.google.android.feedback`, `com.android.tv.feedbackconsent`. Una fuente sola (no TCL); de a uno y mirando la TV.
- **Límite de procesos en segundo plano** (Opciones de desarrollador, "como mucho 3"): puede matar a Volumen Maestro o a Plex. Probar con cuidado o no tocar.
- **Restringir apps en segundo plano**: `cmd appops set <app> RUN_ANY_IN_BACKGROUND ignore` (por ejemplo Plex). Nunca a Volumen Maestro.
- **Listas por modelo de TCL**: `github.com/Gathin23/cleanyourtv` (81 modelos TCL con script para restaurar) y `vgscity/vgs-cleantv`. Compararlas antes con lo ya quitado.
- **NUNCA tocar** (todas las fuentes coinciden): `com.tcl.suspension` (menú de entradas), `com.tcl.ttvs` (Alexa y actualizaciones), `com.tcl.tv`, `com.tcl.tvinput`, `com.tcl.providers.config` y `com.tcl.initsetup` (se cae el Wi-Fi), `com.tcl.tcl_bt_rcu_service` y `com.tcl.autopair` (el control), `com.google.android.tv.remote.service`, `com.android.location.fused` (puede no arrancar), Google Play Services, la Play Store, los teclados.
- **No**: root, desbloquear el cargador de arranque o ROMs: borran la TV y Netflix pasa a calidad SD.

## Imagen y sonido
- Modo de imagen **Película**; apagar "Ahorro de energía", contenido adaptativo, reducción de ruido y "Motion Clarity"; antitemblor bajo o medio.
- **"Coincidir con la frecuencia del contenido"** (Pantalla y sonido → avanzado, si existe en esta TV): películas a 24p sin tirones. Netflix ya lo usa.
- **Retraso de audio digital** (Audio → avanzado): si el sonido del Sony se adelanta o atrasa. Probar también el ajuste del Sony.
- Modo **Diálogo/Voz** y modo noche: ojo, pueden pelearse con el ecualizador de Volumen Maestro (ADR-013). Probar.
- **Menú de servicio de TCL**: solo mirar, nunca cambiar.
- AFRd (cambio de frecuencia del sistema): necesita root, descartado.

## Control y automatización
- **tvQuickActions Pro** y **Button Mapper**: reasignan botones con un toque, doble toque o mantener. **Ojo**: usan el filtro de teclas de accesibilidad, el mismo que rompía el mantener apretado del volumen (ADR-011). Las reseñas confirman retraso y el problema de las teclas mantenidas. Mejor hacerlo dentro de Volumen Maestro con `getevent`, como el panel de apps.
- **Home Assistant** (integración "Android TV Remote"): manejar la TV desde automatizaciones. Necesita un servidor de Home Assistant.
- **Celular como control**: la app Google TV / Google Home. Sin instalar nada en la TV.
- **Botones de marca** (Netflix, YouTube, Prime): Volumen Maestro los podría usar como atajos, pero no puede cancelar lo que ya hacen.

## Red, privacidad y "casteo"
- DNS privado (arriba). Si Disney+ falla con un DNS que filtra, puede ser IPv6 del router.
- Privacidad en la TV: Ajustes → Sistema → Privacidad, y en TCL salir del "programa de mejora de la experiencia".
- **Tailscale**: ver el Plex/Jellyfin de afuera de la casa. Ocupa la única ranura de VPN.
- **scrcpy** desde la PC: ver y manejar la TV en una ventana. Lo protegido sale negro.
- Mejor **Cast** desde la app (YouTube, Plex) que espejar la pantalla. Ethernet o Wi-Fi de 5 GHz para menos cortes.

## Ideas para sumar a Volumen Maestro (de proyectos de GitHub)
1. **Temporizador para dormir** con aviso antes, **atenuador** y **filtro de luz azul** (como OmniTV).
2. **Avisos con los temas de la app** mandados desde el celu o Home Assistant (como TvOverlay o PiPup: un puertito HTTP en la TV).
3. **Pantalla de "apps deshabilitadas"** con deshacer y copia antes de tocar (como CleanTV o Tellyscope).
4. **Estado y captura desde el celular** (como terebi, por Telegram).
5. **Abrir una app al prender la TV** (Launch-On-Boot), junto al reinicio programado.
6. **Límite de uso por app** (temporizador para chicos).

## Cosas a vigilar
- **Reglas nuevas de Google para instalar apps a mano**: arrancan en 2026 en algunos países y se extienden en 2027. No está claro si incluyen Google TV. Las instalaciones por ADB parecerían quedar afuera. Una nota sin confirmar habla de un cambio de seguridad (CVE-2026-0073) que podría limitar la ADB local, que es la que usa Volumen Maestro: vigilar las actualizaciones de la TV.
- Las actualizaciones de TCL pueden volver a habilitar apps deshabilitadas.
