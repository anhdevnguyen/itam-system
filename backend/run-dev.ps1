# run-dev.ps1 — Chạy Spring Boot local với biến từ .env
# Usage: .\run-dev.ps1

$envFile = "$PSScriptRoot\..\\.env"

if (-not (Test-Path $envFile)) {
    Write-Error ".env not found at $envFile. Copy .env.example to .env first."
    exit 1
}

Get-Content $envFile | Where-Object { $_ -notmatch '^\s*#' -and $_ -match '=' } | ForEach-Object {
    $parts = $_ -split '=', 2
    [System.Environment]::SetEnvironmentVariable($parts[0].Trim(), $parts[1].Trim(), 'Process')
}

Write-Host "✓ Environment loaded from .env" -ForegroundColor Green
Write-Host "✓ Starting Spring Boot..." -ForegroundColor Green

mvn spring-boot:run
