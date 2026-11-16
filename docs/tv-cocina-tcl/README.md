# La TV de la cocina (TCL 32") — historia completa

Todo lo que se le hizo a la TV chica de la cocina, cómo y por qué, para poder retomarla cualquier día. Se usa **solo para YouTube**. La TV del living (TCL 65" Google TV) está en [`../tv-guido-tcl/`](../tv-guido-tcl/); Volumen Maestro **no** está instalado acá.

## La TV

| Dato | Valor |
|---|---|
| Modelo | TCL 32" 1080p. Sistema la llama `UnionTV` (producto, equipo y modelo), variante `TCL-LA-RT41KB-S1` (Latinoamérica) |
| Sistema | Android TV 9 (SDK 28), parche de seguridad 2020-07-05, build `PPR2.180905.006.A1` (20200714). No le llegan más actualizaciones |
| Chip | Realtek RTD284x (RT2841), 32 bits, **1,4 GB de RAM**, marcada `low_ram` por el sistema |
| Almacenamiento | 4,2 GB para datos (la mitad libre) |
| Pantalla | Interfaz a 1280×720 (densidad 213) |
| Red | Solo Wi-Fi. IP `192.168.0.8` |
| Reposo | "Inicio rápido" (suspende a RAM y corta la red): desde la PC no se la puede despertar |

## Cómo conectarse

1. En la TV: Ajustes → Preferencias del dispositivo → Acerca de → apretar 7 veces "Compilación" → aparece "Opciones de desarrollador".
2. Opciones de desarrollador → **Depuración USB** activada. Con eso escucha ADB por la red en el puerto 5555.
3. PC y TV en la misma red Wi-Fi. `adb connect 192.168.0.8:5555`. La primera vez la TV pregunta "¿Permitir depuración USB?" → aceptar.
4. **Ojo, hay dos TCL en la red**: la `.219` es la del living. Antes de tocar nada, `adb -s <ip>:5555 shell getprop ro.product.model` tiene que decir `UnionTV` (la del living dice `Smart_TV_Pro`).
5. **Al terminar, apagar la depuración** (acá nada la necesita): Opciones de desarrollador → Depuración USB desactivada, o `adb shell settings put global adb_enabled 0`. El puerto 5555 va sin cifrar.

## Lo que se encontró (15/11/2026)

- Inicio de Google lleno de publicidad: filas de "TCL Channel", Disney+, FIFA+, Pluto TV, novelas "gratis en TCL Channel".
- ~715 MB de RAM libre de 1,4 GB.
- Instalados Netflix y Spotify (Prime venía desactivado de fábrica), servicios de TCL (analítica, tienda, "limpiador", cuenta TCL) y el Ambient de Google.
- 5 apps venían desactivadas de fábrica ([`respaldo-inicio/desactivadas-de-fabrica.txt`](respaldo-inicio/desactivadas-de-fabrica.txt)); no se tocaron.

## Qué se hizo, en orden

Base: [CleanYourTV](https://github.com/Gathin23/cleanyourtv) (commit `ddc3b2c`), que tiene una lista probada para este mismo modelo y variante, también en una unidad de Argentina (`examples/tcl-uniontv-latin-america`). Se usaron sus scripts (`tools/disable.ps1`, con sus chequeos de seguridad) y su launcher.

### 1. Respaldo
Lista de paquetes, desactivados, `getprop` y todos los ajustes guardados antes de tocar nada. Lo público en [`respaldo-inicio/`](respaldo-inicio/); los volcados completos en `.local-privado/tv-cocina/` (fuera del repo).

### 2. Apps desactivadas (32)
Guido eligió: **queda YouTube, Chromecast, búsqueda por voz y HDMI; todo lo demás afuera.** Se desactivaron con `pm disable-user --user 0` (no se desinstala nada, **se pueden volver a activar**), en 4 tandas con captura entre cada una. La lista exacta está en [`apps-desactivadas.txt`](apps-desactivadas.txt) y por tanda en [`listas-por-tanda/`](listas-por-tanda/):

| Paquete | Qué era |
|---|---|
| `com.tcl.bi`, `com.google.android.feedback`, `com.google.android.tv.bugreportsender` | Analítica de TCL, comentarios a Google, envío de errores |
| `com.google.android.tvrecommendations`, `com.google.android.leanbacklauncher.recommendations` | Filas de recomendaciones del inicio |
| `com.tcl.partnercustomizer` (+ `.resource`), `com.google.android.leanbacklauncher.partnerwidget`, `...partnercustomizer` | Promos de TCL y de "socios" en el inicio |
| `android.autoinstalls.config.tcl.device` | Instalaciones automáticas de apps de socios |
| `com.tcl.esticker`, `com.tcl.appmarket2`, `com.tcl.tvweishi` | Etiquetas de tienda, tienda TCL, "limpiador" |
| `com.tcl.usercenter`, `com.tcl.copydatatotv`, `com.tcl.useragreement` | Cuenta TCL, copiar del celu, pantalla de términos |
| `com.netflix.ninja`, `com.spotify.tv.android` | Netflix y Spotify (Guido no los usa en esta TV) |
| `com.tcl.ui_mediaCenter`, `com.tcl.audioplayer`, `com.tcl.imageplayer`, `com.tcl.videoplayer` | Reproductores de pendrive (no se usa el USB) |
| `com.tcl.versionUpdateApp`, `com.tcl.rc.ota` | Actualizaciones de TCL (no llegan más a este modelo) |
| `com.google.android.backdrop`, `com.android.dreams.basic`, `com.android.wallpaper.livepicker` | Ambient de Google y otros protectores/fondos (lo reemplaza el reloj del launcher) |
| `com.google.android.marvin.talkback`, `com.android.gallery3d`, `com.android.printspooler` | Lector de pantalla, galería vieja, impresión |
| `com.tcl.common.terminalmanagerplus` | Servicio de administración de TCL que arranca con la TV |
| `com.google.android.tvlauncher` | El inicio de Google (lo reemplaza el launcher nuevo) |

**`com.tcl.usercenter` vuelve solo**: el sistema lo reactiva en cada arranque (igual que en otras TCL con Android 9). Queda prendido sin hacer nada; no hace falta volver a desactivarlo.

**Se dejaron a propósito**: YouTube, Chromecast (`mediashell`), voz (`katniss`, `speech.pumpkin`, `tts`), HDMI y sintonizador (`com.tcl.tv`, `com.tcl.tvinput`, `com.tcl.tvpassthrough`, `com.tcl.sourcemananger`), teclados, Play Store y Play Services, los servicios del control (`keyhelp`, `keycustomfunctionservice`, `autopair`, `tcl_bt_rcu_service`), **`com.tcl.framework.custom` (lleva el volumen al parlante: ver punto 4, no se toca)**, Ajustes y todo el núcleo de Android. **`com.tcl.factory.view` no se abre nunca** (los paquetes "factory" de TCL pueden activar el modo fábrica al abrirlos).

**Para restaurar**: [`restaurar-apps.ps1`](restaurar-apps.ps1) (todas, o una: `.\restaurar-apps.ps1 com.netflix.ninja`). A mano: `adb shell pm enable --user 0 <paquete>`.

### 3. Launcher nuevo, que abre YouTube
- El launcher de CleanYourTV (`com.gathin.tvhome`), compilado en la PC con [`launcher/config.xml`](launcher/config.xml); el APK que se instaló es [`launcher/cleanyourtv-launcher.apk`](launcher/cleanyourtv-launcher.apk). Así se ve: [`launcher/inicio-nuevo.png`](launcher/inicio-nuevo.png).
- Fondo negro, saludo, hora, fecha, botones Entradas / Wi-Fi / Ajustes. En el dock: YouTube (y Play Store, que entra solo para llenar el lugar).
- Cambios a la config de fábrica del launcher: dock solo con YouTube; `com.tcl.tv` oculto de la grilla y usado como "TV en vivo" del menú Entradas; `inputs_cec_state_only` (en esta TV el HDMI 1 vacío figura "conectado"); "Abrir al iniciar" disponible, también al despertar la pantalla, esperando hasta 60 s a que haya internet; `power_on_home` (si la TV arranca en su app de TV, vuelve a Home) y **`key_filter` apagado** (ver punto 4).
- Instalado con `build-and-install.ps1 -StockLauncher none -HomeKeyService -RestoreKeyServices`: prende el servicio de accesibilidad `com.gathin.tvhome/.HomeKeyService` y le da permiso para volver a prenderse si Android lo apaga.
- En la TV: Ajustes del lanzador → **Abrir al iniciar: YouTube**. Protector de pantalla: el reloj del launcher (`com.gathin.tvhome/.ClockDream`).

### 4. El "AppClean" de TCL
- Después del primer reinicio, al prender la TV aparecía la app de TV/HDMI de TCL y no YouTube.
- Causa (en el registro): `com.tcl.framework.custom` corre un `AppCleanService` que, ~20 s después de prender, **cierra a la fuerza toda app que no sea del sistema** y no esté en pantalla. El launcher moría y no podía abrir YouTube al despertar.
- **Primer intento (descartado): desactivar el paquete entero.** No se puede apagar solo el AppClean desde ADB (el paquete corre como sistema), así que con el OK de Guido se desactivó todo `com.tcl.framework.custom`. El launcher sobrevivía y abría YouTube, pero **el volumen del control dejó de andar**: las teclas llegaban y Android cambiaba su número, pero el cambio no llegaba al parlante (en el registro: `TWindowManager: get service proxy failed`). Ese paquete tiene el `AudioInvokeService` que pasa el volumen al audio de la TV. Se reactivó y el volumen volvió (probado por Guido). Su estado de antes está en `.local-privado/tv-cocina/framework-custom-antes.txt`.
- **Arreglo que quedó: el servicio de accesibilidad del launcher.** Con `HomeKeyService` prendido, el AppClean ya **no cierra el launcher** (comprobado en el registro después de reiniciar: no hay `forceStop com.gathin.tvhome`). El servicio corre con `key_filter` apagado: no lee las teclas, así que no le saca la repetición al volumen mantenido (en las TCL con Google TV, un servicio que filtra teclas hace que Volumen+ suba de a un paso).
- Comprobado el 15/11 22:19: apagar con el control y prender → la pantalla se prende y al segundo el launcher abre YouTube.

### 5. Optimización final
- Animaciones del sistema a 0,5× (`window_animation_scale`, `transition_animation_scale`, `animator_duration_scale`; antes 1,0). Para volver: las tres en `1.0`.
- YouTube recompilado para que abra más rápido: `cmd package compile -m speed -f com.google.android.youtube.tv` (~100 MB más de espacio). Cuando Play Store actualice YouTube, Android vuelve a su compilación normal; se puede repetir el comando.
- **No se hizo**: DNS con AdGuard (no saca la publicidad de YouTube, que es lo único que se usa acá).

### 6. Resultado
- Al prender con el control abre YouTube solo; con Home se ve el launcher limpio. El volumen anda.
- Si la TV **arranca de cero** (reinicio o corte de luz) queda en el launcher, no en YouTube: el launcher espera internet para abrir YouTube y la TV pone su app de TV encima, lo que cancela la espera; `power_on_home` la devuelve a Home con YouTube seleccionado (un OK).
- RAM libre: de ~715 MB a **~788 MB** (con YouTube abierto).
- Depuración ADB apagada al terminar; comprobado que el puerto 5555 rechaza conexiones.

## Cosas que NO funcionan (para no volver a probarlas)
- **Desactivar `com.tcl.framework.custom`**: el control sigue andando pero el volumen no llega al parlante.
- `pm disable` de un componente suelto de `com.tcl.framework.custom`: "Shell cannot change component state".
- Despertar la TV desde la PC: el "Inicio rápido" corta el Wi-Fi en reposo.
- Capturas con `tools/tv.py --screenshot` de CleanYourTV: salen negras en esta TV. `adb shell screencap -p` + `adb pull` sí anda.
- `launcher\build-and-install.ps1` de CleanYourTV corta la compilación por un aviso de Gradle ("SDK XML version 4"). Compilar aparte (`gradlew.bat assembleRelease`) y después instalar con `$env:SKIP_BUILD='1'`.

## Sesiones
- [`sesiones/2026-11-15.md`](sesiones/2026-11-15.md) — el primer día: debloat, launcher, el AppClean de TCL, el volumen y la optimización.
