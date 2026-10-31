---
name: instalar
description: Compila Volumen Maestro, lo instala en la TV de Guido y verifica que el motor arrancó. Usar cuando el usuario dice "instalá", "subilo a la tele", "probalo en la TV", o después de cambiar código de la app.
---

# /instalar — compilar, instalar en la TV y verificar

Variables (PowerShell):

```powershell
$env:JAVA_HOME = (Get-ChildItem 'C:\Program Files\Eclipse Adoptium' -Directory | Where-Object Name -like 'jdk-17*' | Select-Object -First 1).FullName
$adb = "$env:LOCALAPPDATA\Microsoft\WinGet\Packages\Google.PlatformTools_Microsoft.Winget.Source_8wekyb3d8bbwe\platform-tools\adb.exe"
$tv  = "192.168.0.219:5555"
```

## Pasos

1. **Compilar y probar**: `.\gradlew.bat assembleRelease testDebugUnitTest --console=plain`. Si falla, mostrar las líneas `e:` y frenar. Si Gradle dice "being used by another process", esperar unos segundos y reintentar una vez.
2. **Conectar**: `& $adb connect $tv` → `& $adb devices` tiene que decir `device`. `unauthorized` → pedirle a Guido que acepte el cartel en la TV. Sin respuesta → la TV está apagada o cambió de IP (ver `CLAUDE.md`).
3. **Instalar como actualización**: `& $adb -s $tv install -r app\build\outputs\apk\release\app-release.apk`. Si dice `INSTALL_FAILED_UPDATE_INCOMPATIBLE`, la firma no coincide: **frenar y avisar** (no desinstalar sin OK: se pierde la autorización y la config).
4. **Verificar con evidencia** (a los ~5 s):
   - `& $adb -s $tv shell pidof local.volumenmaestro:motor` devuelve un número.
   - `& $adb -s $tv logcat -d -s VolumenMaestro` muestra "Motor iniciado" sin errores después.
   - `& $adb -s $tv shell service call media.audio_flinger 9` devuelve un float (el volumen maestro).
   - Si el cambio es visual: subir el volumen con `cmd media_session volume --stream 3 --adj raise` sobre la pantalla de inicio y sacar `screencap -p /sdcard/x.png` + `pull` para mirarla. Borrar la captura de la TV después.
5. **Reportar**: qué se instaló, la evidencia de cada punto y lo que queda NO VERIFICADO (por ejemplo, lo que depende del oído de Guido).

## Reglas

- Nunca `adb uninstall local.volumenmaestro` ni `pm clear` sin OK: borran la clave ADB autorizada y los ajustes.
- La TV la está usando Guido: no navegar menús con flechas sin mirar una captura antes de apretar OK.
