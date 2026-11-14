$ErrorActionPreference = 'Stop'
$python = Join-Path $PSScriptRoot '.venv\Scripts\python.exe'
if (-not (Test-Path -LiteralPath $python)) { throw 'Execute .\setup.ps1 primeiro (Python 3.12+).' }
Push-Location (Join-Path $PSScriptRoot 'backend')
try { & $python -m uvicorn app.main:app --host 0.0.0.0 --port 8000 }
finally { Pop-Location }
