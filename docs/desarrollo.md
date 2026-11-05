# Desarrollo: entorno, compilar, instalar y depurar

## Herramientas (Windows)

| Herramienta | Cómo se instaló | Dónde |
|---|---|---|
| adb (Platform-Tools 37) | `winget install Google.PlatformTools` | `%LOCALAPPDATA%\Microsoft\WinGet\Packages\Google.PlatformTools_Microsoft.Winget.Source_8wekyb3d8bbwe\platform-tools\` |
| JDK 17 | `winget install EclipseAdoptium.Temurin.17.JDK` | `C:\Program Files\Eclipse Adoptium\jdk-17*` |
| Android SDK | command-line tools de Google + `sdkmanager "platforms;android-34" "build-tools;34.0.0"` | `%LOCALAPPDATA%\Android\Sdk` |
| Gradle | wrapper del repo (8.7); se descarga solo | — |

`local.properties` (no versionado) apunta al SDK: `sdk.dir=C\:\\Users\\<usuario>\\AppData\\Local\\Android\\Sdk`.

## Compilar

```powershell
$env:JAVA_HOME = (Get-ChildItem 'C:\Program Files\Eclipse Adoptium' -Directory | Where-Object Name -like 'jdk-17*' | Select-Object -First 1).FullName
.\gradlew.bat assembleRelease testDebugUnitTest
```

El APK queda en `app\build\outputs\apk\release\app-release.apk` (~1,8 MB, minificado con R8).

## Firma

- `keystore.jks` en la raíz, **versionado en el repo** (alias `vm`, contraseña `android`). Es la misma clave de la v1. Está en el repo público por decisión de Guido (ADR-012): así se puede compilar e instalar como actualización desde cualquier PC con un `git clone`.
- Si se pierde: hay que desinstalar la app de la TV, instalar la nueva y volver a habilitar el servicio y aceptar el permiso de depuración (se pierden los ajustes salvo la copia en `/sdcard`). Para generar una nueva: `keytool -genkeypair -keystore keystore.jks -storepass android -keypass android -alias vm -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Volumen Maestro"`.

## Instalar en la TV

Ver la skill `/instalar` (`.claude/skills/instalar/SKILL.md`). Resumen:

```powershell
adb connect 192.168.0.219:5555
adb -s 192.168.0.219:5555 install -r app\build\outputs\apk\release\app-release.apk
```

Primera instalación en una TV nueva:
1. Activar USB debugging en la TV (ver `tv-guido-tcl/README.md`).
2. Instalar el APK.
3. Habilitar el servicio: Settings → System → Accessibility → Volumen Maestro (o `adb shell settings put secure enabled_accessibility_services local.volumenmaestro/.VolumeService` + `settings put secure accessibility_enabled 1`).
4. Aceptar en la TV el "Allow USB debugging?" que pide la app ("Always allow").
5. Audio Output en **Optical** (menú de la TV).

## Depurar

| Para | Comando |
|---|---|
| Registro del motor | `adb logcat -s VolumenMaestro` |
| ¿Corre el motor? | `adb shell pidof local.volumenmaestro:motor` |
| Volumen maestro real | `adb shell service call media.audio_flinger 9` → `Parcel(00000000 3f800000)` = 1.0 |
| Simular el control | `adb shell cmd media_session volume --stream 3 --adj raise` (o `lower`) |
| Ver la pantalla | `adb shell screencap -p /sdcard/x.png` + `adb pull /sdcard/x.png` (negro sobre Netflix por DRM) |
| Qué camino usa el audio | `adb shell dumpsys media.audio_flinger` (ver `audio-dolby.md`) |
| Estado de los modos | `adb shell settings get system tcl_sys_audio_spdif_outmode` y `settings get global encoded_surround_output` |

Ojo: `input keyevent` desde la PC **no** pasa por el servicio de accesibilidad: para probar el atajo hace falta el control real.

## Lista de verificación en la TV

- [ ] El motor arranca solo al instalar y al reiniciar la TV.
- [ ] Con volumen ±, el panel tapa el cartel de TCL abajo y la barra gris a la derecha.
- [ ] El sonido baja en YouTube y en Netflix (de oído).
- [ ] Apagar desde la app deja Dolby, Auto y volumen 1.0; prender lo vuelve a PCM.
- [ ] Prender la TV con la app apagada → vuelve a ENCENDIDO.
- [ ] Cada tema: vista previa igual que en pantalla.
- [ ] Atajo con un botón libre del control.
- [ ] Reinicio al apagar: con "Desde las" antes de la hora actual y hoy elegido, apagar reinicia y queda apagada; apagar otra vez esa noche, no. La última noche queda guardada: para repetir hay que esperar otro día elegido. En reposo sin red la PC pierde ADB al apagar: para probar solo, `settings put system tcl_sys_network_standby_enabled 1`, y al terminar volver a `0`.
