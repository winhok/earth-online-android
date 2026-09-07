$ErrorActionPreference = 'Stop'
$Root = Split-Path -Parent $PSScriptRoot
$Jar = Join-Path $Root 'gradle/wrapper/gradle-wrapper.jar'
$Expected = '81a82aaea5abcc8ff68b3dfcb58b3c3c429378efd98e7433460610fecd7ae45f'
if (-not (Test-Path $Jar)) {
    $Temp = "$Jar.$([Guid]::NewGuid().ToString('N')).tmp"
    try {
        Invoke-WebRequest -Uri 'https://raw.githubusercontent.com/gradle/gradle/v8.13.0/gradle/wrapper/gradle-wrapper.jar' -OutFile $Temp -TimeoutSec 120 -UseBasicParsing
        if ((Get-FileHash -Path $Temp -Algorithm SHA256).Hash.ToLowerInvariant() -ne $Expected) {
            throw 'Wrapper checksum mismatch. Nothing executed.'
        }
        Move-Item -Path $Temp -Destination $Jar -Force
    } finally {
        if (Test-Path $Temp) { Remove-Item $Temp -Force }
    }
}
if ((Get-FileHash -Path $Jar -Algorithm SHA256).Hash.ToLowerInvariant() -ne $Expected) {
    throw 'Existing wrapper JAR checksum mismatch. Refusing to run.'
}
