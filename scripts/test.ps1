$ErrorActionPreference = 'Stop'
Push-Location (Split-Path $PSScriptRoot -Parent)
try {
    & ./scripts/build.ps1
    $sources = @(Get-ChildItem src/test/java -Filter *.java -Recurse | ForEach-Object { $_.FullName })
    & javac -encoding UTF-8 --release 17 -cp build/classes -d build/test-classes $sources
    if ($LASTEXITCODE -ne 0) { throw 'Test compilation failed.' }
    & java -ea -cp 'build/classes;build/test-classes' timefit.CoreTests
    if ($LASTEXITCODE -ne 0) { throw 'Tests failed.' }
} finally { Pop-Location }
