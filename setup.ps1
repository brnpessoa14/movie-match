param([string]$Python = 'python')
$ErrorActionPreference = 'Stop'
Push-Location $PSScriptRoot
try {
    if (-not (Test-Path '.venv\Scripts\python.exe')) {
        & $Python -m venv .venv
        if ($LASTEXITCODE -ne 0) { throw 'Não foi possível criar o ambiente Python 3.12+. Use -Python caminho\python.exe.' }
    }
    & '.\.venv\Scripts\python.exe' -m pip install -r backend\requirements.lock.txt
    if ($LASTEXITCODE -ne 0) { throw 'Falha ao instalar as dependências.' }
} finally { Pop-Location }
