$ErrorActionPreference = 'Stop'
Push-Location (Split-Path $PSScriptRoot -Parent)
try {
    & javadoc -Xdoclint:all,-missing -quiet -encoding UTF-8 -docencoding UTF-8 -charset UTF-8 -d api-manual -sourcepath src/main/java -subpackages timefit -windowtitle 'TimeFit API 0.4.0' -doctitle 'TimeFit Beta API 0.4.0' -notimestamp
    if ($LASTEXITCODE -ne 0) { throw 'Javadoc generation failed.' }
} finally { Pop-Location }
