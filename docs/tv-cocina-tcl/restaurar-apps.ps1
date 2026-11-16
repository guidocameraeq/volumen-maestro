# Vuelve a activar en la TV de la cocina las apps listadas en apps-desactivadas.txt.
# Uso: .\restaurar-apps.ps1                     (todas, y el inicio vuelve al de Google)
#      .\restaurar-apps.ps1 com.netflix.ninja   (solo esa)
# Antes: en la TV, Depuracion USB activada (ver README.md, "Como conectarse").
param([string[]]$Paquetes)

$tv  = "192.168.0.8:5555"
$adb = (Get-Command adb -ErrorAction SilentlyContinue).Source
if (-not $adb) { $adb = "$env:LOCALAPPDATA\Microsoft\WinGet\Packages\Google.PlatformTools_Microsoft.Winget.Source_8wekyb3d8bbwe\platform-tools\adb.exe" }
if (-not (Test-Path $adb)) { $adb = "$env:LOCALAPPDATA\platform-tools-claude\platform-tools\adb.exe" }

$todas = -not $Paquetes
if ($todas) { $Paquetes = Get-Content (Join-Path $PSScriptRoot "apps-desactivadas.txt") | Where-Object { $_ } }

& $adb connect $tv | Out-Null
foreach ($p in $Paquetes) {
    "$p -> $(& $adb -s $tv shell pm enable --user 0 $p)"
}

if ($todas) {
    # El inicio vuelve al de Google y el protector al Ambient de fabrica.
    & $adb -s $tv shell cmd package set-home-activity --user 0 com.google.android.tvlauncher/.MainActivity
    & $adb -s $tv shell settings put secure screensaver_components com.google.android.backdrop/.Backdrop
    # Apaga el servicio de accesibilidad del launcher y vuelve las animaciones a 1x.
    & $adb -s $tv shell settings delete secure enabled_accessibility_services
    & $adb -s $tv shell settings put secure accessibility_enabled 0
    foreach ($k in 'window_animation_scale', 'transition_animation_scale', 'animator_duration_scale') {
        & $adb -s $tv shell settings put global $k 1.0
    }
    "Listo. Esperar 15 segundos y reiniciar la TV. El inicio nuevo (com.gathin.tvhome) queda instalado; para borrarlo: adb shell pm uninstall com.gathin.tvhome"
}
