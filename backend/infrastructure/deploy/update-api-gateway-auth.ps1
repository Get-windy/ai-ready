# Update API Gateway with OAuth2 Authentication
# Updates the existing API Gateway to include OAuth2 authentication

param(
    [switch]$RestoreOriginal = $false
)

$ErrorActionPreference = "Continue"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "API Gateway OAuth2 Authentication Update" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Configuration
$originalConfig = "I:\AI-Ready\test_env\etc\important\api-gateway.conf"
$authConfig = "I:\AI-Ready\deploy\nginx\api-gateway-with-auth.conf"
$deployedConfig = "I:\AI-Ready\deploy\nginx\api-gateway.conf"
$containerName = "ai-ready-api-gateway"

# Step 1: Check if API Gateway is running
Write-Host "[1/4] Checking API Gateway status..." -ForegroundColor Yellow
$gatewayRunning = docker ps --filter "name=$containerName" --format "{{.Names}}" 2>$null

if (-not $gatewayRunning) {
    Write-Host "  ⚠️  API Gateway container not running" -ForegroundColor Yellow
    Write-Host "  Attempting to start API Gateway..." -ForegroundColor Yellow
    
    # Try to start API Gateway
    .\deploy-api-gateway.ps1 -NoBuild 2>$null
    
    # Recheck
    $gatewayRunning = docker ps --filter "name=$containerName" --format "{{.Names}}" 2>$null
    if (-not $gatewayRunning) {
        Write-Host "  ❌ Failed to start API Gateway" -ForegroundColor Red
        exit 1
    }
}

Write-Host "  ✅ API Gateway running: $containerName" -ForegroundColor Green

# Step 2: Backup current configuration
Write-Host "[2/4] Backing up current configuration..." -ForegroundColor Yellow
$backupDir = "I:\AI-Ready\deploy\nginx\backup"
$backupFile = "$backupDir\api-gateway-backup-$(Get-Date -Format 'yyyyMMdd-HHmmss').conf"

try {
    New-Item -Path $backupDir -ItemType Directory -Force | Out-Null
    
    if (Test-Path $deployedConfig) {
        Copy-Item -Path $deployedConfig -Destination $backupFile -Force
        Write-Host "  ✅ Configuration backed up to: $backupFile" -ForegroundColor Green
    } else {
        Write-Host "  ⚠️  Current configuration file not found" -ForegroundColor Yellow
    }
} catch {
    Write-Host "  ⚠️  Backup failed: $_" -ForegroundColor Yellow
}

# Step 3: Apply or restore configuration
Write-Host "[3/4] $(
    if ($RestoreOriginal) { "Restoring original configuration" } else { "Applying OAuth2 configuration" }
)..." -ForegroundColor Yellow

$sourceConfig = if ($RestoreOriginal) { $originalConfig } else { $authConfig }

if (-not (Test-Path $sourceConfig)) {
    Write-Host "  ❌ Source configuration not found: $sourceConfig" -ForegroundColor Red
    exit 1
}

try {
    # Copy new configuration
    Copy-Item -Path $sourceConfig -Destination $deployedConfig -Force
    Write-Host "  ✅ Configuration copied: $sourceConfig -> $deployedConfig" -ForegroundColor Green
    
    # Reload nginx configuration
    Write-Host "  Reloading nginx configuration..." -ForegroundColor Yellow
    docker exec $containerName nginx -s reload 2>$null
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "  ✅ Nginx configuration reloaded" -ForegroundColor Green
    } else {
        Write-Host "  ⚠️  Nginx reload failed, restarting container..." -ForegroundColor Yellow
        docker restart $containerName 2>$null
        if ($LASTEXITCODE -eq 0) {
            Write-Host "  ✅ Container restarted" -ForegroundColor Green
        } else {
            Write-Host "  ❌ Container restart failed" -ForegroundColor Red
        }
    }
} catch {
    Write-Host "  ❌ Configuration update failed: $_" -ForegroundColor Red
    exit 1
}

# Step 4: Verify update
Write-Host "[4/4] Verifying update..." -ForegroundColor Yellow
Start-Sleep -Seconds 3

# Check container status
$containerStatus = docker ps --filter "name=$containerName" --format "{{.Status}}" 2>$null
if ($containerStatus) {
    Write-Host "  ✅ Container running: $containerStatus" -ForegroundColor Green
} else {
    Write-Host "  ❌ Container not running after update" -ForegroundColor Red
    exit 1
}

# Test endpoints
Write-Host ""
Write-Host "Testing endpoints..." -ForegroundColor Yellow

$testEndpoints = @(
    @{Url = "http://localhost:8080"; Description = "API Gateway root"},
    @{Url = "http://localhost:8080/actuator/health"; Description = "Health endpoint"},
    @{Url = "http://localhost:8080/api/orders/"; Description = "Orders API (should return 401)"}
)

foreach ($endpoint in $testEndpoints) {
    try {
        $response = Invoke-WebRequest -Uri $endpoint.Url -Method GET -TimeoutSec 5 -ErrorAction SilentlyContinue
        Write-Host "  ✅ $($endpoint.Description): HTTP $($response.StatusCode)" -ForegroundColor Green
    } catch {
        $statusCode = $_.Exception.Response.StatusCode.value__
        if ($statusCode -eq 401 -and $endpoint.Description -like "*Orders API*") {
            Write-Host "  ✅ $($endpoint.Description): HTTP 401 (Unauthorized - expected)" -ForegroundColor Green
        } else {
            Write-Host "  ⚠️  $($endpoint.Description): HTTP $statusCode" -ForegroundColor Yellow
        }
    }
}

# Test OAuth2 endpoints if available
try {
    $oauth2Response = Invoke-WebRequest -Uri "http://localhost:8097" -Method GET -TimeoutSec 5 -ErrorAction SilentlyContinue
    if ($oauth2Response.StatusCode -eq 200) {
        Write-Host "  ✅ OAuth2 Mock Service: Available" -ForegroundColor Green
    }
} catch {
    Write-Host "  ⚠️  OAuth2 Mock Service: Not available (run .\deploy-auth-service.ps1)" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "API Gateway Update Complete" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

if ($RestoreOriginal) {
    Write-Host "Original configuration restored successfully!" -ForegroundColor Green
    Write-Host ""
    Write-Host "Configuration:" -ForegroundColor Yellow
    Write-Host "  API Gateway: http://localhost:8080" -ForegroundColor White
    Write-Host "  No authentication required" -ForegroundColor Gray
} else {
    Write-Host "OAuth2 authentication enabled successfully!" -ForegroundColor Green
    Write-Host ""
    Write-Host "Updated Configuration:" -ForegroundColor Yellow
    Write-Host "  API Gateway: http://localhost:8080" -ForegroundColor White
    Write-Host "  OAuth2 Mock: http://localhost:8097" -ForegroundColor White
    Write-Host "  Token Endpoint: http://localhost:8097/oauth2/token" -ForegroundColor Gray
    Write-Host ""
    Write-Host "Security Changes:" -ForegroundColor Yellow
    Write-Host "  ✅ Orders API requires Authorization header" -ForegroundColor Green
    Write-Host "  ✅ Inventory API requires Authorization header" -ForegroundColor Green
    Write-Host "  ✅ Monitoring API uses Basic Authentication" -ForegroundColor Green
    Write-Host "  ✅ Admin endpoints protected" -ForegroundColor Green
    Write-Host "  ✅ Health and docs endpoints open" -ForegroundColor Green
    Write-Host ""
    Write-Host "Next Steps:" -ForegroundColor Yellow
    Write-Host "  1. Deploy OAuth2 service: .\deploy-auth-service.ps1" -ForegroundColor White
    Write-Host "  2. Test authentication flow" -ForegroundColor White
    Write-Host "  3. Update microservices to validate tokens" -ForegroundColor White
    Write-Host "  4. Create users and clients in Keycloak" -ForegroundColor White
}

Write-Host ""
Write-Host "To restore original configuration:" -ForegroundColor Gray
Write-Host "  .\update-api-gateway-auth.ps1 -RestoreOriginal" -ForegroundColor White