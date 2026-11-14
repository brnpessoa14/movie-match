param([string]$ApiBaseUrl = 'http://10.0.2.2:8000/')
$ErrorActionPreference = 'Stop'
if (-not $env:JAVA_HOME) {
    $jbr = Get-ChildItem -LiteralPath 'C:\Program Files\Android' -Directory -ErrorAction SilentlyContinue |
        ForEach-Object { Join-Path $_.FullName 'jbr' } | Where-Object { Test-Path (Join-Path $_ 'bin\java.exe') } | Select-Object -First 1
    if (-not $jbr) { throw 'Configure JAVA_HOME com o JDK do Android Studio (17 ou posterior compatível com Gradle).' }
    $env:JAVA_HOME = $jbr
}
Push-Location $PSScriptRoot
try {
    & '.\gradlew.bat' assembleDebug "-PAPI_BASE_URL=$ApiBaseUrl"
    if ($LASTEXITCODE -ne 0) { throw 'A compilação falhou; consulte a saída do Gradle.' }
} finally { Pop-Location }
