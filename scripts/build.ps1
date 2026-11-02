$ErrorActionPreference = 'Stop'
Push-Location (Split-Path $PSScriptRoot -Parent)
try {
    New-Item -ItemType Directory -Force build/classes, build/test-classes, dist | Out-Null
    $sources = @(Get-ChildItem src/main/java -Filter *.java -Recurse | ForEach-Object { $_.FullName })
    & javac -encoding UTF-8 --release 17 -d build/classes $sources
    if ($LASTEXITCODE -ne 0) { throw 'Java compilation failed.' }
    & jar --create --file dist/ssd_ip_cp4_2026f_224569_app.jar --main-class timefit.App -C build/classes .
    if ($LASTEXITCODE -ne 0) { throw 'JAR packaging failed.' }
    Write-Host 'Built dist/ssd_ip_cp4_2026f_224569_app.jar'
} finally { Pop-Location }
