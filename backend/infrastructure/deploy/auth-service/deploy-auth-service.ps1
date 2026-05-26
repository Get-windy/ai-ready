# Authentication Service Deployment Script
# Deploys OAuth2 authentication services for AI-Ready test environment

param(
    [switch]$Remove = $false,
    [switch]$MockOnly = $false,
    [switch]$KeycloakOnly = $false
)

$ErrorActionPreference = "Continue"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Authentication Service Deployment" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Configuration
$NetworkName = "ai-ready-test-network"

# Step 1: Check Docker network
Write-Host "[1/5] Checking Docker network..." -ForegroundColor Yellow
try {
    $network = docker network ls --filter "name=$NetworkName" --format "{{.Name}}" 2>$null
    if (-not $network) {
        Write-Host "  Creating $NetworkName network..." -ForegroundColor Yellow
        docker network create $NetworkName 2>$null | Out-Null
        if ($LASTEXITCODE -eq 0) {
            Write-Host "  ✅ Network created" -ForegroundColor Green
        } else {
            Write-Host "  ⚠️  Network already exists or creation failed" -ForegroundColor Yellow
        }
    } else {
        Write-Host "  ✅ Network exists: $network" -ForegroundColor Green
    }
} catch {
    Write-Host "  ⚠️  Error checking network: $_" -ForegroundColor Yellow
}

# Step 2: Create directories
Write-Host "[2/5] Creating directories..." -ForegroundColor Yellow
$directories = @(
    "I:\AI-Ready\deploy\auth-service\keycloak-data",
    "I:\AI-Ready\deploy\auth-service\postgres-data",
    "I:\AI-Ready\deploy\auth-service\oauth2-mock-data"
)

foreach ($dir in $directories) {
    try {
        New-Item -Path $dir -ItemType Directory -Force | Out-Null
        Write-Host "  ✅ Created: $dir" -ForegroundColor Green
    } catch {
        Write-Host "  ⚠️  Error creating $dir : $_" -ForegroundColor Yellow
    }
}

# Step 3: Create initialization SQL for Keycloak
Write-Host "[3/5] Creating Keycloak database initialization..." -ForegroundColor Yellow
$initSqlPath = "I:\AI-Ready\deploy\auth-service\init-keycloak-db.sql"
if (-not (Test-Path $initSqlPath)) {
    $initSql = @"
-- Keycloak database initialization
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";
CREATE EXTENSION IF NOT EXISTS "pg_stat_statements";

-- Create additional schemas if needed
CREATE SCHEMA IF NOT EXISTS keycloak_schema;
ALTER DATABASE keycloak SET search_path TO keycloak_schema, public;

-- Create additional users/roles for Keycloak
DO \$\$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'keycloak_app') THEN
        CREATE ROLE keycloak_app WITH LOGIN PASSWORD 'keycloak_app123';
    END IF;
    
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'keycloak_monitor') THEN
        CREATE ROLE keycloak_monitor WITH LOGIN PASSWORD 'keycloak_monitor123';
    END IF;
END
\$\$;

-- Grant permissions
GRANT CONNECT ON DATABASE keycloak TO keycloak_app, keycloak_monitor;
GRANT USAGE ON SCHEMA public TO keycloak_app, keycloak_monitor;
GRANT SELECT ON ALL TABLES IN SCHEMA public TO keycloak_monitor;
"@
    
    $initSql | Out-File -FilePath $initSqlPath -Encoding UTF8
    Write-Host "  ✅ Created initialization SQL" -ForegroundColor Green
} else {
    Write-Host "  ✅ Initialization SQL exists" -ForegroundColor Green
}

# Step 4: Deploy or remove services
Write-Host "[4/5] $(
    if ($Remove) { "Removing" } else { "Deploying" }
) authentication services..." -ForegroundColor Yellow

if ($Remove) {
    # Remove containers
    $containers = @("ai-ready-keycloak", "ai-ready-postgres-auth", "ai-ready-oauth2-mock")
    
    foreach ($container in $containers) {
        $existing = docker ps -a --filter "name=$container" --format "{{.Names}}" 2>$null
        if ($existing) {
            Write-Host "  Stopping and removing: $container" -ForegroundColor Yellow
            docker stop $container 2>$null | Out-Null
            docker rm $container 2>$null | Out-Null
            Write-Host "  ✅ Removed: $container" -ForegroundColor Green
        }
    }
    
    Write-Host ""
    Write-Host "All authentication services removed successfully!" -ForegroundColor Green
    exit 0
}

# Deploy services
$composeFile = "I:\AI-Ready\deploy\auth-service\docker-compose-auth.yml"

if ($MockOnly) {
    Write-Host "  Deploying OAuth2 Mock service only..." -ForegroundColor Yellow
    docker run -d `
        --name ai-ready-oauth2-mock `
        --network $NetworkName `
        -p "8097:80" `
        -v "I:\AI-Ready\deploy\auth-service\oauth2-mock.conf:/etc/nginx/conf.d/default.conf:ro" `
        --restart unless-stopped `
        nginx:alpine 2>$null
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "  ✅ OAuth2 Mock service deployed" -ForegroundColor Green
    } else {
        Write-Host "  ❌ Failed to deploy OAuth2 Mock service" -ForegroundColor Red
    }
} elseif ($KeycloakOnly) {
    Write-Host "  Deploying Keycloak with PostgreSQL..." -ForegroundColor Yellow
    docker-compose -f $composeFile up -d keycloak postgres-auth 2>$null
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "  ✅ Keycloak service deployed" -ForegroundColor Green
    } else {
        Write-Host "  ❌ Failed to deploy Keycloak service" -ForegroundColor Red
    }
} else {
    Write-Host "  Deploying all authentication services..." -ForegroundColor Yellow
    docker-compose -f $composeFile up -d 2>$null
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "  ✅ All authentication services deployed" -ForegroundColor Green
    } else {
        Write-Host "  ❌ Failed to deploy authentication services" -ForegroundColor Red
        Write-Host "  Trying to deploy OAuth2 Mock service as fallback..." -ForegroundColor Yellow
        
        # Try fallback
        docker run -d `
            --name ai-ready-oauth2-mock `
            --network $NetworkName `
            -p "8097:80" `
            -v "I:\AI-Ready\deploy\auth-service\oauth2-mock.conf:/etc/nginx/conf.d/default.conf:ro" `
            --restart unless-stopped `
            nginx:alpine 2>$null
        
        if ($LASTEXITCODE -eq 0) {
            Write-Host "  ✅ OAuth2 Mock service deployed as fallback" -ForegroundColor Green
        }
    }
}

# Step 5: Wait and verify
Write-Host "[5/5] Verifying services..." -ForegroundColor Yellow
Start-Sleep -Seconds 5

# Check OAuth2 Mock service
$mockRunning = docker ps --filter "name=ai-ready-oauth2-mock" --format "{{.Names}}" 2>$null
if ($mockRunning) {
    Write-Host "  ✅ OAuth2 Mock service running: http://localhost:8097" -ForegroundColor Green
    
    # Test endpoint
    try {
        $response = Invoke-WebRequest -Uri "http://localhost:8097/health" -Method GET -TimeoutSec 5 -ErrorAction SilentlyContinue
        if ($response.StatusCode -eq 200) {
            Write-Host "  ✅ OAuth2 Mock health check passed" -ForegroundColor Green
        }
    } catch {
        Write-Host "  ⚠️  OAuth2 Mock health check failed: $_" -ForegroundColor Yellow
    }
}

# Check Keycloak service
$keycloakRunning = docker ps --filter "name=ai-ready-keycloak" --format "{{.Names}}" 2>$null
if ($keycloakRunning) {
    Write-Host "  ✅ Keycloak service running: http://localhost:8087" -ForegroundColor Green
    
    # Wait a bit longer for Keycloak to initialize
    Write-Host "  Waiting for Keycloak to initialize (this may take a minute)..." -ForegroundColor Yellow
    Start-Sleep -Seconds 30
    
    try {
        $response = Invoke-WebRequest -Uri "http://localhost:8087/health/ready" -Method GET -TimeoutSec 10 -ErrorAction SilentlyContinue
        if ($response.StatusCode -eq 200) {
            Write-Host "  ✅ Keycloak health check passed" -ForegroundColor Green
        }
    } catch {
        Write-Host "  ⚠️  Keycloak may still be initializing: $_" -ForegroundColor Yellow
    }
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Authentication Service Deployment Complete" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Deployed Services:" -ForegroundColor Yellow
if ($mockRunning) {
    Write-Host "  ✅ OAuth2 Mock Service" -ForegroundColor Green
    Write-Host "     URL: http://localhost:8097" -ForegroundColor White
    Write-Host "     Token Endpoint: http://localhost:8097/oauth2/token" -ForegroundColor Gray
    Write-Host "     Userinfo Endpoint: http://localhost:8097/oauth2/userinfo" -ForegroundColor Gray
}
if ($keycloakRunning) {
    Write-Host "  ✅ Keycloak OAuth2 Server" -ForegroundColor Green
    Write-Host "     URL: http://localhost:8087" -ForegroundColor White
    Write-Host "     Admin Console: http://localhost:8087/admin" -ForegroundColor Gray
    Write-Host "     Admin User: admin" -ForegroundColor Gray
    Write-Host "     Admin Password: admin123" -ForegroundColor Gray
}
Write-Host ""
Write-Host "Configuration Notes:" -ForegroundColor Yellow
Write-Host "  1. Update API Gateway to use OAuth2 endpoints" -ForegroundColor White
Write-Host "  2. Configure microservices to validate JWT tokens" -ForegroundColor White
Write-Host "  3. Create Keycloak realms, clients, and users" -ForegroundColor White
Write-Host ""
Write-Host "Next Steps:" -ForegroundColor Yellow
Write-Host "  1. Run: .\update-api-gateway-auth.ps1" -ForegroundColor White
Write-Host "  2. Configure services to use OAuth2" -ForegroundColor White
Write-Host "  3. Test authentication flow" -ForegroundColor White