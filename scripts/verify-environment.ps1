# AI-Ready Environment Verification Script
# Run: powershell -ExecutionPolicy Bypass -File .\verify-environment.ps1

Write-Host "==========================================" -ForegroundColor Green
Write-Host "AI-Ready Environment Verification" -ForegroundColor Green
Write-Host "==========================================" -ForegroundColor Green

$allPassed = $true

# 1. Check Java
Write-Host "`n[1/7] Checking Java 17..." -ForegroundColor Yellow
try {
    $javaOutput = java -version 2>&1 | Out-String
    if ($javaOutput -match "17") {
        Write-Host "   [OK] Java 17 installed" -ForegroundColor Green
    } else {
        Write-Host "   [FAIL] Java version incorrect, need Java 17" -ForegroundColor Red
        $allPassed = $false
    }
} catch {
    Write-Host "   [FAIL] Java not installed" -ForegroundColor Red
    $allPassed = $false
}

# 2. Check Maven
Write-Host "`n[2/7] Checking Maven..." -ForegroundColor Yellow
try {
    $mvnOutput = mvn -version 2>&1 | Out-String
    if ($mvnOutput -match "Apache Maven") {
        Write-Host "   [OK] Maven installed" -ForegroundColor Green
    } else {
        Write-Host "   [FAIL] Maven not installed" -ForegroundColor Red
        $allPassed = $false
    }
} catch {
    Write-Host "   [FAIL] Maven not installed" -ForegroundColor Red
    $allPassed = $false
}

# 3. Check Node.js
Write-Host "`n[3/7] Checking Node.js..." -ForegroundColor Yellow
try {
    $nodeVersion = node --version 2>&1
    if ($nodeVersion -match "v\d+") {
        Write-Host "   [OK] Node.js $nodeVersion installed" -ForegroundColor Green
    }
} catch {
    Write-Host "   [FAIL] Node.js not installed" -ForegroundColor Red
    $allPassed = $false
}

# 4. Check npm
Write-Host "`n[4/7] Checking npm..." -ForegroundColor Yellow
try {
    $npmVersion = npm --version 2>&1
    if ($npmVersion -match "\d+") {
        Write-Host "   [OK] npm $npmVersion installed" -ForegroundColor Green
    }
} catch {
    Write-Host "   [FAIL] npm not installed" -ForegroundColor Red
    $allPassed = $false
}

# 5. Check project structure
Write-Host "`n[5/7] Checking project structure..." -ForegroundColor Yellow
$projectRoot = "I:\AI-Ready"
$requiredDirs = @("core-base", "core-common", "smart-admin-web", "docker", "docs")
foreach ($dir in $requiredDirs) {
    $path = Join-Path $projectRoot $dir
    if (Test-Path $path) {
        Write-Host "   [OK] $dir directory exists" -ForegroundColor Green
    } else {
        Write-Host "   [WARN] $dir directory missing" -ForegroundColor Yellow
    }
}

# 6. Check pom.xml
Write-Host "`n[6/7] Checking pom.xml..." -ForegroundColor Yellow
$pomPath = Join-Path $projectRoot "pom.xml"
if (Test-Path $pomPath) {
    Write-Host "   [OK] pom.xml created" -ForegroundColor Green
} else {
    Write-Host "   [WARN] pom.xml missing" -ForegroundColor Yellow
}

# 7. Database service hint
Write-Host "`n[7/7] Database service check..." -ForegroundColor Yellow
Write-Host "   [INFO] PostgreSQL and Redis need to be started manually or via Docker" -ForegroundColor Yellow

# Summary
Write-Host "`n==========================================" -ForegroundColor Green
if ($allPassed) {
    Write-Host "[SUCCESS] Core environment verified!" -ForegroundColor Green
} else {
    Write-Host "[FAIL] Some core components missing" -ForegroundColor Red
}

Write-Host "`nNext steps:" -ForegroundColor Cyan
Write-Host "   1. Start PostgreSQL and Redis services" -ForegroundColor White
Write-Host "   2. cd I:\AI-Ready" -ForegroundColor White
Write-Host "   3. mvn clean install -DskipTests" -ForegroundColor White
Write-Host "==========================================" -ForegroundColor Green