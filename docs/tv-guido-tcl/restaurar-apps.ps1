# Vuelve a instalar en la TV las apps de fábrica listadas en apps-quitadas.txt.
# Uso: .\restaurar-apps.ps1                 (restaura todas)
#      .\restaurar-apps.ps1 com.tcl.hearaid  (restaura solo esa)
param([string[]]$Paquetes)

$tv  = "192.168.0.219:5555"
$adb = "$env:LOCALAPPDATA\Microsoft\WinGet\Packages\Google.PlatformTools_Microsoft.Winget.Source_8wekyb3d8bbwe\platform-tools\adb.exe"

if (-not $Paquetes) { $Paquetes = Get-Content (Join-Path $PSScriptRoot "apps-quitadas.txt") | Where-Object { $_ } }

& $adb connect $tv | Out-Null
foreach ($p in $Paquetes) {
    "$p -> $(& $adb -s $tv shell cmd package install-existing --user 0 $p)"
}
