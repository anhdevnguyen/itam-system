# hash-password.ps1 — Compile và chạy HashPassword.java
# Usage: .\hash-password.ps1

$secJar  = (Get-ChildItem "$env:USERPROFILE\.m2\repository\org\springframework\security\spring-security-crypto" -Filter "*.jar" -Recurse | Where-Object { $_.Name -notlike "*sources*" -and $_.Name -notlike "*javadoc*" } | Select-Object -First 1).FullName
$logJar  = (Get-ChildItem "$env:USERPROFILE\.m2\repository\commons-logging" -Filter "*.jar" -Recurse | Where-Object { $_.Name -notlike "*sources*" } | Select-Object -First 1).FullName
$outDir  = "$PSScriptRoot\target\hash-tool"
$srcFile = "$PSScriptRoot\HashPassword.java"

if (-not $secJar) { Write-Error "Khong tim thay spring-security-crypto jar. Chay 'mvn compile' truoc."; exit 1 }
if (-not $logJar) { Write-Error "Khong tim thay commons-logging jar. Chay 'mvn compile' truoc."; exit 1 }

New-Item -ItemType Directory -Force -Path $outDir | Out-Null

Write-Host "Compiling HashPassword.java..." -ForegroundColor Cyan
javac -cp "$secJar;$logJar" $srcFile -d $outDir
if ($LASTEXITCODE -ne 0) { Write-Error "Compile that bai."; exit 1 }

Write-Host "Running..." -ForegroundColor Cyan
Write-Host ""
java -cp "$outDir;$secJar;$logJar" HashPassword
